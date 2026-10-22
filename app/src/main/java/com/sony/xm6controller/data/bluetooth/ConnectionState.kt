package com.sony.xm6controller.data.bluetooth

/**
 * Reactive state of the Bluetooth RFCOMM connection to Sony WH-1000XM6.
 */
sealed class ConnectionState {
    data class Disconnected(val reason: String? = null) : ConnectionState()
    data class Connecting(val deviceName: String, val address: String) : ConnectionState()
    data class Connected(
        val deviceName: String,
        val address: String,
        val protocolVersion: String = "MDR v2"
    ) : ConnectionState()
    data class Error(val message: String, val canRetry: Boolean = true) : ConnectionState()

    val isConnected: Boolean
        get() = this is Connected

    val isConnecting: Boolean
        get() = this is Connecting
}
