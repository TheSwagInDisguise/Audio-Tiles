package com.sony.xm6controller.data.protocol

import com.sony.xm6controller.domain.EqPreset
import com.sony.xm6controller.domain.NoiseControlMode
import com.sony.xm6controller.domain.NoiseControlState

/**
 * Universal Sony MDR Command Payload Builder.
 *
 * Constructs byte payloads for Sony MDR v1 and v2 commands:
 * - Handshake/Initialization: [0x00, 0x00]
 * - ASC Query (GET): [0x66, subtype]
 * - ASC Modification (SET): V2 [0x68, 0x17, 0x01, on, amb, wind, voice, level]
 *                           V1 [0x68, 0x07, 0x01, on, amb, voice, level]
 * - Equalizer (5 Bands + Clear Bass + Presets): Opcode 0x58
 * - Speak-to-Chat: Opcode 0xF8
 * - DSEE Extreme: Opcode 0xE8
 * - Wearing Pause & Auto Power-Off: Opcode 0xD8
 * - Touch Sensor: Opcode 0xC8
 */
object MdrCommandBuilder {

    /**
     * Handshake initialization request payload.
     * Wire layout: [0x00, 0x00]
     */
    fun buildHandshakePayload(): ByteArray {
        return byteArrayOf(
            MdrConstants.OP_INIT_REQ, // 0x00
            0x00.toByte()             // 0x00
        )
    }

    /**
     * Query current Ambient Sound Control status.
     * @param subtype 0x17 for MDR v2 (XM5/XM6/LinkBuds/ULT), 0x07 for legacy v1 (XM4/XM3)
     */
    fun buildGetNoiseControlPayload(subtype: Byte = MdrConstants.SUBTYPE_NC_ASM_V2): ByteArray {
        return byteArrayOf(
            MdrConstants.OP_ASC_GET, // 0x66
            subtype
        )
    }

