package me.audiotiles.domain

import android.bluetooth.BluetoothDevice
import android.content.Context
import me.audiotiles.data.bluetooth.BluetoothConnectionManager
import me.audiotiles.data.bluetooth.ConnectionState
import me.audiotiles.data.bluetooth.DeviceScanner
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean Architecture Repository providing unified domain access to Wireless Headphones and Earbuds.
 */
class HeadphoneRepository(private val context: Context) {

    private val connectionManager = BluetoothConnectionManager.getInstance(context)
    private val scanner = DeviceScanner(context)

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val noiseControlState: StateFlow<NoiseControlState> = connectionManager.noiseControlState
    val batteryState: StateFlow<BatteryState> = connectionManager.batteryState
    val equalizerState: StateFlow<EqualizerState> = connectionManager.equalizerState
    val tenBandEqState: StateFlow<TenBandEqState> = connectionManager.tenBandEqState
    val soundFeaturesState: StateFlow<SoundFeaturesState> = connectionManager.soundFeaturesState
    val tileCycleScheme: StateFlow<TileCycleScheme> = connectionManager.tileCycleScheme
    val targetDevice: StateFlow<BluetoothDevice?> = connectionManager.targetDevice
    val deviceCapabilities: StateFlow<DeviceCapabilities> = connectionManager.deviceCapabilities
    val isEqBypassed: StateFlow<Boolean> = connectionManager.isEqBypassed
    val userPresets: StateFlow<List<UserEqPreset>> = connectionManager.userPresets
    val diagnosticLogs: StateFlow<List<String>> = connectionManager.diagnosticLogs
    val diagnosticEntries: StateFlow<List<me.audiotiles.data.bluetooth.MdrDiagnosticLogEntry>> = connectionManager.diagnosticEntries

    val autoEqCatalog: List<AutoEqCatalogEntry> = AutoEqCatalog.entries

    val dynamicBassManager: DynamicBassManager by lazy { DynamicBassManager(context, this) }

    fun notifyWidgetUpdates() {
        try {
            me.audiotiles.widget.AudioCompactWidgetProvider.notifyUpdate(context)
            me.audiotiles.widget.AudioStudioWidgetProvider.notifyUpdate(context)
        } catch (_: Throwable) {}
    }

    fun isBluetoothAvailable(): Boolean = scanner.isBluetoothEnabled()
    fun hasBluetoothPermission(): Boolean = scanner.hasBluetoothPermission()
    fun getPairedDevices(): List<BluetoothDevice> = scanner.getPairedDevices()
    fun refreshTargetDevice(preferredAddress: String? = null) = connectionManager.refreshTargetDevice(preferredAddress)
    fun setTileCycleScheme(scheme: TileCycleScheme) = connectionManager.setTileCycleScheme(scheme)

    fun exportDiagnosticReport(): String = connectionManager.exportDiagnosticReport()
    fun clearDiagnosticLogs() = connectionManager.clearDiagnosticLogs()
    fun toggleEqBypass() {
        connectionManager.toggleEqBypass()
        notifyWidgetUpdates()
    }
    fun saveUserPreset(name: String, state: TenBandEqState) = connectionManager.saveUserPreset(name, state)
    fun deleteUserPreset(presetId: String) = connectionManager.deleteUserPreset(presetId)
    fun loadUserPresets(): List<UserEqPreset> = connectionManager.loadUserPresets()

    suspend fun connect(target: BluetoothDevice? = null): Result<Unit> {
        val res = connectionManager.connect(target)
        notifyWidgetUpdates()
        return res
    }

    suspend fun disconnect() {
        connectionManager.disconnect()
        notifyWidgetUpdates()
    }

    suspend fun setMode(mode: NoiseControlMode): Result<NoiseControlMode> {
        val res = connectionManager.setMode(mode)
        notifyWidgetUpdates()
        return res
    }

