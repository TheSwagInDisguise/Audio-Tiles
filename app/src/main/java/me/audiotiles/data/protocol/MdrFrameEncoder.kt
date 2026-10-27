package me.audiotiles.data.protocol

import java.io.ByteArrayOutputStream

/**
 * Sony MDR Frame Encoder.
 *
 * Constructs framed wire messages:
 * 0x3E + ESCAPE(type + seq + length_BE_4 + payload + checksum) + 0x3C
 */
object MdrFrameEncoder {

    /**
     * Escapes Sony MDR reserved bytes (0x3C, 0x3D, 0x3E).
     */
    fun escape(raw: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(raw.size + 4)
        for (b in raw) {
            when (b) {
                MdrConstants.START_BYTE,
                MdrConstants.STOP_BYTE,
                MdrConstants.ESCAPE_BYTE -> {
                    out.write(MdrConstants.ESCAPE_BYTE.toInt())
                    out.write((b.toInt() and (MdrConstants.ESCAPE_MASK.toInt() and 0xFF)))
                }
                else -> {
                    out.write(b.toInt())
                }
            }
        }
        return out.toByteArray()
    }

    /**
     * Enqueue a full MDR frame from message type, sequence number, and payload bytes.
     *
     * @param type Message type (e.g. 0x0C for COMMAND_1, 0x01 for ACK)
     * @param seq Sequence number (0 or 1)
     * @param payload Raw payload bytes (e.g., [0x68, 0x17, 0x01, ...])
     * @return Fully framed and escaped byte array ready to transmit over RFCOMM socket
     */
    fun encode(type: Byte, seq: Int, payload: ByteArray = ByteArray(0)): ByteArray {
        val payloadLen = payload.size
        // 1B type + 1B seq + 4B len + N payload + 1B checksum
        val bodyLen = 1 + 1 + 4 + payloadLen + 1
        val body = ByteArray(bodyLen)

        // Byte 0: Message Type (0x0C for Command, 0x01 for ACK)
        body[0] = type
        // Byte 1: Sequence Number (0 or 1)
        body[1] = (seq and 0xFF).toByte()
        // Bytes 2-5: Payload Length (32-bit Big Endian)
        body[2] = ((payloadLen ushr 24) and 0xFF).toByte()
        body[3] = ((payloadLen ushr 16) and 0xFF).toByte()
        body[4] = ((payloadLen ushr 8) and 0xFF).toByte()
        body[5] = (payloadLen and 0xFF).toByte()

        // Bytes 6 .. 6+payloadLen-1: Payload
        if (payloadLen > 0) {
            System.arraycopy(payload, 0, body, 6, payloadLen)
        }

        // Byte bodyLen-1: Checksum (modular sum mod 256 of all preceding bytes)
        var sum = 0
        for (i in 0 until bodyLen - 1) {
            sum += (body[i].toInt() and 0xFF)
        }
        body[bodyLen - 1] = (sum and 0xFF).toByte()

        // Escape reserved bytes
        val escapedBody = escape(body)

        // Final packet: 0x3E + escapedBody + 0x3C
        val packet = ByteArray(escapedBody.size + 2)
        packet[0] = MdrConstants.START_BYTE
        System.arraycopy(escapedBody, 0, packet, 1, escapedBody.size)
        packet[packet.size - 1] = MdrConstants.STOP_BYTE

        return packet
    }

    /**
     * Builds an ACK packet acknowledging a received command sequence.
     * In Sony MDR v2 protocol, ACK payload is empty (len=0) and carries (1 - seq) or seq.
     */
    fun buildAck(seq: Int): ByteArray {
        return encode(MdrConstants.TYPE_ACK, seq, ByteArray(0))
    }
}
