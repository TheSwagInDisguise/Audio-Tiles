package com.sony.xm6controller.domain

/**
 * Noise control modes supported by Sony WH-1000XM6.
 *
 * Strict cycling order:
 * NOISE_CANCELLING -> AMBIENT_SOUND -> OFF -> NOISE_CANCELLING
 */
enum class NoiseControlMode(
    val modeIndex: Int,
    val label: String,
    val shortLabel: String,
    val tileSubtitle: String
) {
    NOISE_CANCELLING(
        modeIndex = 0,
        label = "Noise Cancelling",
        shortLabel = "NC On",
        tileSubtitle = "Active Noise Cancelling"
    ),
    AMBIENT_SOUND(
        modeIndex = 1,
        label = "Ambient Sound",
        shortLabel = "Ambient",
        tileSubtitle = "Transparency Mode"
    ),
    OFF(
        modeIndex = 2,
        label = "Off",
        shortLabel = "ANC Off",
        tileSubtitle = "Noise Control Off"
    );

    /**
     * Strict cycling order as required:
     * Noise Cancelling -> Ambient Sound -> Off -> Noise Cancelling
     */
    fun next(): NoiseControlMode {
        return when (this) {
            NOISE_CANCELLING -> AMBIENT_SOUND
            AMBIENT_SOUND -> OFF
            OFF -> NOISE_CANCELLING
        }
    }

    companion object {
        fun fromIndex(index: Int): NoiseControlMode {
            return entries.firstOrNull { it.modeIndex == index } ?: NOISE_CANCELLING
        }
    }
}

/**
 * Comprehensive device noise control state snapshot.
 */
data class NoiseControlState(
    val mode: NoiseControlMode = NoiseControlMode.OFF,
    val ambientLevel: Int = 20,
    val focusOnVoice: Boolean = false,
    val hasWindByte: Boolean = true,
    val subtype: Byte = 0x17.toByte(),
    val rawPayload: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoiseControlState
        if (mode != other.mode) return false
        if (ambientLevel != other.ambientLevel) return false
        if (focusOnVoice != other.focusOnVoice) return false
        if (hasWindByte != other.hasWindByte) return false
        if (subtype != other.subtype) return false
        if (rawPayload != null) {
            if (other.rawPayload == null) return false
            if (!rawPayload.contentEquals(other.rawPayload)) return false
        } else if (other.rawPayload != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mode.hashCode()
        result = 31 * result + ambientLevel
        result = 31 * result + focusOnVoice.hashCode()
        result = 31 * result + hasWindByte.hashCode()
        result = 31 * result + subtype.toInt()
        result = 31 * result + (rawPayload?.contentHashCode() ?: 0)
        return result
    }
}