    /**
     * Constructs a SET command payload to switch noise control mode.
     * Automatically formats for either MDR v2 (with wind byte) or MDR v1 legacy (without wind byte).
     */
    fun buildSetNoiseControlPayload(
        targetMode: NoiseControlMode,
        currentState: NoiseControlState? = null,
        ambientLevel: Int = MdrConstants.AMBIENT_LEVEL_DEFAULT,
        focusOnVoice: Boolean = false
    ): ByteArray {
        val raw = currentState?.rawPayload
        val subtype = currentState?.subtype ?: MdrConstants.SUBTYPE_NC_ASM_V2
        val isV1 = (subtype == MdrConstants.SUBTYPE_NC_ASM_V1 || subtype == 0x01.toByte())
        val hasWind = currentState?.hasWindByte ?: (!isV1)
        val clampedLevel = ambientLevel.coerceIn(MdrConstants.AMBIENT_LEVEL_MIN, MdrConstants.AMBIENT_LEVEL_MAX).toByte()

        if (raw != null && raw.size >= 7) {
            val pl = raw.clone()
            pl[0] = MdrConstants.OP_ASC_SET // 0x68
            pl[2] = 0x01.toByte()

            when (targetMode) {
                NoiseControlMode.OFF -> {
                    pl[3] = MdrConstants.ASC_SWITCH_OFF // on = 0x00
                    pl[4] = MdrConstants.ASM_MODE_NC     // amb = 0x00
                }
                NoiseControlMode.NOISE_CANCELLING -> {
                    pl[3] = MdrConstants.ASC_SWITCH_ON  // on = 0x01
                    pl[4] = MdrConstants.ASM_MODE_NC     // amb = 0x00
                }
                NoiseControlMode.AMBIENT_SOUND -> {
                    pl[3] = MdrConstants.ASC_SWITCH_ON  // on = 0x01
                    pl[4] = MdrConstants.ASM_MODE_AMBIENT// amb = 0x01
                }
            }

            val windByte = if (currentState.windNoiseReduction && targetMode == NoiseControlMode.NOISE_CANCELLING) {
                MdrConstants.WIND_NOISE_REDUCTION_ON
            } else {
                MdrConstants.WIND_NOISE_REDUCTION_OFF
            }

            if (hasWind && pl.size > 5) {
                pl[5] = windByte
            }

            val voiceIdx = if (hasWind) 6 else 5
            val levelIdx = voiceIdx + 1

            if (pl.size > voiceIdx) {
                pl[voiceIdx] = if (focusOnVoice) MdrConstants.VOICE_FOCUS_ON else MdrConstants.VOICE_FOCUS_OFF
            }
            if (pl.size > levelIdx) {
                pl[levelIdx] = clampedLevel
            }
            return pl
        }

        // Fresh frame generation
        val onByte: Byte
        val ambByte: Byte

        when (targetMode) {
            NoiseControlMode.OFF -> {
                onByte = MdrConstants.ASC_SWITCH_OFF
                ambByte = MdrConstants.ASM_MODE_NC
            }
            NoiseControlMode.NOISE_CANCELLING -> {
                onByte = MdrConstants.ASC_SWITCH_ON
                ambByte = MdrConstants.ASM_MODE_NC
            }
            NoiseControlMode.AMBIENT_SOUND -> {
                onByte = MdrConstants.ASC_SWITCH_ON
                ambByte = MdrConstants.ASM_MODE_AMBIENT
            }
        }

        // V1 legacy 7-byte frame (XM3, XM4 legacy)
        if (isV1) {
            return byteArrayOf(
                MdrConstants.OP_ASC_SET,
                subtype,
                0x01.toByte(),
                onByte,
                ambByte,
                if (focusOnVoice) 0x01.toByte() else 0x00.toByte(),
                clampedLevel
            )
        }

        // V2 standard 8-byte frame (XM5, XM6, LinkBuds, ULT WEAR)
        val windByte = if (currentState?.windNoiseReduction == true && targetMode == NoiseControlMode.NOISE_CANCELLING) {
            MdrConstants.WIND_NOISE_REDUCTION_ON
        } else {
            MdrConstants.WIND_NOISE_REDUCTION_OFF
        }

        return byteArrayOf(
            MdrConstants.OP_ASC_SET,
            subtype,
            0x01.toByte(),
            onByte,
            ambByte,
            windByte,
            if (focusOnVoice) 0x01.toByte() else 0x00.toByte(),
            clampedLevel
        )
    }

