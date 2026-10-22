package com.sony.xm6controller.data.protocol

import com.sony.xm6controller.domain.NoiseControlMode
import com.sony.xm6controller.domain.NoiseControlState

/**
 * Sony MDR Command Payload Builder.
 *
 * Constructs byte payloads for MDR commands:
 * - Handshake/Initialization: [0x00, 0x00]
 * - ASC Query (GET): [0x66, subtype]
 * - ASC Modification (SET): [0x68, subtype, 0x01, on, amb, wind, voice, level]
 */
object MdrCommandBuilder {

    /**
     * Handshake initialization request payload.
     * Required by Sony MDR v2 protocol immediately after RFCOMM socket connection.
     * Sony headphones ignore any subsequent command until this handshake succeeds.
     *
     * Wire layout: [0x00, 0x00]
     */
    fun buildHandshakePayload(): ByteArray {
        return byteArrayOf(
            MdrConstants.OP_INIT_REQ, // 0x00
            0x00.toByte()             // 0x00
        )
    }

    /**
     * Query current Ambient Sound Control (ANC/Ambient/Off) status.
     *
     * @param subtype 0x17 for standard MDR v2 (XM5/XM6), or 0x15 for alternate firmware
     * Wire layout: [0x66, subtype]
     */
    fun buildGetNoiseControlPayload(subtype: Byte = MdrConstants.SUBTYPE_NC_ASM_V2): ByteArray {
        return byteArrayOf(
            MdrConstants.OP_ASC_GET, // 0x66
            subtype                  // 0x17 or 0x15
        )
    }

    /**
     * Constructs a SET command payload to switch the headphone's noise control mode.
     *
     * If a cached state with raw payload from an earlier GET/NOTIFY is available,
     * this updates the active bytes while preserving any model-specific flags.
     * Otherwise, constructs the standard 8-byte Sony XM5/XM6 payload.
     *
     * @param targetMode Target mode: NOISE_CANCELLING, AMBIENT_SOUND, or OFF
     * @param currentState Optional last-known state for field preservation
     * @param ambientLevel Ambient sound volume (1..20, default 20)
     * @param focusOnVoice Enable voice enhancement in ambient mode
     *
     * Sony MDR v2 NC/ASM Payload Structure (8 bytes):
     * [0] 0x68  (P_ASC_SET command opcode)
     * [1] 0x17  (Inquired Subtype: NC_ASM_V2)
     * [2] 0x01  (Configuration type indicator)
     * [3] <on>  (0x01 = NC/Ambient active, 0x00 = Off/Normal)
     * [4] <amb> (0x00 = Noise Cancelling, 0x01 = Ambient Sound)
     * [5] 0x02  (Wind noise reduction: 0x02 = normal/off, 0x03 = wind reduction)
     * [6] <voi> (0x00 = voice focus off, 0x01 = voice focus on)
     * [7] <lvl> (Ambient volume level: 1 to 20, 0x01..0x14)
     */
    fun buildSetNoiseControlPayload(
        targetMode: NoiseControlMode,
        currentState: NoiseControlState? = null,
        ambientLevel: Int = MdrConstants.AMBIENT_LEVEL_DEFAULT,
        focusOnVoice: Boolean = false
    ): ByteArray {
        val raw = currentState?.rawPayload
        val hasWind = currentState?.hasWindByte ?: true
        val subtype = currentState?.subtype ?: MdrConstants.SUBTYPE_NC_ASM_V2
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

        // Fresh frame generation for WH-1000XM6 (Standard 8-byte layout)
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

        val windByte = if (currentState?.windNoiseReduction == true && targetMode == NoiseControlMode.NOISE_CANCELLING) {
            MdrConstants.WIND_NOISE_REDUCTION_ON
        } else {
            MdrConstants.WIND_NOISE_REDUCTION_OFF
        }

        return byteArrayOf(
            MdrConstants.OP_ASC_SET,                        // 0x68
            subtype,                                        // 0x17
            0x01.toByte(),                                  // Config selector
            onByte,                                         // On/Off (0x01 or 0x00)
            ambByte,                                        // Mode (0x00 NC, 0x01 Ambient)
            windByte,                                       // Wind noise reduction byte
            if (focusOnVoice) 0x01.toByte() else 0x00.toByte(), // Voice focus
            clampedLevel                                    // Ambient level (1..20)
        )
    }

    /**
     * Builds complete framed and escaped packet to query headphone battery level.
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
     * Builds complete framed and escaped packet for handshake.
     */
    fun buildHandshakeFrame(seq: Int): ByteArray {
        return MdrFrameEncoder.encode(
            type = MdrConstants.TYPE_COMMAND_1,
            seq = seq,
            payload = buildHandshakePayload()
        )
    }

    /**
     * Builds complete framed and escaped packet for ASC query.
     */
    fun buildGetNoiseControlFrame(seq: Int, subtype: Byte = MdrConstants.SUBTYPE_NC_ASM_V2): ByteArray {
        return MdrFrameEncoder.encode(
            type = MdrConstants.TYPE_COMMAND_1,
            seq = seq,
            payload = buildGetNoiseControlPayload(subtype)
        )
    }

    /**
     * Builds complete framed and escaped packet for mode switch.
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
}
