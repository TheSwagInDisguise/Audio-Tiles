package com.sony.xm6controller.data.protocol

/**
 * Representation of an unescaped Sony MDR protocol packet.
 *
 * @property type Message type (e.g., 0x01 for ACK, 0x0C for COMMAND_1, 0x0E for COMMAND_2)
 * @property sequence Sequence number (0 or 1)
 * @property payload Command payload bytes (excluding framing, length header, and checksum)
 */
data class MdrPacket(
    val type: Byte,
    val sequence: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MdrPacket
        if (type != other.type) return false
        if (sequence != other.sequence) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.toInt()
        result = 31 * result + sequence
        result = 31 * result + payload.contentHashCode()
        return result
    }

    fun toHexString(): String {
        val typeHex = "%02X".format(type)
        val payloadHex = payload.joinToString("") { "%02X".format(it) }
        return "Type=0x$typeHex Seq=$sequence Len=${payload.size} Payload=[$payloadHex]"
    }
}
