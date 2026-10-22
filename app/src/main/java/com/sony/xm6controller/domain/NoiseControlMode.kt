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
     * Cycles to the next mode based on the user-selected cycle scheme.
     */
    fun next(scheme: TileCycleScheme = TileCycleScheme.ALL_THREE): NoiseControlMode {
        return when (scheme) {
            TileCycleScheme.ALL_THREE -> when (this) {
                NOISE_CANCELLING -> AMBIENT_SOUND
                AMBIENT_SOUND -> OFF
                OFF -> NOISE_CANCELLING
            }
            TileCycleScheme.NC_AMBIENT -> when (this) {
                NOISE_CANCELLING -> AMBIENT_SOUND
                AMBIENT_SOUND -> NOISE_CANCELLING
                OFF -> NOISE_CANCELLING
            }
            TileCycleScheme.NC_OFF -> when (this) {
                NOISE_CANCELLING -> OFF
                OFF -> NOISE_CANCELLING
                AMBIENT_SOUND -> NOISE_CANCELLING
            }
        }
    }

    companion object {
        fun fromIndex(index: Int): NoiseControlMode {
            return entries.firstOrNull { it.modeIndex == index } ?: NOISE_CANCELLING
        }
    }
}

/**
 * Quick Settings Tile cycling preference.
 */
enum class TileCycleScheme(val label: String, val description: String) {
    ALL_THREE("NC → Ambient → Off", "Cycles through all 3 noise control modes"),
    NC_AMBIENT("NC ↔ Ambient Only", "Toggles directly between Noise Cancelling and Ambient"),
    NC_OFF("NC ↔ Off Only", "Toggles directly between Noise Cancelling and Off")
}

/**
 * Headphone battery telemetry state.
 */
data class BatteryState(
    val level: Int = -1, // -1 indicates unknown/loading, 0..100%
    val isCharging: Boolean = false
) {
    val isAvailable: Boolean get() = level >= 0
    val displayString: String get() = if (level >= 0) "$level%" else "Unknown"
}

/**
 * Comprehensive device noise control state snapshot.
 */
data class NoiseControlState(
    val mode: NoiseControlMode = NoiseControlMode.OFF,
    val ambientLevel: Int = 20,
    val focusOnVoice: Boolean = false,
    val windNoiseReduction: Boolean = false,
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
        if (windNoiseReduction != other.windNoiseReduction) return false
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
        result = 31 * result + windNoiseReduction.hashCode()
        result = 31 * result + hasWindByte.hashCode()
        result = 31 * result + subtype.toInt()
        result = 31 * result + (rawPayload?.contentHashCode() ?: 0)
        return result
    }
}
