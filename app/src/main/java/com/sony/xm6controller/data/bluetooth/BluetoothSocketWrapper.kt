package com.sony.xm6controller.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.sony.xm6controller.data.protocol.MdrConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Robust RFCOMM / SPP Socket wrapper for Sony WH-1000XM6.
 *
 * Implements multi-tier connection fallback (Standard SPP -> Sony V2 UUID -> Insecure SPP -> Reflection Channel 1),
 * cancellable coroutine timeouts, and clean stream management.
 */
class BluetoothSocketWrapper(private val device: BluetoothDevice) {

    private var socket: BluetoothSocket? = null
    var inputStream: InputStream? = null
        private set
    var outputStream: OutputStream? = null
        private set

    val isConnected: Boolean
        get() = socket?.isConnected == true

    /**
     * Connects to the Bluetooth device using non-blocking Coroutine I/O with timeout.
     *
     * @param timeoutMs Timeout in milliseconds for the connection attempt
     */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    suspend fun connect(timeoutMs: Long = 6000L): Result<Unit> = withContext(Dispatchers.IO) {
        // Cancel active discovery to speed up connection and prevent timeouts
        val adapter = BluetoothAdapter.getDefaultAdapter()
        try {
            if (adapter?.isDiscovering == true) {
                adapter.cancelDiscovery()
            }
        } catch (_: SecurityException) {}

        try {
            withTimeout(timeoutMs) {
                attemptMultiTierConnect()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            close()
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptMultiTierConnect() {
        val uuidsToTry = listOf(
            MdrConstants.UUID_SPP,
            MdrConstants.UUID_SONY_V2_PRIMARY,
            MdrConstants.UUID_SONY_V2_SECONDARY,
            MdrConstants.UUID_SONY_V1
        )

        var lastException: Exception? = null

        // Tier 1 & 2: Standard Secure SPP and Sony Vendor UUIDs
        for (uuid in uuidsToTry) {
            try {
                val s = device.createRfcommSocketToServiceRecord(uuid)
                s.connect()
                socket = s
                inputStream = s.inputStream
                outputStream = s.outputStream
                return
            } catch (e: IOException) {
                lastException = e
            }
        }

        // Tier 3: Insecure RFCOMM with standard SPP (helps on some OEM stacks like ColorOS)
        try {
            val s = device.createInsecureRfcommSocketToServiceRecord(MdrConstants.UUID_SPP)
            s.connect()
            socket = s
            inputStream = s.inputStream
            outputStream = s.outputStream
            return
        } catch (e: IOException) {
            lastException = e
        }

        // Tier 4: Direct Channel Reflection fallback (bypasses SDP cache issues)
        try {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            val s = method.invoke(device, 1) as BluetoothSocket
            s.connect()
            socket = s
            inputStream = s.inputStream
            outputStream = s.outputStream
            return
        } catch (e: Exception) {
            lastException = e
        }

        throw lastException ?: IOException("Failed to establish RFCOMM connection to ${device.name}")
    }

    /**
     * Sends bytes over the RFCOMM socket.
     */
    suspend fun write(data: ByteArray) = withContext(Dispatchers.IO) {
        val os = outputStream ?: throw IOException("Socket is not connected")
        os.write(data)
        os.flush()
    }

    /**
     * Safely closes the socket and underlying streams.
     */
    fun close() {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}

        inputStream = null
        outputStream = null
        socket = null
    }
}
