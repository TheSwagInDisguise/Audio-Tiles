package com.sony.xm6controller.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.sony.xm6controller.data.protocol.MdrConstants

/**
 * Universal Bluetooth Device Scanner for Sony Headphones and True Wireless Earbuds.
 *
 * Supports:
 * - Over-Ear Flagships: WH-1000XM6, WH-1000XM5, WH-1000XM4, WH-1000XM3, MDR-1000X
 * - Lifestyle & Bass: WH-ULT900N (ULT WEAR), WH-XB910N, WH-XB900N, WH-CH720N, WH-CH520
 * - True Wireless Earbuds: WF-1000XM5, WF-1000XM4, WF-1000XM3, WF-C700N, WF-C510
 * - LinkBuds Series: LinkBuds (WF-L900), LinkBuds S (WF-LS900N), LinkBuds Open, LinkBuds Fit
 * - Neckbands: WI-1000XM2, WI-1000X, WI-C100
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
     * Identifies if a Bluetooth device is a Sony audio product.
     */
    @SuppressLint("MissingPermission")
    fun isSonyAudioDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.uppercase() ?: ""
        if (name.startsWith("WH-") || name.startsWith("WF-") || name.startsWith("WI-") ||
            name.startsWith("MDR-") || name.contains("LINKBUDS") || name.contains("ULT900") ||
            name.contains("1000X") || name.contains("SONY")
        ) {
            return true
        }

        // Check SDP UUIDs for Sony MDR custom service record
        val uuids = try { device.uuids } catch (_: SecurityException) { null }
        if (uuids != null) {
            for (record in uuids) {
                val u = record.uuid
                if (u == MdrConstants.UUID_SONY_V2_PRIMARY ||
                    u == MdrConstants.UUID_SONY_V1 ||
                    u == MdrConstants.UUID_SONY_ALT ||
                    u == MdrConstants.UUID_SONY_V2_SECONDARY
                ) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Finds the target paired Sony device with intelligent fallback priority:
     * 1. Exact match for preferred address (if provided)
     * 2. WH-1000XM6
     * 3. Any 1000X series (WH-1000XM5, WF-1000XM5, XM4, XM3)
     * 4. LinkBuds, ULT WEAR, or other Sony audio products
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
            name.contains("WH-1000XM6", ignoreCase = true) || name.contains("1000XM6", ignoreCase = true)
        }
        if (xm6 != null) return xm6

        // 2. XM5 / XM4 / WF series
        val xmSeries = paired.firstOrNull { dev ->
            val name = dev.name ?: ""
            name.contains("1000XM", ignoreCase = true)
        }
        if (xmSeries != null) return xmSeries

        // 3. Any other Sony headphone/earbud
        return paired.firstOrNull { isSonyAudioDevice(it) }
    }

    /**
     * Returns all paired Bluetooth audio devices for the UI selector,
     * sorting Sony devices to the top.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission() || bluetoothAdapter == null) {
            return emptyList()
        }
        val devices = try {
            bluetoothAdapter.bondedDevices.toList()
        } catch (_: SecurityException) {
            emptyList()
        }
        return devices.sortedByDescending { isSonyAudioDevice(it) }
    }
}
