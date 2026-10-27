package me.audiotiles.data.protocol

import me.audiotiles.domain.BatteryState
import me.audiotiles.domain.EqPreset
import me.audiotiles.domain.EqualizerState
import me.audiotiles.domain.NoiseControlMode
import me.audiotiles.domain.NoiseControlState
import me.audiotiles.domain.SpeakToChatState

/**
 * Universal Sony MDR Response & Notification Parser.
 *
 * Deserializes incoming MDR payloads into domain state models:
 * - 0x67 / 0x69: Ambient Sound Control
 * - 0x23 / 0x24: Battery Telemetry (Over-Ear single & TWS multi-cell)
 * - 0x57 / 0x59: Equalizer & Clear Bass
 * - 0xF7 / 0xF9: Speak-to-Chat
 * - 0xE7 / 0xE9: DSEE Extreme
 */
object MdrResponseParser {

    /**
     * Determines if a packet payload is an Ambient Sound Control state packet.
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
     * Parses an ASC state packet (0x67 RET or 0x69 NOTIFY) supporting both v1 and v2 layouts.
     * Preserves existing ambientLevel when the packet reports Noise Cancelling or Off mode,
     * preventing dummy status bytes (e.g. 0x01 in ANC packets) from clobbering ambient level to 1.
     */
    fun parseAscState(payload: ByteArray, previousState: NoiseControlState? = null): NoiseControlState? {
        if (payload.size < 6) return null
        val opcode = payload[0]
        if (opcode != MdrConstants.OP_ASC_RET && opcode != MdrConstants.OP_ASC_NOTIFY) {
            return null
        }

        val subtype = payload[1]
        val isV1 = (subtype == MdrConstants.SUBTYPE_NC_ASM_V1 || subtype == 0x01.toByte() || subtype == 0x02.toByte())
        val hasWind = (!isV1 && payload.size >= 8)

        val onByte = payload[3].toInt() and 0xFF
        val ambByte = payload[4].toInt() and 0xFF

        val mode: NoiseControlMode = when {
            onByte == 0 -> NoiseControlMode.OFF
            ambByte == 1 -> NoiseControlMode.AMBIENT_SOUND
            else -> NoiseControlMode.NOISE_CANCELLING
        }

        val voiceIdx = if (hasWind) 6 else 5
        val levelIdx = voiceIdx + 1

        val voiceByte = if (payload.size > voiceIdx) (payload[voiceIdx].toInt() and 0xFF) else 0
        val levelByte = if (payload.size > levelIdx) (payload[levelIdx].toInt() and 0xFF) else MdrConstants.AMBIENT_LEVEL_DEFAULT

        // In ANC or Off mode, the byte at levelIdx is a protocol status/mode byte (often 0x01),
        // NOT the user's ambient sound passthrough volume. Only parse as ambient level when in AMBIENT_SOUND mode.
        val clampedLevel = if (mode == NoiseControlMode.AMBIENT_SOUND && levelByte in MdrConstants.AMBIENT_LEVEL_MIN..MdrConstants.AMBIENT_LEVEL_MAX) {
            levelByte
        } else {
            previousState?.ambientLevel ?: MdrConstants.AMBIENT_LEVEL_DEFAULT
        }

        val windByte = if (hasWind && payload.size > 5) (payload[5].toInt() and 0xFF) else 0
        val isWindNoiseReduction = (windByte == 3 || windByte == 5)

        return NoiseControlState(
            mode = mode,
            ambientLevel = clampedLevel,
            focusOnVoice = (voiceByte == 1),
            windNoiseReduction = isWindNoiseReduction,
            hasWindByte = hasWind,
            subtype = subtype,
            rawPayload = payload.clone()
        )
    }

