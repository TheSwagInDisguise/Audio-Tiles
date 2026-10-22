package com.sony.xm6controller.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Handles Bluetooth device discovery from paired list and model matching.
 */
class DeviceScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    /**
     * Checks if required Bluetooth runtime permissions are granted.
     */
    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if Bluetooth adapter is enabled on the phone.
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Finds the paired Sony WH-1000XM6 headphone.
     *
     * Priority:
     * 1. Exact match for "WH-1000XM6"
     * 2. Device containing "1000XM6"
     * 3. Fallback to any paired Sony 1000X series device ("WH-1000XM5", "WH-1000XM4")
     */
    @SuppressLint("MissingPermission")
    fun findTargetDevice(preferredAddress: String? = null): BluetoothDevice? {
        if (!hasBluetoothPermission() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return null
        }

        val paired = try {
            bluetoothAdapter.bondedDevices ?: emptySet()
        } catch (_: SecurityException) {
            return null
        }

        if (preferredAddress != null) {
            val preferred = paired.firstOrNull { it.address.equals(preferredAddress, ignoreCase = true) }
            if (preferred != null) return preferred
        }

        // 1. Direct XM6 match
        val xm6 = paired.firstOrNull { dev ->
            val name = dev.name ?: ""
            name.contains("WH-1000XM6", ignoreCase = true) ||
                    name.contains("1000XM6", ignoreCase = true)
        }
        if (xm6 != null) return xm6

        // 2. Fallback to XM series if user is testing with XM5 or XM4
        return paired.firstOrNull { dev ->
            val name = dev.name ?: ""
            name.contains("WH-1000XM", ignoreCase = true) ||
                    name.contains("WF-1000XM", ignoreCase = true) ||
                    name.contains("ULT900N", ignoreCase = true)
        }
    }

    /**
     * Returns all paired Bluetooth audio devices for the UI selector.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission() || bluetoothAdapter == null) {
            return emptyList()
        }
        return try {
            bluetoothAdapter.bondedDevices.toList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}
