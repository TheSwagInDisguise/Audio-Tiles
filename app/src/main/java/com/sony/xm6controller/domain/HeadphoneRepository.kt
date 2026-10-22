package com.sony.xm6controller.domain

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.sony.xm6controller.data.bluetooth.BluetoothConnectionManager
import com.sony.xm6controller.data.bluetooth.ConnectionState
import com.sony.xm6controller.data.bluetooth.DeviceScanner
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean Architecture Repository providing unified domain access to Sony Headphones and Earbuds.
 */
class HeadphoneRepository(private val context: Context) {

    private val connectionManager = BluetoothConnectionManager.getInstance(context)
    private val scanner = DeviceScanner(context)

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val noiseControlState: StateFlow<NoiseControlState> = connectionManager.noiseControlState
    val batteryState: StateFlow<BatteryState> = connectionManager.batteryState
    val equalizerState: StateFlow<EqualizerState> = connectionManager.equalizerState
    val soundFeaturesState: StateFlow<SoundFeaturesState> = connectionManager.soundFeaturesState
    val tileCycleScheme: StateFlow<TileCycleScheme> = connectionManager.tileCycleScheme
    val targetDevice: StateFlow<BluetoothDevice?> = connectionManager.targetDevice
    val diagnosticLogs: StateFlow<List<String>> = connectionManager.diagnosticLogs

    fun isBluetoothAvailable(): Boolean = scanner.isBluetoothEnabled()
    fun hasBluetoothPermission(): Boolean = scanner.hasBluetoothPermission()
    fun getPairedDevices(): List<BluetoothDevice> = scanner.getPairedDevices()
    fun refreshTargetDevice(preferredAddress: String? = null) = connectionManager.refreshTargetDevice(preferredAddress)
    fun setTileCycleScheme(scheme: TileCycleScheme) = connectionManager.setTileCycleScheme(scheme)

    suspend fun connect(target: BluetoothDevice? = null): Result<Unit> {
        return connectionManager.connect(target)
    }

    suspend fun disconnect() {
        connectionManager.disconnect()
    }

    suspend fun setMode(mode: NoiseControlMode): Result<NoiseControlMode> {
        return connectionManager.setMode(mode)
    }

    suspend fun cycleMode(): Result<NoiseControlMode> {
        return connectionManager.cycleMode()
    }

    suspend fun setAmbientLevel(level: Int): Result<Unit> {
        return connectionManager.setAmbientLevel(level)
    }

    suspend fun setFocusOnVoice(enabled: Boolean): Result<Unit> {
        return connectionManager.setFocusOnVoice(enabled)
    }

    suspend fun setWindNoiseReduction(enabled: Boolean): Result<Unit> {
        return connectionManager.setWindNoiseReduction(enabled)
    }

    suspend fun queryBattery(): Result<Unit> {
        return connectionManager.queryBattery()
    }

    // --- Sound Connect Features ---

    suspend fun setEqualizerPreset(preset: EqPreset): Result<Unit> {
        return connectionManager.setEqualizerPreset(preset)
    }

    suspend fun setEqualizerBands(preset: EqPreset, clearBass: Int, bands: List<Int>): Result<Unit> {
        return connectionManager.setEqualizerBands(preset, clearBass, bands)
    }

    suspend fun queryEqualizer(): Result<Unit> {
        return connectionManager.queryEqualizer()
    }

    suspend fun setSpeakToChat(enabled: Boolean, sensitivity: Int = 1, timeout: Int = 1): Result<Unit> {
        return connectionManager.setSpeakToChat(enabled, sensitivity, timeout)
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
        return connectionManager.setSoundQualityPriority(qualityPriority)
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
