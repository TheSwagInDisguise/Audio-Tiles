package com.sony.xm6controller.data.protocol

import java.io.ByteArrayOutputStream

/**
 * Sony MDR Frame Decoder.
 *
 * Handles streaming byte reception, frame delimiter detection (0x3E .. 0x3C),
 * unescaping, length validation, and checksum verification.
 */
class MdrFrameDecoder {

    private val buffer = ByteArrayOutputStream(512)
    private var inFrame = false

    /**
     * Feeds incoming raw bytes from Bluetooth socket.
     * Returns a list of all successfully decoded packets within the chunk.
     */
    fun feed(data: ByteArray, length: Int = data.size): List<MdrPacket> {
        val packets = mutableListOf<MdrPacket>()
        for (i in 0 until length) {
            val b = data[i]
            when (b) {
                MdrConstants.START_BYTE -> {
                    inFrame = true
                    buffer.reset()
                }
                MdrConstants.STOP_BYTE -> {
                    if (inFrame) {
                        inFrame = false
                        val rawFrame = buffer.toByteArray()
                        buffer.reset()
                        tryDecode(rawFrame)?.let { packets.add(it) }
                    }
                }
                else -> {
                    if (inFrame) {
                        buffer.write(b.toInt())
                    }
                }
            }
        }
        return packets
    }

    /**
     * Resets internal buffer state on socket disconnect or error.
     */
    fun reset() {
        buffer.reset()
        inFrame = false
    }

    companion object {
        /**
         * Unescapes bytes that were escaped with 0x3D.
         * 0x3D followed by byte b -> b | 0x10
         */
        fun unescape(escaped: ByteArray): ByteArray {
            val out = ByteArrayOutputStream(escaped.size)
            var i = 0
            while (i < escaped.size) {
                val b = escaped[i]
                if (b == MdrConstants.ESCAPE_BYTE && i + 1 < escaped.size) {
                    val next = escaped[i + 1]
                    // Restore escaped byte: next | 0x10
                    val restored = (next.toInt() or (MdrConstants.UNESCAPE_MASK.toInt() and 0xFF)).toByte()
                    out.write(restored.toInt())
                    i += 2
                } else {
                    out.write(b.toInt())
                    i++
                }
            }
            return out.toByteArray()
        }

        /**
         * Decodes a single unescaped frame body into an MdrPacket.
         * Validates minimum frame size, payload length prefix, and checksum.
         */
        fun tryDecode(escapedFrame: ByteArray): MdrPacket? {
            val body = unescape(escapedFrame)
            // Minimum body: 1B type + 1B seq + 4B len + 0B payload + 1B checksum = 7 bytes
            if (body.size < 7) {
                return null
            }

            // Verify checksum
            if (!MdrChecksum.verify(body)) {
                return null
            }

            val type = body[0]
            val seq = body[1].toInt() and 0xFF

            // Parse 32-bit big-endian payload length
            val plen = ((body[2].toInt() and 0xFF) shl 24) or
                    ((body[3].toInt() and 0xFF) shl 16) or
                    ((body[4].toInt() and 0xFF) shl 8) or
                    (body[5].toInt() and 0xFF)

            val payloadStart = 6
            val payloadEnd = body.size - 1 // Exclude checksum byte
            val actualPayloadLen = payloadEnd - payloadStart

            if (plen != actualPayloadLen || plen < 0) {
                return null
            }

            val payload = ByteArray(plen)
            if (plen > 0) {
                System.arraycopy(body, payloadStart, payload, 0, plen)
            }

            return MdrPacket(type = type, sequence = seq, payload = payload)
        }
    }
}
