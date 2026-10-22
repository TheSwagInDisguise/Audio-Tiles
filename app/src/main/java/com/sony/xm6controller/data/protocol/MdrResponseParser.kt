package com.sony.xm6controller.data.protocol

import com.sony.xm6controller.domain.NoiseControlMode
import com.sony.xm6controller.domain.NoiseControlState

/**
 * Sony MDR Response & Notification Parser.
 *
 * Deserializes incoming MDR payloads into domain state models:
 * - 0x67: Ambient Sound Control GET response
 * - 0x69: Ambient Sound Control unsolicited status notification (e.g. headset button push)
 */
object MdrResponseParser {

    /**
     * Determines if a packet payload is an Ambient Sound Control state packet
     * (either response 0x67 or notify 0x69).
     */
    fun isAscStatePacket(payload: ByteArray): Boolean {
        if (payload.size < 7) return false
        val opcode = payload[0]
        return opcode == MdrConstants.OP_ASC_RET || opcode == MdrConstants.OP_ASC_NOTIFY
    }

    /**
     * Determines if a packet is a handshake response (0x01).
     */
    fun isHandshakeResponse(payload: ByteArray): Boolean {
        return payload.isNotEmpty() && payload[0] == MdrConstants.OP_INIT_RESP
    }

    /**
     * Parses an ASC state packet (0x67 RET or 0x69 NOTIFY).
     *
     * Wire layout:
     * [0] Opcode (0x67 or 0x69)
     * [1] Subtype (0x17 or 0x15)
     * [2] Config Type (0x01)
     * [3] On/Off byte (0x00 = Off, 0x01 = Active)
     * [4] Amb byte (0x00 = Noise Cancelling, 0x01 = Ambient Sound)
     * [5] Wind byte (if 8-byte layout: 0x02 = Off, 0x03/0x05 = Wind noise reduction)
     * [6] Voice focus byte (0x00 or 0x01)
     * [7] Ambient level byte (1..20)
     */
    fun parseAscState(payload: ByteArray): NoiseControlState? {
        if (payload.size < 7) return null
        val opcode = payload[0]
        if (opcode != MdrConstants.OP_ASC_RET && opcode != MdrConstants.OP_ASC_NOTIFY) {
            return null
        }

        val subtype = payload[1]
        val hasWind = (subtype == MdrConstants.SUBTYPE_NC_ASM_V2 && payload.size >= 8)

        val onByte = payload[3].toInt() and 0xFF
        val ambByte = payload[4].toInt() and 0xFF

        val voiceIdx = if (hasWind) 6 else 5
        val levelIdx = voiceIdx + 1

        val voiceByte = if (payload.size > voiceIdx) (payload[voiceIdx].toInt() and 0xFF) else 0
        val levelByte = if (payload.size > levelIdx) (payload[levelIdx].toInt() and 0xFF) else MdrConstants.AMBIENT_LEVEL_DEFAULT

        val mode: NoiseControlMode = when {
            onByte == 0 -> NoiseControlMode.OFF
            ambByte == 1 -> NoiseControlMode.AMBIENT_SOUND
            else -> NoiseControlMode.NOISE_CANCELLING
        }

        val clampedLevel = if (levelByte in MdrConstants.AMBIENT_LEVEL_MIN..MdrConstants.AMBIENT_LEVEL_MAX) {
            levelByte
        } else {
            MdrConstants.AMBIENT_LEVEL_DEFAULT
        }

        return NoiseControlState(
            mode = mode,
            ambientLevel = clampedLevel,
            focusOnVoice = (voiceByte == 1),
            hasWindByte = hasWind,
            subtype = subtype,
            rawPayload = payload.clone()
        )
    }
}
