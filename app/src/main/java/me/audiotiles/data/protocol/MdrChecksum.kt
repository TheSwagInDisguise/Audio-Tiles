package me.audiotiles.data.protocol

/**
 * Sony MDR Protocol Checksum Calculator.
 *
 * Checksum is the 8-bit unsigned modular sum (mod 256) of all body bytes
 * (Message Type + Sequence Number + 4-byte BE Payload Length + Payload bytes).
 * Calculated before escape encoding on TX, and verified after unescaping on RX.
 */
object MdrChecksum {

    /**
     * Computes the 1-byte checksum over raw body bytes.
     */
    fun calculate(body: ByteArray): Byte {
        var sum = 0
        for (b in body) {
            sum += (b.toInt() and 0xFF)
        }
        return (sum and 0xFF).toByte()
    }

    /**
     * Computes the checksum from components without extra byte array allocations.
     */
    fun calculate(type: Byte, seq: Int, payloadLength: Int, payload: ByteArray): Byte {
        var sum = (type.toInt() and 0xFF) + (seq and 0xFF)
        sum += (payloadLength ushr 24) and 0xFF
        sum += (payloadLength ushr 16) and 0xFF
        sum += (payloadLength ushr 8) and 0xFF
        sum += payloadLength and 0xFF

        for (b in payload) {
            sum += (b.toInt() and 0xFF)
        }
        return (sum and 0xFF).toByte()
    }

    /**
     * Validates that the checksum at the end of the unescaped frame matches the body sum.
     */
    fun verify(unescapedFrame: ByteArray): Boolean {
        if (unescapedFrame.size < 7) return false // 1B type + 1B seq + 4B len + 1B checksum
        var sum = 0
        for (i in 0 until unescapedFrame.size - 1) {
            sum += (unescapedFrame[i].toInt() and 0xFF)
        }
        val expected = (sum and 0xFF).toByte()
        val actual = unescapedFrame[unescapedFrame.size - 1]
        return expected == actual
    }
}
