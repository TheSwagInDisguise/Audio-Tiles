package com.sony.xm6controller.data.bluetooth

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.sony.xm6controller.data.protocol.MdrCommandBuilder
import com.sony.xm6controller.data.protocol.MdrConstants
import com.sony.xm6controller.data.protocol.MdrFrameDecoder
import com.sony.xm6controller.data.protocol.MdrFrameEncoder
import com.sony.xm6controller.data.protocol.MdrPacket
import com.sony.xm6controller.data.protocol.MdrResponseParser
import com.sony.xm6controller.domain.BatteryState
import com.sony.xm6controller.domain.EqPreset
import com.sony.xm6controller.domain.EqualizerState
import com.sony.xm6controller.domain.NoiseControlMode
import com.sony.xm6controller.domain.NoiseControlState
import com.sony.xm6controller.domain.SoundFeaturesState
import com.sony.xm6controller.domain.SpeakToChatState
import com.sony.xm6controller.domain.TileCycleScheme
import com.sony.xm6controller.service.XM6ForegroundService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/**
 * Singleton Bluetooth Connection & Session Manager for Sony WH-1000XM6.
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

    private val _soundFeaturesState = MutableStateFlow(SoundFeaturesState())
    val soundFeaturesState: StateFlow<SoundFeaturesState> = _soundFeaturesState.asStateFlow()

    private val _tileCycleScheme = MutableStateFlow(TileCycleScheme.ALL_THREE)
    val tileCycleScheme: StateFlow<TileCycleScheme> = _tileCycleScheme.asStateFlow()

    private val _targetDevice = MutableStateFlow<BluetoothDevice?>(null)
    val targetDevice: StateFlow<BluetoothDevice?> = _targetDevice.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<String>>(emptyList())
    val diagnosticLogs: StateFlow<List<String>> = _diagnosticLogs.asStateFlow()

    init {
        // Initial target scan
        refreshTargetDevice()
        registerBluetoothAclReceiver()
    }

    fun setTileCycleScheme(scheme: TileCycleScheme) {
        _tileCycleScheme.value = scheme
        addLog("Tile cycle scheme changed to: ${scheme.label}")
    }

    fun refreshTargetDevice(preferredAddress: String? = null) {
        val dev = deviceScanner.findTargetDevice(preferredAddress)
        _targetDevice.value = dev
    }

    private fun addLog(message: String) {
        Log.d(TAG, message)
        val current = _diagnosticLogs.value.toMutableList()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        current.add("[$timestamp] $message")
        if (current.size > 50) {
            current.removeAt(0)
        }
        _diagnosticLogs.value = current
    }

    /**
     * Resets the idle disconnect timer to conserve phone and headphone battery.
     * When foreground service is not running, drops the socket after 2 minutes of inactivity.
     */
    private fun touchActivity() {
        idleDisconnectJob?.cancel()
        if (!XM6ForegroundService.isServiceRunning) {
            idleDisconnectJob = scope.launch {
                delay(120_000L) // 2 minutes idle timeout
                if (socketWrapper?.isConnected == true && !XM6ForegroundService.isServiceRunning) {
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
     * Connects to the Sony WH-1000XM6, performs the MDR handshake, and queries initial ANC mode.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(target: BluetoothDevice? = null): Result<Unit> = commandMutex.withLock {
        val device = target ?: _targetDevice.value ?: deviceScanner.findTargetDevice()
        if (device == null) {
            val err = "Sony WH-1000XM6 not found in paired devices."
            _connectionState.value = ConnectionState.Error(err)
            addLog("Connect error: $err")
            return Result.failure(IllegalStateException(err))
        }

        _targetDevice.value = device
        val deviceName = device.name ?: "WH-1000XM6"
        val address = device.address

        if (socketWrapper?.isConnected == true && _connectionState.value is ConnectionState.Connected) {
            addLog("Already connected to $deviceName ($address)")
            touchActivity()
            return Result.success(Unit)
        }

        disconnectInternal()

        _connectionState.value = ConnectionState.Connecting(deviceName, address)
        addLog("Connecting RFCOMM socket to $deviceName ($address)...")

        // Terminate any background Sony Sound Connect / Songpal processes holding the socket
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.killBackgroundProcesses("com.sony.songpal.mdr")
            am?.killBackgroundProcesses("jp.co.sony.songpal.mdr")
        } catch (_: Exception) {}

        val wrapper = BluetoothSocketWrapper(device, ::addLog)
        val connectResult = wrapper.connect(timeoutMs = 14000L)
        if (connectResult.isFailure) {
            val ex = connectResult.exceptionOrNull() ?: IOException("Connection failed")
            val msg = if (ex.message?.contains("read ret: -1") == true || ex.message?.contains("closed") == true) {
                "Failed to connect: Headset closed connection (read ret: -1). If Sony Sound Connect is open, force stop it."
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
        sendRawBytes(frame)
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
        sendRawBytes(frame)
    }

    /**
     * Public battery refresh method.
     */
    suspend fun queryBattery(): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connect()
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
            val conn = connect()
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

        val ackWaiter = CompletableDeferred<Int>()
        pendingAckWaiter = ackWaiter

        if (!sendRawBytes(frame)) {
            pendingAckWaiter = null
            return Result.failure(IOException("Failed to write to Bluetooth socket"))
        }

        val ackReceived = withTimeoutOrNull(2500L) {
            ackWaiter.await()
        }
        pendingAckWaiter = null

        if (ackReceived == null) {
            addLog("Warning: No ACK received for mode switch to ${targetMode.label}")
        } else {
            addLog("ACK received (seq=$ackReceived). Mode applied.")
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
            val conn = connect()
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

        sendRawBytes(frame)
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
            val conn = connect()
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

        sendRawBytes(frame)
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
            val conn = connect()
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

        sendRawBytes(frame)
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
            val conn = connect()
            if (conn.isFailure) return conn
        }
        addLog("Setting EQ Preset: ${preset.displayName}...")
        val frame = MdrCommandBuilder.buildSetEqualizerPresetFrame(txSequence, preset)
        sendRawBytes(frame)
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
            val conn = connect()
            if (conn.isFailure) return conn
        }
        addLog("Applying 5-band Equalizer (Clear Bass: $clearBass)...")
        val frame = MdrCommandBuilder.buildSetEqualizerBandsFrame(
            seq = txSequence,
            preset = preset,
            clearBass = clearBass,
            bands = bands
        )
        sendRawBytes(frame)
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
     * Queries current Equalizer curve from device.
     */
    suspend fun queryEqualizer(): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connect()
            if (conn.isFailure) return conn
        }
        val frame = MdrCommandBuilder.buildGetEqualizerFrame(txSequence)
        sendRawBytes(frame)
        touchActivity()
        return Result.success(Unit)
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
            val conn = connect()
            if (conn.isFailure) return conn
        }
        addLog("Setting Speak-to-Chat: $enabled (sens=$sensitivity, timeout=$timeout)...")
        val frame = MdrCommandBuilder.buildSetSpeakToChatFrame(txSequence, enabled, sensitivity, timeout)
        sendRawBytes(frame)
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
            val conn = connect()
            if (conn.isFailure) return conn
        }
        addLog("Setting DSEE Extreme: $enabled...")
        val frame = MdrCommandBuilder.buildSetDseeFrame(txSequence, enabled)
        sendRawBytes(frame)
        _soundFeaturesState.value = _soundFeaturesState.value.copy(dseeExtreme = enabled)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Toggles "Pause when headphones are taken off".
     */
    suspend fun setWearingPause(enabled: Boolean): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connect()
            if (conn.isFailure) return conn
        }
        addLog("Setting Pause on Removal: $enabled...")
        val frame = MdrCommandBuilder.buildSetWearingPauseFrame(txSequence, enabled)
        sendRawBytes(frame)
        _soundFeaturesState.value = _soundFeaturesState.value.copy(wearingPause = enabled)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Configures Auto Power-Off timer (0=Never, 1=5m, 2=30m, 3=60m, 4=180m).
     */
    suspend fun setAutoPowerOff(timerIndex: Int): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connect()
            if (conn.isFailure) return conn
        }
        addLog("Setting Auto Power-Off timer index: $timerIndex...")
        val frame = MdrCommandBuilder.buildSetAutoPowerOffFrame(txSequence, timerIndex)
        sendRawBytes(frame)
        _soundFeaturesState.value = _soundFeaturesState.value.copy(autoPowerOffIndex = timerIndex)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Toggles Touch Sensor Control Panel on headphone earcup.
     */
    suspend fun setTouchSensor(enabled: Boolean): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connect()
            if (conn.isFailure) return conn
        }
        addLog("Setting Touch Sensor Panel: $enabled...")
        val frame = MdrCommandBuilder.buildSetTouchSensorFrame(txSequence, enabled)
        sendRawBytes(frame)
        _soundFeaturesState.value = _soundFeaturesState.value.copy(touchSensorEnabled = enabled)
        touchActivity()
        return Result.success(Unit)
    }

    /**
     * Toggles Sound Quality Mode (Sound Quality Priority LDAC vs Stable Connection AAC/SBC).
     */
    suspend fun setSoundQualityPriority(qualityPriority: Boolean): Result<Unit> = commandMutex.withLock {
        if (socketWrapper?.isConnected != true) {
            val conn = connect()
            if (conn.isFailure) return conn
        }
        addLog("Setting Sound Quality Mode: ${if (qualityPriority) "Priority on Sound Quality" else "Priority on Stable Connection"}...")
        val frame = MdrCommandBuilder.buildSetSoundQualityPriorityFrame(txSequence, qualityPriority)
        sendRawBytes(frame)
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
            true
        } catch (e: Exception) {
            addLog("Write error: ${e.message}")
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
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send host ACK: ${e.message}")
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
                    addLog("Socket read exception: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (_connectionState.value is ConnectionState.Connected) {
                        _connectionState.value = ConnectionState.Disconnected("Connection closed by peer")
                        addLog("Connection closed.")
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
                pendingAckWaiter?.complete(packet.sequence)
            }

            MdrConstants.TYPE_COMMAND_1,
            MdrConstants.TYPE_COMMAND_2 -> {
                val ackSeq = 1 - packet.sequence
                sendHostAck(ackSeq)

                val payload = packet.payload
                if (payload.isEmpty()) return

                // Check for Handshake Response (0x01)
                if (MdrResponseParser.isHandshakeResponse(payload)) {
                    pendingHandshakeWaiter?.complete(payload)
                    return
                }

                // Check for ASC state packet (0x67 RET or 0x69 NOTIFY)
                if (MdrResponseParser.isAscStatePacket(payload)) {
                    val ascState = MdrResponseParser.parseAscState(payload)
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
        private const val TAG = "XM6BluetoothManager"

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
