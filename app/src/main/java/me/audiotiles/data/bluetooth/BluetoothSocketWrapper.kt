package me.audiotiles.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import me.audiotiles.data.protocol.MdrConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.UUID

/**
 * Robust RFCOMM / SPP Socket wrapper for Sony MDR audio hardware.
 *
 * Implements resilient multi-tier connection fallback:
 * 1. Dynamic SDP UUID inspection (advertised Sony MDR UUIDs prioritized)
 * 2. Secure RFCOMM to advertised Sony MDR service records (primary standard for bonded headsets)
 * 3. Direct RFCOMM channel reflection (channel 9 for WF-series TWS earbuds, channel 1, 2, 8 for over-ear)
 * 4. Insecure RFCOMM fallback to advertised Sony MDR UUIDs
 * 5. Secure & Insecure fallback across remaining known Sony MDR UUIDs
 * 6. Generic SPP UUID fallback
 * 7. Socket descriptor leak prevention (always closes failed socket before retrying)
 * 8. Inter-attempt teardown delay for Android Bluetooth stack / Fluoride recovery
 */
class BluetoothSocketWrapper(
    private val device: BluetoothDevice,
    private val onLog: ((String) -> Unit)? = null
) {

    private var socket: BluetoothSocket? = null
    @Volatile
    private var activeConnectingSocket: BluetoothSocket? = null

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
    suspend fun connect(timeoutMs: Long = 18000L): Result<Unit> = withContext(Dispatchers.IO) {
        // Cancel active discovery to speed up connection and prevent timeouts
        val adapter = BluetoothAdapter.getDefaultAdapter()
        try {
            if (adapter?.isDiscovering == true) {
                adapter.cancelDiscovery()
                log("Cancelled active Bluetooth discovery")
            }
        } catch (_: SecurityException) {}

        val deadline = System.currentTimeMillis() + timeoutMs
        val overallWatchdog = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            delay(timeoutMs)
            log("Connection deadline reached ($timeoutMs ms), aborting socket connect syscall...")
            try {
                activeConnectingSocket?.close()
            } catch (_: Exception) {}
            close()
        }

        try {
            attemptMultiTierConnect(deadline)
            Result.success(Unit)
        } catch (e: Exception) {
            close()
            Result.failure(e)
        } finally {
            overallWatchdog.cancel()
            activeConnectingSocket = null
        }
    }

    /**
     * Attempts a single connection with a dedicated timeout and clean socket lifecycle management.
     */
    @SuppressLint("MissingPermission")
    private suspend fun tryConnectSocket(
        socketCandidate: BluetoothSocket,
        description: String,
        attemptTimeoutMs: Long,
        deadlineMs: Long
    ): Result<Unit> {
        if (System.currentTimeMillis() >= deadlineMs) {
            return Result.failure(IOException("Connection deadline reached"))
        }

        val remainingMs = (deadlineMs - System.currentTimeMillis()).coerceAtLeast(1000L)
        val effectiveTimeout = minOf(attemptTimeoutMs, remainingMs)

        var watchdogJob: kotlinx.coroutines.Job? = null
        return try {
            activeConnectingSocket = socketCandidate
            watchdogJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                delay(effectiveTimeout)
                log("Attempt timeout (${effectiveTimeout}ms) reached for $description, closing candidate...")
                try {
                    socketCandidate.close()
                } catch (_: Exception) {}
            }

            log("Attempting $description...")
            socketCandidate.connect()
            watchdogJob.cancel()
            activeConnectingSocket = null

            socket = socketCandidate
            inputStream = socketCandidate.inputStream
            outputStream = socketCandidate.outputStream
            log("Connected successfully via $description")
            Result.success(Unit)
        } catch (e: Exception) {
            watchdogJob?.cancel()
            activeConnectingSocket = null
            try {
                socketCandidate.close()
            } catch (_: Exception) {}
            log("$description failed: ${e.message?.take(60)}")
            delay(120L) // Allow Bluetooth stack / Fluoride to release L2CAP resource
            Result.failure(e)
        }
    }

    /**
     * Creates an RFCOMM socket directly bound to a hardware channel via reflection, bypassing SDP.
     */
    @SuppressLint("MissingPermission")
    private fun createDirectRfcommSocket(channel: Int, secure: Boolean): BluetoothSocket? {
        return try {
            val methodName = if (secure) "createRfcommSocket" else "createInsecureRfcommSocket"
            val method: Method = device.javaClass.getMethod(methodName, Int::class.javaPrimitiveType)
            method.invoke(device, channel) as? BluetoothSocket
        } catch (e: Exception) {
            log("Direct RFCOMM reflection failed (ch=$channel, sec=$secure): ${e.message}")
            null
        }
    }

    /**
     * Returns prioritized direct RFCOMM channels based on device form-factor.
     */
    private fun getDirectRfcommChannels(): List<Int> {
        val name = device.name?.uppercase() ?: ""
        val isTws = name.startsWith("WF-") || name.contains("LINKBUDS")
        return if (isTws) {
            // WF-1000XM4, WF-1000XM5, LinkBuds run MDR on RFCOMM channel 9
            listOf(9, 1, 2, 8)
        } else {
            // WH-1000XM3, XM4, XM5, ULT WEAR run MDR on RFCOMM channel 1, 9, 2, 8
            listOf(1, 9, 2, 8)
        }
    }

    /**
     * Resolves candidate UUIDs partitioned into cached matches and fallbacks.
     */
    @SuppressLint("MissingPermission")
    private fun resolveCandidateUuids(): Pair<List<UUID>, List<UUID>> {
        val knownMdrUuids = listOf(
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

        val matchingUuids = mutableListOf<UUID>()
        for (known in knownMdrUuids) {
            if (cachedUuids.any { it.toString().equals(known.toString(), ignoreCase = true) }) {
                matchingUuids.add(known)
                log("SDP matches Sony MDR service: $known")
            }
        }

        val remainingUuids = knownMdrUuids.filter { !matchingUuids.contains(it) }

        return Pair(matchingUuids, remainingUuids)
    }

    @SuppressLint("MissingPermission")
    private suspend fun attemptMultiTierConnect(deadlineMs: Long) {
        val (matchingUuids, remainingUuids) = resolveCandidateUuids()
        val directChannels = getDirectRfcommChannels()
        var lastException: Exception? = null

        // TIER 1: Secure RFCOMM on matching advertised Sony MDR UUIDs (Standard for bonded devices)
        for (uuid in matchingUuids) {
            if (System.currentTimeMillis() >= deadlineMs) break
            val shortUuid = uuid.toString().substring(0, 8) + "..."
            try {
                val candidate = device.createRfcommSocketToServiceRecord(uuid)
                val res = tryConnectSocket(candidate, "Tier 1: Secure RFCOMM -> $shortUuid", 7000L, deadlineMs)
                if (res.isSuccess) return
                lastException = res.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
                log("Tier 1 create socket failed: ${e.message}")
            }
        }

        // TIER 2: Direct RFCOMM Channel Reflection (Bypasses slow/stale SDP, connects in <200ms)
        // Highly effective for WF-1000XM4 (channel 9) and WH-series (channel 1, 9, 2, 8)
        for (ch in directChannels) {
            if (System.currentTimeMillis() >= deadlineMs) break
            val directSecure = createDirectRfcommSocket(ch, secure = true)
            if (directSecure != null) {
                val res = tryConnectSocket(directSecure, "Tier 2A: Direct Secure RFCOMM (Channel $ch)", 3500L, deadlineMs)
                if (res.isSuccess) return
                lastException = res.exceptionOrNull() as? Exception
            }
            val directInsecure = createDirectRfcommSocket(ch, secure = false)
            if (directInsecure != null) {
                val res = tryConnectSocket(directInsecure, "Tier 2B: Direct Insecure RFCOMM (Channel $ch)", 2500L, deadlineMs)
                if (res.isSuccess) return
                lastException = res.exceptionOrNull() as? Exception
            }
        }

        // TIER 3: Insecure RFCOMM on matching advertised Sony MDR UUIDs
        for (uuid in matchingUuids) {
            if (System.currentTimeMillis() >= deadlineMs) break
            val shortUuid = uuid.toString().substring(0, 8) + "..."
            try {
                val candidate = device.createInsecureRfcommSocketToServiceRecord(uuid)
                val res = tryConnectSocket(candidate, "Tier 3: Insecure RFCOMM -> $shortUuid", 6000L, deadlineMs)
                if (res.isSuccess) return
                lastException = res.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
                log("Tier 3 create socket failed: ${e.message}")
            }
        }

        // TIER 4: Remaining known Sony MDR UUIDs (Secure then Insecure)
        val allRemaining = if (matchingUuids.isEmpty()) {
            listOf(
                MdrConstants.UUID_SONY_V2_PRIMARY,
                MdrConstants.UUID_SONY_ALT,
                MdrConstants.UUID_SONY_V1,
                MdrConstants.UUID_SONY_V2_SECONDARY
            )
        } else {
            remainingUuids
        }

        for (uuid in allRemaining) {
            if (System.currentTimeMillis() >= deadlineMs) break
            val shortUuid = uuid.toString().substring(0, 8) + "..."
            try {
                val candidate = device.createRfcommSocketToServiceRecord(uuid)
                val res = tryConnectSocket(candidate, "Tier 4A: Secure RFCOMM -> $shortUuid", 5000L, deadlineMs)
                if (res.isSuccess) return
                lastException = res.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
            }
            try {
                val candidate = device.createInsecureRfcommSocketToServiceRecord(uuid)
                val res = tryConnectSocket(candidate, "Tier 4B: Insecure RFCOMM -> $shortUuid", 4000L, deadlineMs)
                if (res.isSuccess) return
                lastException = res.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
            }
        }

        // TIER 5: Generic SPP Profile UUID fallback
        if (System.currentTimeMillis() < deadlineMs) {
            try {
                val candidate = device.createRfcommSocketToServiceRecord(MdrConstants.UUID_SPP)
                val res = tryConnectSocket(candidate, "Tier 5: Generic SPP RFCOMM", 4000L, deadlineMs)
                if (res.isSuccess) return
                lastException = res.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
            }
        }

        val hint = when {
            lastException?.message?.contains("read ret: -1") == true ||
            lastException?.message?.contains("Socket failure") == true ||
            lastException?.message?.contains("Bluetooth Socket") == true ->
                " (Headset rejected or closed connection. If OEM Sony Sound Connect is running, force stop it)"
            else -> ""
        }

        throw lastException ?: IOException("Failed to establish RFCOMM connection to ${device.name ?: "headset"}$hint")
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