    /**
     * Parses Battery Telemetry payloads (0x23 RET or 0x24 NOTIFY).
     * Supports both single battery (Over-Ear) and multi-cell telemetry (TWS Earbuds: Left, Right, Case).
     */
    fun parseBatteryState(payload: ByteArray): BatteryState? {
        if (payload.size < 3) return null
        val opcode = payload[0]
        if (opcode != MdrConstants.OP_BATTERY_RET && opcode != MdrConstants.OP_BATTERY_NOTIFY) {
            return null
        }

        // TWS Multi-cell payload (Left, Right, Case)
        // Wire layout: [0x23/0x24, subtype, L_level, L_charge, R_level, R_charge, Case_level, Case_charge]
        if (payload.size >= 8) {
            val left = payload[2].toInt() and 0xFF
            val leftCharge = payload[3].toInt() != 0
            val right = payload[4].toInt() and 0xFF
            val rightCharge = payload[5].toInt() != 0
            val case = payload[6].toInt() and 0xFF
            val caseCharge = payload[7].toInt() != 0

            return BatteryState(
                level = maxOf(left, right),
                isCharging = leftCharge || rightCharge,
                isTws = true,
                leftLevel = left,
                isLeftCharging = leftCharge,
                rightLevel = right,
                isRightCharging = rightCharge,
                caseLevel = case,
                isCaseCharging = caseCharge
            )
        }

        // Single battery payload (Over-Ear)
        // Wire layout: [0x23/0x24, 0x00, level, is_charging]
        val level = payload[2].toInt() and 0xFF
        val isCharging = if (payload.size >= 4) (payload[3].toInt() != 0) else false
        return BatteryState(
            level = level,
            isCharging = isCharging,
            isTws = false
        )
    }

    /**
     * Parses Equalizer status packets (0x57 RET or 0x59 NOTIFY).
     * Wire layout: [0x57, 0x00, preset_id, band_count, clear_bass, b0, b1, b2, b3, b4]
     */
    fun parseEqualizerState(payload: ByteArray): EqualizerState? {
        if (payload.size < 4) return null
        val opcode = payload[0]
        if (opcode != MdrConstants.OP_EQ_RET && opcode != MdrConstants.OP_EQ_NOTIFY) {
            return null
        }

        val presetId = payload[2].toInt() and 0xFF
        val preset = EqPreset.fromId(presetId)

        if (payload.size >= 10) {
            val clearBass = (payload[4].toInt() and 0xFF) - 10
            val b0 = (payload[5].toInt() and 0xFF) - 10
            val b1 = (payload[6].toInt() and 0xFF) - 10
            val b2 = (payload[7].toInt() and 0xFF) - 10
            val b3 = (payload[8].toInt() and 0xFF) - 10
            val b4 = (payload[9].toInt() and 0xFF) - 10

            return EqualizerState(
                preset = preset,
                clearBass = clearBass,
                band400Hz = b0,
                band1kHz = b1,
                band2_5kHz = b2,
                band6_3kHz = b3,
                band16kHz = b4
            )
        }

        return EqualizerState(preset = preset)
    }

    /**
     * Parses Speak-to-Chat status packets (0xF7 RET or 0xF9 NOTIFY).
     * Wire layout: [0xF7/0xF9, subtype, onByte (0x00=Enabled, 0x01=Disabled), sensitivity, timeout]
     */
    fun parseSpeakToChatState(payload: ByteArray): SpeakToChatState? {
        if (payload.size < 3) return null
        val opcode = payload[0]
        if (opcode != MdrConstants.OP_STC_RET && opcode != MdrConstants.OP_STC_NOTIFY) {
            return null
        }
        val onByte = payload[2].toInt() and 0xFF
        val isEnabled = (onByte == 0x00)
        val sensitivity = if (payload.size > 3) payload[3].toInt() and 0xFF else 1
        val timeout = if (payload.size > 4) payload[4].toInt() and 0xFF else 1

        return SpeakToChatState(
            enabled = isEnabled,
            sensitivity = sensitivity,
            timeout = timeout
        )
    }

    /**
     * Parses DSEE status packets (0xE7 RET or 0xE9 NOTIFY).
     */
    fun parseDseeState(payload: ByteArray): Boolean? {
        if (payload.size < 3) return null
        val opcode = payload[0]
        if (opcode != MdrConstants.OP_DSEE_RET && opcode != MdrConstants.OP_DSEE_NOTIFY) {
            return null
        }
        return payload[2].toInt() != 0
    }
}