    suspend fun setNoiseControlMode(mode: NoiseControlMode): Result<NoiseControlMode> = setMode(mode)

    suspend fun cycleMode(): Result<NoiseControlMode> {
        val res = connectionManager.cycleMode()
        notifyWidgetUpdates()
        return res
    }

    suspend fun cycleNoiseControlMode(): Result<NoiseControlMode> = cycleMode()

    suspend fun setAmbientLevel(level: Int): Result<Unit> {
        val res = connectionManager.setAmbientLevel(level)
        notifyWidgetUpdates()
        return res
    }

    suspend fun setFocusOnVoice(enabled: Boolean): Result<Unit> {
        val res = connectionManager.setFocusOnVoice(enabled)
        notifyWidgetUpdates()
        return res
    }

    suspend fun setAmbientSound(level: Int, focusOnVoice: Boolean): Result<Unit> {
        val r1 = connectionManager.setAmbientLevel(level)
        val r2 = connectionManager.setFocusOnVoice(focusOnVoice)
        notifyWidgetUpdates()
        return if (r1.isSuccess && r2.isSuccess) Result.success(Unit) else (if (r1.isFailure) r1 else r2)
    }

    suspend fun setWindNoiseReduction(enabled: Boolean): Result<Unit> {
        val res = connectionManager.setWindNoiseReduction(enabled)
        notifyWidgetUpdates()
        return res
    }

    suspend fun queryBattery(): Result<Unit> {
        return connectionManager.queryBattery()
    }

    // --- Sound Connect Features ---

    suspend fun setEqualizerPreset(preset: EqPreset): Result<Unit> {
        val res = connectionManager.setEqualizerPreset(preset)
        notifyWidgetUpdates()
        return res
    }

    suspend fun setEqualizerBands(
        preset: EqPreset,
        clearBass: Int,
        bands: List<Int>,
        isDynamicCompensation: Boolean = false
    ): Result<Unit> {
        if (!isDynamicCompensation) {
            dynamicBassManager.onManualClearBassChanged(clearBass)
        }
        val res = connectionManager.setEqualizerBands(preset, clearBass, bands)
        notifyWidgetUpdates()
        return res
    }

    suspend fun queryEqualizer(): Result<Unit> {
        return connectionManager.queryEqualizer()
    }

    suspend fun applyTenBandEq(state: TenBandEqState): Result<Unit> {
        val res = connectionManager.applyTenBandEq(state)
        notifyWidgetUpdates()
        return res
    }

    suspend fun importAndApplyPeqProfile(profile: PeqProfile): Result<Unit> {
        val res = connectionManager.importAndApplyPeqProfile(profile)
        notifyWidgetUpdates()
        return res
    }

    suspend fun setSpeakToChat(enabled: Boolean, sensitivity: Int = 1, timeout: Int = 1): Result<Unit> {
        val res = connectionManager.setSpeakToChat(enabled, sensitivity, timeout)
        notifyWidgetUpdates()
        return res
    }

    suspend fun setDsee(enabled: Boolean): Result<Unit> {
        return connectionManager.setDsee(enabled)
    }

    suspend fun setWearingPause(enabled: Boolean): Result<Unit> {
        return connectionManager.setWearingPause(enabled)
    }

    suspend fun setAutoPowerOff(timerIndex: Int): Result<Unit> {
        return connectionManager.setAutoPowerOff(timerIndex)
    }

    suspend fun setTouchSensor(enabled: Boolean): Result<Unit> {
        return connectionManager.setTouchSensor(enabled)
    }

    suspend fun setSoundQualityPriority(qualityPriority: Boolean): Result<Unit> {
        val res = connectionManager.setSoundQualityPriority(qualityPriority)
        notifyWidgetUpdates()
        return res
    }

    companion object {
        @Volatile
        private var INSTANCE: HeadphoneRepository? = null

        fun getInstance(context: Context): HeadphoneRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HeadphoneRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
