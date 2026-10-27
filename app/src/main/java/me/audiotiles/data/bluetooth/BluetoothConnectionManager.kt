package me.audiotiles.data.bluetooth

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import me.audiotiles.data.protocol.MdrCommandBuilder
import me.audiotiles.data.protocol.MdrConstants
import me.audiotiles.data.protocol.MdrFrameDecoder
import me.audiotiles.data.protocol.MdrFrameEncoder
import me.audiotiles.data.protocol.MdrPacket
import me.audiotiles.data.protocol.MdrResponseParser
import me.audiotiles.domain.BatteryState
import me.audiotiles.domain.DeviceCapabilities
import me.audiotiles.domain.EqPreset
import me.audiotiles.domain.EqualizerState
import me.audiotiles.domain.NoiseControlMode
import me.audiotiles.domain.NoiseControlState
import me.audiotiles.domain.PeqProfile
import me.audiotiles.domain.SoundFeaturesState
import me.audiotiles.domain.SpeakToChatState
import me.audiotiles.domain.TenBandEqState
import me.audiotiles.domain.TileCycleScheme
import me.audiotiles.domain.UserEqPreset
import me.audiotiles.service.AudioForegroundService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.audiotiles.BuildConfig
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

enum class LogDirection {
    TX, RX, ACK_TX, ACK_RX, INFO, WARN, ERROR
}

data class MdrDiagnosticLogEntry(
    val id: Long = System.nanoTime(),
    val timestampMs: Long = System.currentTimeMillis(),
    val timeFormatted: String,
    val direction: LogDirection,
    val opcode: Int? = null,
    val opcodeName: String = "",
    val rawHex: String = "",
    val payloadHex: String = "",
    val note: String = ""
)

/**
 * Singleton Bluetooth Connection & Session Manager for Wireless Headphones and Earbuds.
 *
 * Coordinates:
 * - Thread-safe RFCOMM socket lifecycle with multi-tier fallback
 * - Alternating-bit Stop-and-Wait ARQ sequencing
 * - Automatic host ACK replies to device frames
 * - Headphone battery telemetry queries and live updates
 * - Ambient sound level (1..20), Voice Focus, and Wind Noise Reduction controls
 * - Idle power-saving auto-disconnect (conserves battery when foreground service is off)
 * - Hardware disconnect receiver (cleans up immediately when headphones power down)
 */
class BluetoothConnectionManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deviceScanner = DeviceScanner(context)
    private val frameDecoder = MdrFrameDecoder()
    private val commandMutex = Mutex()

    private var socketWrapper: BluetoothSocketWrapper? = null
    private var readJob: Job? = null
    private var idleDisconnectJob: Job? = null

    // Alternating transmit sequence number (0 or 1)
    private var txSequence = 0

    // Command response synchronization primitives
    private var pendingAckWaiter: CompletableDeferred<Int>? = null
    private var pendingHandshakeWaiter: CompletableDeferred<ByteArray>? = null
    private var pendingAscWaiter: CompletableDeferred<NoiseControlState>? = null

    // Reactive State Streams
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _noiseControlState = MutableStateFlow(NoiseControlState())
    val noiseControlState: StateFlow<NoiseControlState> = _noiseControlState.asStateFlow()

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _tenBandEqState = MutableStateFlow(TenBandEqState())
    val tenBandEqState: StateFlow<TenBandEqState> = _tenBandEqState.asStateFlow()

    private val _soundFeaturesState = MutableStateFlow(SoundFeaturesState())
    val soundFeaturesState: StateFlow<SoundFeaturesState> = _soundFeaturesState.asStateFlow()

    private val _tileCycleScheme = MutableStateFlow(TileCycleScheme.ALL_THREE)
    val tileCycleScheme: StateFlow<TileCycleScheme> = _tileCycleScheme.asStateFlow()

    private val _targetDevice = MutableStateFlow<BluetoothDevice?>(null)
    val targetDevice: StateFlow<BluetoothDevice?> = _targetDevice.asStateFlow()

    private val _deviceCapabilities = MutableStateFlow(DeviceCapabilities())
    val deviceCapabilities: StateFlow<DeviceCapabilities> = _deviceCapabilities.asStateFlow()

    private val _isEqBypassed = MutableStateFlow(false)
    val isEqBypassed: StateFlow<Boolean> = _isEqBypassed.asStateFlow()

    private val _userPresets = MutableStateFlow<List<UserEqPreset>>(UserEqPreset.DEFAULT_PRESETS)
    val userPresets: StateFlow<List<UserEqPreset>> = _userPresets.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<String>>(emptyList())
    val diagnosticLogs: StateFlow<List<String>> = _diagnosticLogs.asStateFlow()

    private val _diagnosticEntries = MutableStateFlow<List<MdrDiagnosticLogEntry>>(emptyList())
    val diagnosticEntries: StateFlow<List<MdrDiagnosticLogEntry>> = _diagnosticEntries.asStateFlow()

    init {
        // Load persisted settings
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        val savedSchemeName = prefs.getString(KEY_TILE_CYCLE_SCHEME, null)
        if (savedSchemeName != null) {
            try {
                _tileCycleScheme.value = TileCycleScheme.valueOf(savedSchemeName)
            } catch (_: Exception) {}
        }
        val savedDeviceAddr = prefs.getString(KEY_PREFERRED_DEVICE_ADDR, null)

        // Initial target scan
        refreshTargetDevice(savedDeviceAddr)
        loadUserPresets()
        registerBluetoothAclReceiver()
    }

    fun setTileCycleScheme(scheme: TileCycleScheme) {
        _tileCycleScheme.value = scheme
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TILE_CYCLE_SCHEME, scheme.name).apply()
        addLog("Tile cycle scheme changed to: ${scheme.label}")
    }

    @SuppressLint("MissingPermission")
    fun refreshTargetDevice(preferredAddress: String? = null) {
        if (preferredAddress != null) {
            val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PREFERRED_DEVICE_ADDR, preferredAddress).apply()
        }
        val dev = deviceScanner.findTargetDevice(preferredAddress)
        val currentConnectedAddr = (_connectionState.value as? ConnectionState.Connected)?.address
        if (currentConnectedAddr != null && dev != null && !currentConnectedAddr.equals(dev.address, ignoreCase = true)) {
            addLog("Target device changed from $currentConnectedAddr to ${dev.address}. Reconnecting...")
            scope.launch {
                disconnect()
            }
        }
        _targetDevice.value = dev
        _deviceCapabilities.value = DeviceCapabilities.resolve(dev?.name)
    }

    private fun addLog(message: String) {
        Log.d(TAG, message)
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        _diagnosticLogs.update { current ->
            val list = current.toMutableList()
            list.add("[$timestamp] $message")
            if (list.size > 200) list.removeAt(0)
            list
        }
    }

    fun logDiagnostic(
        direction: LogDirection,
        opcode: Int? = null,
        opcodeName: String = "",
        rawHex: String = "",
        payloadHex: String = "",
        note: String = ""
    ) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        val entry = MdrDiagnosticLogEntry(
            timeFormatted = timestamp,
            direction = direction,
            opcode = opcode,
            opcodeName = opcodeName,
            rawHex = rawHex,
            payloadHex = payloadHex,
            note = note
        )
        _diagnosticEntries.update { current ->
            val list = current.toMutableList()
            list.add(entry)
            if (list.size > 200) list.removeAt(0)
            list
        }

        val summary = buildString {
            append("[${direction.name}] ")
            if (opcodeName.isNotEmpty()) append("$opcodeName ")
            if (payloadHex.isNotEmpty()) append("[$payloadHex] ")
            if (note.isNotEmpty()) append(note)
        }
        addLog(summary)
    }

    fun clearDiagnosticLogs() {
        _diagnosticLogs.value = emptyList()
        _diagnosticEntries.value = emptyList()
        logDiagnostic(LogDirection.INFO, note = "Diagnostic logs cleared by user")
    }

    private fun maskMac(address: String): String {
        val parts = address.split(":")
        if (parts.size == 6) {
            return "XX:XX:XX:XX:${parts[4]}:${parts[5]}"
        }
        return "XX:XX:XX:XX"
    }

    @SuppressLint("MissingPermission")
    fun exportDiagnosticReport(): String {
        val dev = _targetDevice.value
        val caps = _deviceCapabilities.value
        val conn = _connectionState.value
        val nc = _noiseControlState.value
        val bat = _batteryState.value
        val eq = _tenBandEqState.value
        val bypass = _isEqBypassed.value
        val logs = _diagnosticEntries.value

        return buildString {
            appendLine("==================================================")
            appendLine("       SONY AUDIO TILES DIAGNOSTIC DUMP REPORT     ")
            appendLine("==================================================")
            appendLine("Generated At: ${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US).format(java.util.Date())}")
            appendLine("App Version: ${me.audiotiles.BuildConfig.VERSION_NAME} (Code ${me.audiotiles.BuildConfig.VERSION_CODE})")
            appendLine("Android OS: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Device Hardware: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (${android.os.Build.DEVICE})")
            appendLine()
            appendLine("--- TARGET HEADSET & HARDWARE CAPABILITIES ---")
            appendLine("Device Name: ${dev?.name ?: "None"}")
            appendLine("Device Address: ${dev?.address?.let { maskMac(it) } ?: "N/A"}")
            appendLine("Resolved Model: ${caps.modelName} (${caps.family.displayName})")
            appendLine("Capabilities:")
            appendLine("  - Speak-to-Chat Hardware: ${caps.hasSpeakToChat}")
            appendLine("  - DSEE AI Audio Upscaling: ${caps.hasDsee}")
            appendLine("  - Wind Buffeting Suppression: ${caps.hasWindNoiseReduction}")
            appendLine("  - Clear Bass Subwoofer: ${caps.hasClearBass}")
            appendLine("  - Multi-Cell Battery (TWS L/R/Case): ${caps.hasMultiBattery}")
            appendLine("  - Optical Proximity / Wearing Sensor: ${caps.hasWearingDetection}")
            appendLine("  - Capacitive Touch Gesture Controls: ${caps.hasTouchSensorControl}")
            appendLine()
            appendLine("--- ACTIVE TELEMETRY & SOUND PROFILE ---")
            appendLine("Connection Status: ${conn::class.simpleName}")
            appendLine("Noise Control Mode: ${nc.mode.label} (Level ${nc.ambientLevel}, Voice Focus: ${nc.focusOnVoice})")
            appendLine("Battery Status: ${bat.displayString}")
            appendLine("EQ Bypass Active (A/B Test): $bypass")
            val hwEq = eq.toHardwareDspEq()
            appendLine("Clear Bass Level: ${hwEq.clearBass}")
            appendLine("10-Band EQ Frequencies & Gains:")
            me.audiotiles.domain.TEN_BAND_FREQUENCIES.forEachIndexed { i, freq ->
                val gain = eq.bands.getOrElse(i) { 0f }
                appendLine("    ${freq.toString().padEnd(6)} Hz : ${if (gain >= 0) "+" else ""}${gain} dB")
            }
            appendLine()
            appendLine("--- RECENT MDR PACKET STREAM (Last ${logs.size} Events) ---")
            for (log in logs) {
                appendLine("[${log.timeFormatted}] [${log.direction.name.padEnd(6)}] " +
                    (if (log.opcodeName.isNotEmpty()) "${log.opcodeName.padEnd(16)} | " else "") +
                    (if (log.payloadHex.isNotEmpty()) "Payload: ${log.payloadHex} | " else "") +
                    log.note)
                if (log.rawHex.isNotEmpty() && log.rawHex != log.payloadHex) {
                    appendLine("         Raw Wire: ${log.rawHex}")
                }
            }
            appendLine("==================================================")
            appendLine("             END OF DIAGNOSTIC REPORT             ")
            appendLine("==================================================")
        }
    }

    /**
     * Toggles A/B Audition Bypass: sends Flat 0dB to headset DSP momentarily
     * or restores the active 10-band profile.
     */
    fun toggleEqBypass() {
        val newBypass = !_isEqBypassed.value
        scope.launch {
            commandMutex.withLock {
                if (socketWrapper?.isConnected != true) {
                    val conn = connectInternal()
                    if (conn.isFailure) {
                        addLog("Cannot toggle EQ bypass: device not connected")
                        return@withLock
                    }
                }
                _isEqBypassed.value = newBypass
                if (newBypass) {
                    logDiagnostic(LogDirection.INFO, opcodeName = "A/B_BYPASS", note = "A/B Audition: EQ Bypassed (Flat 0dB sent to hardware DSP)")
                    val frame = MdrCommandBuilder.buildSetEqualizerBandsFrame(
                        seq = txSequence,
                        clearBass = 0,
                        bands = listOf(0, 0, 0, 0, 0)
                    )
                    val res = sendFrameWithAck(frame)
                    if (res.isFailure) {
                        _isEqBypassed.value = !newBypass
                        addLog("Warning: EQ bypass command not acknowledged")
                    }
                } else {
                    logDiagnostic(LogDirection.INFO, opcodeName = "A/B_RESTORE", note = "A/B Audition: EQ Restored to active 10-band profile")
                    val res = applyTenBandEqInternal(_tenBandEqState.value)
                    if (res.isFailure) {
                        _isEqBypassed.value = !newBypass
                        addLog("Warning: EQ restore command not acknowledged")
                    }
                }
            }
        }
    }

    // --- User Preset Persistence ---
    private val PREFS_PRESETS = "sony_audio_tiles_presets"
    private val KEY_SAVED_PRESETS = "user_presets_json"

    fun loadUserPresets(): List<UserEqPreset> {
        val prefs = context.getSharedPreferences(PREFS_PRESETS, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_SAVED_PRESETS, null)
        if (rawJson.isNullOrEmpty()) {
            _userPresets.value = UserEqPreset.DEFAULT_PRESETS
            return UserEqPreset.DEFAULT_PRESETS
        }
        return try {
            val arr = org.json.JSONArray(rawJson)
            val list = mutableListOf<UserEqPreset>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id", java.util.UUID.randomUUID().toString())
                val name = o.optString("name", "Preset $i")
                val cb = o.optInt("clearBass", 0)
                val gainsArr = o.optJSONArray("gains")
                val gains = mutableListOf<Float>()
                if (gainsArr != null) {
                    for (j in 0 until gainsArr.length()) {
                        gains.add(gainsArr.optDouble(j, 0.0).toFloat())
                    }
                } else {
                    gains.addAll(List(10) { 0.0f })
                }
                list.add(UserEqPreset(id, name, gains, cb))
            }
            if (list.isEmpty()) UserEqPreset.DEFAULT_PRESETS else list
        } catch (_: Throwable) {
            UserEqPreset.DEFAULT_PRESETS
        }.also {
            _userPresets.value = it
        }
    }

    fun saveUserPreset(name: String, tenBandState: TenBandEqState) {
        val current = _userPresets.value.toMutableList()
        val newPreset = UserEqPreset(
            name = name.trim(),
            tenBandGains = tenBandState.bands,
            clearBass = tenBandState.clearBass
        )
        current.add(0, newPreset)
        _userPresets.value = current
        persistUserPresets(current)
        logDiagnostic(LogDirection.INFO, note = "Saved custom user preset: \"${name.trim()}\"")
    }

    fun deleteUserPreset(presetId: String) {
        val current = _userPresets.value.toMutableList()
        current.removeAll { it.id == presetId }
        _userPresets.value = current
        persistUserPresets(current)
        logDiagnostic(LogDirection.INFO, note = "Deleted custom preset ID: $presetId")
    }

    private fun persistUserPresets(presets: List<UserEqPreset>) {
        scope.launch(Dispatchers.IO) {
            try {
                val arr = org.json.JSONArray()
                for (p in presets) {
                    val o = org.json.JSONObject()
                    o.put("id", p.id)
                    o.put("name", p.name)
                    o.put("clearBass", p.clearBass)
                    val gArr = org.json.JSONArray()
                    p.tenBandGains.forEach { gArr.put(it.toDouble()) }
                    o.put("gains", gArr)
                    arr.put(o)
                }
                context.getSharedPreferences(PREFS_PRESETS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SAVED_PRESETS, arr.toString())
                    .apply()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Resets the idle disconnect timer to conserve phone and headphone battery.
     * When foreground service is not running, drops the socket after 2 minutes of inactivity.
     */
    private fun touchActivity() {
        idleDisconnectJob?.cancel()
        if (!AudioForegroundService.isServiceRunning) {
            idleDisconnectJob = scope.launch {
                delay(120_000L) // 2 minutes idle timeout
                if (socketWrapper?.isConnected == true && !AudioForegroundService.isServiceRunning) {
                    addLog("Power Saver: Auto-disconnecting idle RFCOMM socket (120s timeout)")
                    disconnectInternal()
                }
            }
        }
    }

    /**
     * Registers a broadcast receiver to instantly release socket resources
     * when the headphone is turned off or leaves range.
     */
    private fun registerBluetoothAclReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    @Suppress("DEPRECATION")
                    val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val currentAddr = _targetDevice.value?.address
                    if (dev != null && dev.address.equals(currentAddr, ignoreCase = true)) {
                        addLog("Bluetooth ACL disconnected for ${dev.name ?: dev.address}. Cleaning up socket.")
                        scope.launch { disconnect() }
                    }
                }
            }
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register ACL receiver: ${e.message}")
        }
    }

    /**
     * Sends framed packet and waits for device ACK, preventing sequence desync.
     */
    private suspend fun sendFrameWithAck(frame: ByteArray, timeoutMs: Long = 2000L): Result<Unit> {
        val waiter = CompletableDeferred<Int>()
        pendingAckWaiter = waiter

        if (!sendRawBytes(frame)) {
            pendingAckWaiter = null
            return Result.failure(IOException("Failed to write frame to Bluetooth socket"))
        }

        val ackReceived = withTimeoutOrNull(timeoutMs) {
            waiter.await()
        }
        pendingAckWaiter = null

        if (ackReceived == null) {
            addLog("Warning: Frame timed out waiting for device ACK (seq=$txSequence)")
            return Result.failure(IOException("Timeout waiting for headset ACK"))
        }
        return Result.success(Unit)
    }

    /**
     * Connects to the Sony headphone, performs the MDR handshake, and queries initial ANC mode.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(target: BluetoothDevice? = null): Result<Unit> = commandMutex.withLock {
        connectInternal(target)
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectInternal(target: BluetoothDevice? = null): Result<Unit> {
        val device = target ?: _targetDevice.value ?: deviceScanner.findTargetDevice()
        if (device == null) {
            val err = "Headphone not found in paired devices."
            _connectionState.value = ConnectionState.Error(err)
            addLog("Connect error: $err")
            return Result.failure(IllegalStateException(err))
        }

        _targetDevice.value = device
        val deviceName = device.name ?: "Wireless Headphone"
        val address = device.address

        val currentConnectedAddr = (_connectionState.value as? ConnectionState.Connected)?.address
        if (socketWrapper?.isConnected == true) {
            if (currentConnectedAddr != null && !currentConnectedAddr.equals(address, ignoreCase = true)) {
                addLog("Switching connection from $currentConnectedAddr to $deviceName ($address)")
                disconnectInternal()
            } else {
                addLog("Already connected to $deviceName ($address)")
                touchActivity()
                return Result.success(Unit)
            }
        } else {
            disconnectInternal()
        }

        _connectionState.value = ConnectionState.Connecting(deviceName, address)
        addLog("Connecting RFCOMM socket to $deviceName ($address)...")

        val wrapper = BluetoothSocketWrapper(device, ::addLog)
        val connectResult = wrapper.connect(timeoutMs = 14000L)
        if (connectResult.isFailure) {
            val ex = connectResult.exceptionOrNull() ?: IOException("Connection failed")
            val msg = if (ex.message?.contains("read ret: -1") == true || ex.message?.contains("closed") == true) {
                "Failed to connect: Headset closed connection (read ret: -1). If OEM Sound Connect is open, force stop it."
            } else {
                "Failed to connect: ${ex.message}"
            }
            _connectionState.value = ConnectionState.Error(msg)
            addLog("Socket connect failed: ${ex.message}")
            return Result.failure(ex)
        }

        socketWrapper = wrapper
        frameDecoder.reset()
        txSequence = 0

        // Start non-blocking socket receiver
        startReaderLoop(wrapper)

        // Perform Sony MDR Handshake (0x00 0x00)
        addLog("Sending MDR Handshake...")
        val handshakeOk = performHandshake()
        if (!handshakeOk) {
            addLog("Handshake did not receive response, probing state directly...")
        } else {
            addLog("Handshake acknowledged by headset.")
        }

        // Query initial Noise Control State (0x66 0x17)
        val initialAsc = queryNoiseControlInternal()
        if (initialAsc != null) {
            _noiseControlState.value = initialAsc
            addLog("Headphone state: ${initialAsc.mode.label} (Level ${initialAsc.ambientLevel})")
        } else {
            addLog("ASC query not answered yet; ready for mode commands.")
        }

        // Query initial Battery State on confirmed V2 devices (Opcode 0x22 powers off legacy V1 devices!)
        val isV1 = (_noiseControlState.value.subtype == MdrConstants.SUBTYPE_NC_ASM_V1 || _noiseControlState.value.subtype == 0x01.toByte())
        if (!isV1) {
            queryBatteryInternal()
            delay(120L)
            queryEqualizerInternal()
        } else {
            addLog("Legacy MDR V1 detected: Battery opcode 0x22 omitted for device safety.")
        }

        _connectionState.value = ConnectionState.Connected(deviceName, address)
        touchActivity()
        return Result.success(Unit)
    }

    private suspend fun queryEqualizerInternal() {
        val frame = MdrCommandBuilder.buildGetEqualizerFrame(txSequence)
        sendFrameWithAck(frame, timeoutMs = 1500L)
    }

    /**
     * Executes MDR Handshake sequence:
     * TX: 0x0C [0x00, 0x00]
     * Headset responds with ACK, then 0x0C [0x01 ...]
     */
    private suspend fun performHandshake(): Boolean {
        val waiter = CompletableDeferred<ByteArray>()
        pendingHandshakeWaiter = waiter

        val frame = MdrCommandBuilder.buildHandshakeFrame(txSequence)
        val sendResult = sendRawBytes(frame)
        if (!sendResult) {
            pendingHandshakeWaiter = null
            return false
        }

        val result = withTimeoutOrNull(2500L) {
            waiter.await()
        }
        pendingHandshakeWaiter = null
        return result != null
    }

    /**
     * Queries headset for current ANC / Ambient mode.
     */
    private suspend fun queryNoiseControlInternal(): NoiseControlState? {
        val waiter = CompletableDeferred<NoiseControlState>()
        pendingAscWaiter = waiter

        // First attempt standard 0x17 subtype
        val frame = MdrCommandBuilder.buildGetNoiseControlFrame(txSequence, MdrConstants.SUBTYPE_NC_ASM_V2)
        if (!sendRawBytes(frame)) {
            pendingAscWaiter = null
            return null
        }

        var state = withTimeoutOrNull(2000L) { waiter.await() }

        // Fallback to alternate 0x15 subtype if 0x17 was not answered
        if (state == null) {
            val fallbackWaiter = CompletableDeferred<NoiseControlState>()
            pendingAscWaiter = fallbackWaiter
            val fallbackFrame = MdrCommandBuilder.buildGetNoiseControlFrame(txSequence, MdrConstants.SUBTYPE_NC_ASM_ALT)
            if (sendRawBytes(fallbackFrame)) {
                state = withTimeoutOrNull(1500L) { fallbackWaiter.await() }
            }
        }

        pendingAscWaiter = null
        return state
    }

    /**
     * Queries headset for battery level.
     */
    private suspend fun queryBatteryInternal() {
        val frame = MdrCommandBuilder.buildGetBatteryFrame(txSequence)
        sendFrameWithAck(frame, timeoutMs = 1500L)
    }

    /**
     * Public battery refresh method.
     */
    suspend fun queryBattery(): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        queryBatteryInternal()
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Sets noise control mode on the headphone.
     */
    suspend fun setMode(targetMode: NoiseControlMode): Result<NoiseControlMode> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) {
                return Result.failure(conn.exceptionOrNull() ?: IOException("Not connected"))
            }
        }

        addLog("Switching mode to: ${targetMode.label}...")
        val currentState = _noiseControlState.value

        val frame = MdrCommandBuilder.buildSetNoiseControlFrame(
            seq = txSequence,
            targetMode = targetMode,
            currentState = currentState,
            ambientLevel = currentState.ambientLevel,
            focusOnVoice = currentState.focusOnVoice
        )

        val sendRes = sendFrameWithAck(frame, timeoutMs = 2500L)
        if (sendRes.isFailure) {
            addLog("Warning: Mode switch to ${targetMode.label} failed or not acknowledged")
            return Result.failure(sendRes.exceptionOrNull() ?: IOException("Mode switch not acknowledged"))
        }

        val updatedState = currentState.copy(mode = targetMode)
        _noiseControlState.value = updatedState
        touchActivity()

        return Result.success(targetMode)
    }

    /**
     * Sets the Ambient Sound level (1 to 20).
     */
    suspend fun setAmbientLevel(level: Int): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }

        val clamped = level.coerceIn(MdrConstants.AMBIENT_LEVEL_MIN, MdrConstants.AMBIENT_LEVEL_MAX)
        val current = _noiseControlState.value

        addLog("Setting ambient sound level to $clamped...")
        val frame = MdrCommandBuilder.buildSetNoiseControlFrame(
            seq = txSequence,
            targetMode = NoiseControlMode.AMBIENT_SOUND,
            currentState = current,
            ambientLevel = clamped,
            focusOnVoice = current.focusOnVoice
        )

        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _noiseControlState.value = current.copy(
            mode = NoiseControlMode.AMBIENT_SOUND,
            ambientLevel = clamped
        )
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Toggles Voice Focus in Ambient Sound mode.
     */
    suspend fun setFocusOnVoice(enabled: Boolean): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }

        val current = _noiseControlState.value
        addLog("Setting Focus on Voice: $enabled...")
        val frame = MdrCommandBuilder.buildSetNoiseControlFrame(
            seq = txSequence,
            targetMode = NoiseControlMode.AMBIENT_SOUND,
            currentState = current,
            ambientLevel = current.ambientLevel,
            focusOnVoice = enabled
        )

        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _noiseControlState.value = current.copy(
            mode = NoiseControlMode.AMBIENT_SOUND,
            focusOnVoice = enabled
        )
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Toggles Wind Noise Reduction in Noise Cancelling mode.
     */
    suspend fun setWindNoiseReduction(enabled: Boolean): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }

        val current = _noiseControlState.value
        addLog("Setting Wind Noise Reduction: $enabled...")
        val frame = MdrCommandBuilder.buildSetNoiseControlFrame(
            seq = txSequence,
            targetMode = NoiseControlMode.NOISE_CANCELLING,
            currentState = current.copy(windNoiseReduction = enabled),
            ambientLevel = current.ambientLevel,
            focusOnVoice = current.focusOnVoice
        )

        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _noiseControlState.value = current.copy(
            mode = NoiseControlMode.NOISE_CANCELLING,
            windNoiseReduction = enabled
        )
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Cycles through noise control modes using the active cycle scheme.
     */
    suspend fun cycleMode(): Result<NoiseControlMode> {
        val current = _noiseControlState.value.mode
        val next = current.next(_tileCycleScheme.value)
        return setMode(next)
    }

    /**
     * Sets Sony Equalizer preset (Off, Bright, Excited, Bass Boost, Custom, etc.)
     */
    suspend fun setEqualizerPreset(preset: EqPreset): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        addLog("Setting EQ Preset: ${preset.displayName}...")
        val frame = MdrCommandBuilder.buildSetEqualizerPresetFrame(txSequence, preset)
        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _equalizerState.value = _equalizerState.value.copy(preset = preset)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Sets 5-Band Equalizer values and Clear Bass.
     */
    suspend fun setEqualizerBands(
        preset: EqPreset = EqPreset.CUSTOM_1,
        clearBass: Int,
        bands: List<Int>
    ): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        addLog("Applying 5-band Equalizer (Clear Bass: $clearBass)...")
        val frame = MdrCommandBuilder.buildSetEqualizerBandsFrame(
            seq = txSequence,
            preset = preset,
            clearBass = clearBass,
            bands = bands
        )
        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _equalizerState.value = EqualizerState(
            preset = preset,
            clearBass = clearBass,
            band400Hz = bands.getOrElse(0) { 0 },
            band1kHz = bands.getOrElse(1) { 0 },
            band2_5kHz = bands.getOrElse(2) { 0 },
            band6_3kHz = bands.getOrElse(3) { 0 },
            band16kHz = bands.getOrElse(4) { 0 }
        )
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Applies a 10-band Equalizer curve by downsampling & translating to Sony hardware DSP.
     */
    suspend fun applyTenBandEq(state: TenBandEqState): Result<Unit> = commandMutex.withLock {
        applyTenBandEqInternal(state)
    }

    private suspend fun applyTenBandEqInternal(state: TenBandEqState): Result<Unit> {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }

        val hw = state.toHardwareDspEq()
        addLog("Applying 10-Band EQ '${state.profileName}' -> Hardware DSP (CB=${hw.clearBass}, 400=${hw.band400Hz}, 1k=${hw.band1kHz}, 2.5k=${hw.band2_5kHz}, 6.3k=${hw.band6_3kHz}, 16k=${hw.band16kHz})")
        val frame = MdrCommandBuilder.buildSetEqualizerBandsFrame(
            seq = txSequence,
            preset = EqPreset.CUSTOM_1,
            clearBass = hw.clearBass,
            bands = hw.bands
        )
        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _tenBandEqState.value = state
        _equalizerState.value = EqualizerState(
            preset = EqPreset.CUSTOM_1,
            clearBass = hw.clearBass,
            band400Hz = hw.band400Hz,
            band1kHz = hw.band1kHz,
            band2_5kHz = hw.band2_5kHz,
            band6_3kHz = hw.band6_3kHz,
            band16kHz = hw.band16kHz
        )
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Imports and applies a PEQ profile (from peqdb, AutoEq, or EqualizerAPO).
     */
    suspend fun importAndApplyPeqProfile(profile: PeqProfile): Result<Unit> {
        addLog("Imported PEQ profile: '${profile.name}' (${profile.filters.size} filters, Preamp: ${profile.preampDb} dB)")
        val tenBand = profile.sampleToTenBand()
        return applyTenBandEq(tenBand)
    }

    /**
     * Queries current Equalizer curve from device.
     */
    suspend fun queryEqualizer(): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        val frame = MdrCommandBuilder.buildGetEqualizerFrame(txSequence)
        val res = sendFrameWithAck(frame)
        touchActivity()
        return res
    }

    /**
     * Toggles Speak-to-Chat (automatic ambient passthrough on user speech).
     */
    suspend fun setSpeakToChat(
        enabled: Boolean,
        sensitivity: Int = 1,
        timeout: Int = 1
    ): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        addLog("Setting Speak-to-Chat: $enabled (sens=$sensitivity, timeout=$timeout)...")
        val frame = MdrCommandBuilder.buildSetSpeakToChatFrame(txSequence, enabled, sensitivity, timeout)
        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _soundFeaturesState.value = _soundFeaturesState.value.copy(
            speakToChat = SpeakToChatState(enabled = enabled, sensitivity = sensitivity, timeout = timeout)
        )
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Toggles DSEE Extreme (audio upscaling).
     */
    suspend fun setDsee(enabled: Boolean): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        addLog("Setting DSEE Extreme: $enabled...")
        val frame = MdrCommandBuilder.buildSetDseeFrame(txSequence, enabled)
        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _soundFeaturesState.value = _soundFeaturesState.value.copy(dseeExtreme = enabled)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Toggles "Pause when headphones are taken off".
     */
    suspend fun setWearingPause(enabled: Boolean): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        addLog("Setting Pause on Removal: $enabled...")
        val frame = MdrCommandBuilder.buildSetWearingPauseFrame(txSequence, enabled)
        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _soundFeaturesState.value = _soundFeaturesState.value.copy(wearingPause = enabled)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Configures Auto Power-Off timer (0=Never, 1=5m, 2=30m, 3=60m, 4=180m).
     */
    suspend fun setAutoPowerOff(timerIndex: Int): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        addLog("Setting Auto Power-Off timer index: $timerIndex...")
        val frame = MdrCommandBuilder.buildSetAutoPowerOffFrame(txSequence, timerIndex)
        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _soundFeaturesState.value = _soundFeaturesState.value.copy(autoPowerOffIndex = timerIndex)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Toggles Touch Sensor Control Panel on headphone earcup.
     */
    suspend fun setTouchSensor(enabled: Boolean): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        addLog("Setting Touch Sensor Panel: $enabled...")
        val frame = MdrCommandBuilder.buildSetTouchSensorFrame(txSequence, enabled)
        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _soundFeaturesState.value = _soundFeaturesState.value.copy(touchSensorEnabled = enabled)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Toggles Sound Quality Mode (Sound Quality Priority LDAC vs Stable Connection AAC/SBC).
     */
    suspend fun setSoundQualityPriority(qualityPriority: Boolean): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connectInternal()
            if (conn.isFailure) return conn
        }
        addLog("Setting Sound Quality Mode: ${if (qualityPriority) "Priority on Sound Quality" else "Priority on Stable Connection"}...")
        val frame = MdrCommandBuilder.buildSetSoundQualityPriorityFrame(txSequence, qualityPriority)
        val res = sendFrameWithAck(frame)
        if (res.isFailure) return res

        _soundFeaturesState.value = _soundFeaturesState.value.copy(soundQualityPriority = qualityPriority)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Transmits raw bytes over active Bluetooth socket.
     */
    private suspend fun sendRawBytes(bytes: ByteArray): Boolean {
        return try {
            val wrapper = socketWrapper ?: return false
            wrapper.write(bytes)
            val hex = bytes.joinToString(" ") { "%02X".format(it) }
            logDiagnostic(
                direction = LogDirection.TX,
                opcodeName = "TX_WIRE",
                rawHex = hex,
                payloadHex = if (bytes.size > 8) bytes.copyOfRange(7, bytes.size - 2).joinToString(" ") { "%02X".format(it) } else hex,
                note = "RFCOMM Sent ${bytes.size} bytes"
            )
            true
        } catch (e: Exception) {
            logDiagnostic(LogDirection.ERROR, opcodeName = "TX_ERROR", note = "Write error: ${e.message}")
            false
        }
    }

    /**
     * Transmits host ACK frame acknowledging device frame with sequence (1 - seq).
     */
    private fun sendHostAck(ackSeq: Int) {
        scope.launch {
            try {
                val ackFrame = MdrFrameEncoder.buildAck(ackSeq)
                socketWrapper?.write(ackFrame)
                logDiagnostic(LogDirection.ACK_TX, opcodeName = "HOST_ACK", note = "Host ACK sent (seq=$ackSeq)")
            } catch (e: Exception) {
                logDiagnostic(LogDirection.WARN, opcodeName = "HOST_ACK_FAIL", note = "Failed to send host ACK: ${e.message}")
            }
        }
    }

    /**
     * Continuously reads incoming RFCOMM bytes, parses frames, and handles ACKs.
     */
    private fun startReaderLoop(wrapper: BluetoothSocketWrapper) {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            val isStream = wrapper.inputStream ?: return@launch

            try {
                while (isActive && wrapper.isConnected) {
                    val bytesRead = isStream.read(buffer)
                    if (bytesRead <= 0) {
                        break
                    }

                    val packets = frameDecoder.feed(buffer, bytesRead)
                    for (packet in packets) {
                        handleIncomingPacket(packet)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    logDiagnostic(LogDirection.ERROR, opcodeName = "SOCKET_EX", note = "Socket read exception: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (_connectionState.value is ConnectionState.Connected) {
                        _connectionState.value = ConnectionState.Disconnected("Connection closed by peer")
                        logDiagnostic(LogDirection.INFO, opcodeName = "DISCONNECT", note = "RFCOMM connection closed.")
                    }
                }
            }
        }
    }

    /**
     * Dispatches decoded Sony MDR packet.
     */
    private fun handleIncomingPacket(packet: MdrPacket) {
        when (packet.type) {
            MdrConstants.TYPE_ACK -> {
                txSequence = 1 - txSequence
                logDiagnostic(LogDirection.ACK_RX, opcodeName = "DEVICE_ACK", note = "Headset confirmed frame (seq=${packet.sequence})")
                pendingAckWaiter?.complete(packet.sequence)
            }

            MdrConstants.TYPE_COMMAND_1,
            MdrConstants.TYPE_COMMAND_2 -> {
                val ackSeq = 1 - packet.sequence
                sendHostAck(ackSeq)

                val payload = packet.payload
                if (payload.isEmpty()) return

                val opCode = payload[0].toInt() and 0xFF
                val opName = "0x%02X".format(opCode)
                val payloadStr = payload.joinToString(" ") { "%02X".format(it) }
                logDiagnostic(LogDirection.RX, opcode = opCode, opcodeName = "RX_$opName", payloadHex = payloadStr, note = "Packet Type ${packet.type}")

                // Check for Handshake Response (0x01)
                if (MdrResponseParser.isHandshakeResponse(payload)) {
                    pendingHandshakeWaiter?.complete(payload)
                    return
                }

                // Check for ASC state packet (0x67 RET or 0x69 NOTIFY)
                if (MdrResponseParser.isAscStatePacket(payload)) {
                    val ascState = MdrResponseParser.parseAscState(payload, _noiseControlState.value)
                    if (ascState != null) {
                        _noiseControlState.value = ascState
                        pendingAscWaiter?.complete(ascState)
                        val opcodeName = if (payload[0] == MdrConstants.OP_ASC_NOTIFY) "NOTIFY (Headset Button)" else "GET_RESPONSE"
                        addLog("Received $opcodeName: ${ascState.mode.label} (Level ${ascState.ambientLevel})")
                    }
                    touchActivity()
                    return
                }

                // Check for Battery Telemetry (0x23 RET or 0x24 NOTIFY)
                if (payload.size >= 3 && (payload[0] == MdrConstants.OP_BATTERY_RET || payload[0] == MdrConstants.OP_BATTERY_NOTIFY)) {
                    val battery = MdrResponseParser.parseBatteryState(payload)
                    if (battery != null) {
                        _batteryState.value = battery
                        addLog("Battery Telemetry: ${battery.displayString}")
                    }
                    touchActivity()
                    return
                }

                // Check for Equalizer Telemetry (0x57 RET or 0x59 NOTIFY)
                if (payload.size >= 3 && (payload[0] == MdrConstants.OP_EQ_RET || payload[0] == MdrConstants.OP_EQ_NOTIFY)) {
                    val eq = MdrResponseParser.parseEqualizerState(payload)
                    if (eq != null) {
                        _equalizerState.value = eq
                        addLog("Equalizer State: ${eq.preset.displayName} (Clear Bass: ${eq.clearBass})")
                    }
                    touchActivity()
                    return
                }

                // Check for Speak-to-Chat (0xF7 RET or 0xF9 NOTIFY)
                if (payload.size >= 3 && (payload[0] == MdrConstants.OP_STC_RET || payload[0] == MdrConstants.OP_STC_NOTIFY)) {
                    val stc = MdrResponseParser.parseSpeakToChatState(payload)
                    if (stc != null) {
                        _soundFeaturesState.value = _soundFeaturesState.value.copy(speakToChat = stc)
                        addLog("Speak-to-Chat State: ${if (stc.enabled) "ON" else "OFF"}")
                    }
                    touchActivity()
                    return
                }

                // Check for DSEE Extreme (0xE7 RET or 0xE9 NOTIFY)
                if (payload.size >= 3 && (payload[0] == MdrConstants.OP_DSEE_RET || payload[0] == MdrConstants.OP_DSEE_NOTIFY)) {
                    val dsee = MdrResponseParser.parseDseeState(payload)
                    if (dsee != null) {
                        _soundFeaturesState.value = _soundFeaturesState.value.copy(dseeExtreme = dsee)
                        addLog("DSEE Extreme State: ${if (dsee) "AUTO" else "OFF"}")
                    }
                    touchActivity()
                    return
                }
            }
        }
    }

    /**
     * Disconnects and cleans up resources.
     */
    suspend fun disconnect() = commandMutex.withLock {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        idleDisconnectJob?.cancel()
        idleDisconnectJob = null
        readJob?.cancel()
        readJob = null
        socketWrapper?.close()
        socketWrapper = null
        frameDecoder.reset()
        _connectionState.value = ConnectionState.Disconnected()
    }

    companion object {
        private const val TAG = "AudioBluetoothManager"
        private const val PREFS_SETTINGS = "audio_tiles_settings"
        private const val KEY_TILE_CYCLE_SCHEME = "key_tile_cycle_scheme"
        private const val KEY_PREFERRED_DEVICE_ADDR = "key_preferred_device_addr"

        @Volatile
        private var INSTANCE: BluetoothConnectionManager? = null

        fun getInstance(context: Context): BluetoothConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothConnectionManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
