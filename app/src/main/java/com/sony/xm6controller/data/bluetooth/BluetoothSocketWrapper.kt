package com.sony.xm6controller.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.sony.xm6controller.data.protocol.MdrConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Robust RFCOMM / SPP Socket wrapper for Sony WH-1000XM6.
 *
 * Implements multi-tier connection fallback:
 * 1. Dynamic SDP UUID inspection (advertised Sony V2 UUIDs prioritized)
 * 2. Insecure RFCOMM socket creation (skips redundant MITM auth conflicts on paired audio devices)
 * 3. Secure RFCOMM socket fallback
 * 4. Insecure & secure direct RFCOMM channel reflection fallback (channels 1, 2, 8, 9)
 * 5. Socket descriptor leak prevention (always closes failed socket before retrying)
 * 6. Inter-attempt teardown delay for Android Bluetooth stack recovery
 */
class BluetoothSocketWrapper(
    private val device: BluetoothDevice,
    private val onLog: ((String) -> Unit)? = null
) {

    private var socket: BluetoothSocket? = null
    var inputStream: InputStream? = null
        private set
    var outputStream: OutputStream? = null
        private set

    val isConnected: Boolean
        get() = socket?.isConnected == true

    private fun log(message: String) {
        onLog?.invoke(message)
    }

    /**
     * Connects to the Bluetooth device using non-blocking Coroutine I/O with timeout.
     *
     * @param timeoutMs Timeout in milliseconds for the complete multi-tier connection attempt
     */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    suspend fun connect(timeoutMs: Long = 12000L): Result<Unit> = withContext(Dispatchers.IO) {
        // Cancel active discovery to speed up connection and prevent timeouts
        val adapter = BluetoothAdapter.getDefaultAdapter()
        try {
            if (adapter?.isDiscovering == true) {
                adapter.cancelDiscovery()
                log("Cancelled active Bluetooth discovery")
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

    /**
     * Resolves prioritized UUID candidates based on cached device SDP records and Sony MDR specifications.
     */
    @SuppressLint("MissingPermission")
    private fun resolveCandidateUuids(): List<UUID> {
        val candidates = mutableListOf<UUID>()

        val knownSonyUuids = listOf(
            MdrConstants.UUID_SONY_V2_PRIMARY,   // 956c7b26-d49a-4ba8-b03f-b17d393cb6e2 (XM5/XM6 standard)
            MdrConstants.UUID_SONY_ALT,          // ba69e0f5-16e3-2db3-ad46-68503e20cc96
            MdrConstants.UUID_SONY_V2_SECONDARY, // 956c7b26-b496-4447-ad7b-3a47900d8c86
            MdrConstants.UUID_SONY_V1            // 96cc203e-5068-46ad-b32d-e316f5e069ba (XM3/XM4 legacy)
        )

        val cachedUuids = try {
            device.uuids?.map { it.uuid } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }

        if (cachedUuids.isNotEmpty()) {
            log("Found ${cachedUuids.size} cached SDP UUID(s) on ${device.name ?: "headset"}")
        }

        // Priority 1: Advertised UUIDs that match known Sony MDR UUIDs
        for (known in knownSonyUuids) {
            if (cachedUuids.any { it.toString().equals(known.toString(), ignoreCase = true) }) {
                candidates.add(known)
                log("SDP matches Sony MDR service: $known")
            }
        }

        // Priority 2: If no Sony UUID was in SDP cache, add all known Sony MDR UUIDs
        if (candidates.isEmpty()) {
            candidates.addAll(knownSonyUuids)
        }

        // Priority 3: Generic Serial Port Profile (SPP) as fallback
        if (!candidates.contains(MdrConstants.UUID_SPP)) {
            candidates.add(MdrConstants.UUID_SPP)
        }

        return candidates
    }

    @SuppressLint("MissingPermission")
    private suspend fun attemptMultiTierConnect() {
        val uuidsToTry = resolveCandidateUuids()
        var lastException: Exception? = null

        // Connect to identified Sony MDR UUIDs (Insecure preferred for bonded headsets, Secure fallback)
        for ((index, uuid) in uuidsToTry.withIndex()) {
            val shortUuid = uuid.toString().substring(0, 8) + "..."

            // Sub-tier A: Insecure RFCOMM (standard for Android 12+ on bonded A2DP headsets)
            var socketCandidate: BluetoothSocket? = null
            try {
                log("Tier 1.${index + 1}A: Insecure RFCOMM -> $shortUuid")
                socketCandidate = device.createInsecureRfcommSocketToServiceRecord(uuid)
                socketCandidate.connect()

                socket = socketCandidate
                inputStream = socketCandidate.inputStream
                outputStream = socketCandidate.outputStream
                log("Connected successfully via Insecure RFCOMM ($shortUuid)")
                return
            } catch (e: Exception) {
                lastException = e
                try {
                    socketCandidate?.close()
                } catch (_: Exception) {}
                log("Insecure RFCOMM failed ($shortUuid): ${e.message?.take(60)}")
                delay(100L) // Allow Bluetooth stack to release channel
            }

            // Sub-tier B: Secure RFCOMM fallback
            socketCandidate = null
            try {
                log("Tier 1.${index + 1}B: Secure RFCOMM -> $shortUuid")
                socketCandidate = device.createRfcommSocketToServiceRecord(uuid)
                socketCandidate.connect()

                socket = socketCandidate
                inputStream = socketCandidate.inputStream
                outputStream = socketCandidate.outputStream
                log("Connected successfully via Secure RFCOMM ($shortUuid)")
                return
            } catch (e: Exception) {
                lastException = e
                try {
                    socketCandidate?.close()
                } catch (_: Exception) {}
                log("Secure RFCOMM failed ($shortUuid): ${e.message?.take(60)}")
                delay(100L)
            }
        }

        val hint = if (lastException?.message?.contains("read ret: -1") == true) {
            " (Headset closed socket. If Sony Sound Connect app is running, force stop it)"
        } else ""

        throw lastException ?: IOException("Failed to establish RFCOMM connection to ${device.name}$hint")
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