    /**
     * Builds complete framed packet to query headphone battery level.
     */
    fun buildGetBatteryFrame(seq: Int): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_BATTERY_GET, 0x00)
        return MdrFrameEncoder.encode(
            type = MdrConstants.TYPE_COMMAND_1,
            seq = seq,
            payload = payload
        )
    }

    /**
     * Builds complete framed packet for handshake.
     */
    fun buildHandshakeFrame(seq: Int): ByteArray {
        return MdrFrameEncoder.encode(
            type = MdrConstants.TYPE_COMMAND_1,
            seq = seq,
            payload = buildHandshakePayload()
        )
    }

    /**
     * Builds complete framed packet for ASC query.
     */
    fun buildGetNoiseControlFrame(seq: Int, subtype: Byte = MdrConstants.SUBTYPE_NC_ASM_V2): ByteArray {
        return MdrFrameEncoder.encode(
            type = MdrConstants.TYPE_COMMAND_1,
            seq = seq,
            payload = buildGetNoiseControlPayload(subtype)
        )
    }

    /**
     * Builds complete framed packet for mode switch.
     */
    fun buildSetNoiseControlFrame(
        seq: Int,
        targetMode: NoiseControlMode,
        currentState: NoiseControlState? = null,
        ambientLevel: Int = MdrConstants.AMBIENT_LEVEL_DEFAULT,
        focusOnVoice: Boolean = false
    ): ByteArray {
        val payload = buildSetNoiseControlPayload(
            targetMode = targetMode,
            currentState = currentState,
            ambientLevel = ambientLevel,
            focusOnVoice = focusOnVoice
        )
        return MdrFrameEncoder.encode(
            type = MdrConstants.TYPE_COMMAND_1,
            seq = seq,
            payload = payload
        )
    }

    // --- Equalizer & Clear Bass Builders ---

    fun buildGetEqualizerFrame(seq: Int): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_EQ_GET, 0x00)
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    fun buildSetEqualizerPresetFrame(seq: Int, preset: EqPreset): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_EQ_SET, 0x00, preset.id.toByte(), 0x00)
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    fun buildSetEqualizerBandsFrame(
        seq: Int,
        preset: EqPreset = EqPreset.CUSTOM_1,
        clearBass: Int = 0,
        bands: List<Int> = listOf(0, 0, 0, 0, 0)
    ): ByteArray {
        // Values in MDR protocol are offset by +10: -10 dB -> 0, 0 dB -> 10, +10 dB -> 20
        val cbByte = (clearBass.coerceIn(-10, 10) + 10).toByte()
        val b0 = (bands.getOrElse(0) { 0 }.coerceIn(-10, 10) + 10).toByte()
        val b1 = (bands.getOrElse(1) { 0 }.coerceIn(-10, 10) + 10).toByte()
        val b2 = (bands.getOrElse(2) { 0 }.coerceIn(-10, 10) + 10).toByte()
        val b3 = (bands.getOrElse(3) { 0 }.coerceIn(-10, 10) + 10).toByte()
        val b4 = (bands.getOrElse(4) { 0 }.coerceIn(-10, 10) + 10).toByte()

        val payload = byteArrayOf(
            MdrConstants.OP_EQ_SET,
            0x00,
            preset.id.toByte(),
            0x06.toByte(), // 6 band values follow (Clear Bass + 5 EQ bands)
            cbByte,
            b0,
            b1,
            b2,
            b3,
            b4
        )
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    // --- Speak-to-Chat Builders ---

    fun buildGetSpeakToChatFrame(seq: Int): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_STC_GET, 0x0C)
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    fun buildSetSpeakToChatFrame(seq: Int, enabled: Boolean, sensitivity: Int = 1, timeout: Int = 1): ByteArray {
        val onByte = if (enabled) 0x00.toByte() else 0x01.toByte()
        val payload = byteArrayOf(
            MdrConstants.OP_STC_SET,
            0x0C,
            onByte,
            sensitivity.coerceIn(0, 2).toByte(),
            timeout.coerceIn(0, 2).toByte()
        )
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    // --- DSEE Extreme Builders ---

    fun buildGetDseeFrame(seq: Int): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_DSEE_GET, 0x01)
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    fun buildSetDseeFrame(seq: Int, enabled: Boolean): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_DSEE_SET, 0x01, if (enabled) 0x01.toByte() else 0x00.toByte())
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    // --- Wearing Detection & Auto Power-Off Builders ---

    fun buildSetWearingPauseFrame(seq: Int, enabled: Boolean): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_SYSTEM_SET, 0x01, if (enabled) 0x01.toByte() else 0x00.toByte())
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    fun buildSetAutoPowerOffFrame(seq: Int, timerIndex: Int): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_SYSTEM_SET, 0x02, timerIndex.coerceIn(0, 4).toByte())
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    // --- Touch Sensor Control Builder ---

    fun buildSetTouchSensorFrame(seq: Int, enabled: Boolean): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_TOUCH_SENSOR_SET, 0x01, if (enabled) 0x01.toByte() else 0x00.toByte())
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }

    // --- Sound Quality Mode (LDAC vs Stable Connection) ---

    fun buildSetSoundQualityPriorityFrame(seq: Int, priorityQuality: Boolean): ByteArray {
        val payload = byteArrayOf(MdrConstants.OP_ASC_SET, 0x05, 0x01, if (priorityQuality) 0x00.toByte() else 0x01.toByte())
        return MdrFrameEncoder.encode(type = MdrConstants.TYPE_COMMAND_1, seq = seq, payload = payload)
    }
}
