package com.sony.xm6controller.domain

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.sony.xm6controller.data.bluetooth.BluetoothConnectionManager
import com.sony.xm6controller.data.bluetooth.ConnectionState
import com.sony.xm6controller.data.bluetooth.DeviceScanner
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean Architecture Repository providing unified domain access to Sony WH-1000XM6.
 */
class HeadphoneRepository(private val context: Context) {

    private val connectionManager = BluetoothConnectionManager.getInstance(context)
    private val scanner = DeviceScanner(context)

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val noiseControlState: StateFlow<NoiseControlState> = connectionManager.noiseControlState
    val targetDevice: StateFlow<BluetoothDevice?> = connectionManager.targetDevice
    val diagnosticLogs: StateFlow<List<String>> = connectionManager.diagnosticLogs

    fun isBluetoothAvailable(): Boolean = scanner.isBluetoothEnabled()
    fun hasBluetoothPermission(): Boolean = scanner.hasBluetoothPermission()
    fun getPairedDevices(): List<BluetoothDevice> = scanner.getPairedDevices()
    fun refreshTargetDevice(preferredAddress: String? = null) = connectionManager.refreshTargetDevice(preferredAddress)

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
