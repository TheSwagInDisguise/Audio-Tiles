package me.audiotiles.domain

/**
 * Noise control modes supported across Sony Bluetooth headphones and earbuds.
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
 * Device form factor classification.
 */
enum class DeviceType(val displayName: String) {
    OVER_EAR("Over-Ear Headphones"),
    TRUE_WIRELESS("True Wireless Earbuds"),
    IN_EAR("In-Ear / Neckband");

    companion object {
        fun fromName(name: String?): DeviceType {
            if (name == null) return OVER_EAR
            val upper = name.uppercase()
            return when {
                upper.startsWith("WF-") || upper.contains("LINKBUDS") || upper.contains("BUDS") -> TRUE_WIRELESS
                upper.startsWith("WI-") -> IN_EAR
                else -> OVER_EAR
            }
        }
    }
}

/**
 * Universal battery telemetry state supporting both Over-Ear and TWS Earbuds.
 */
data class BatteryState(
    val level: Int = -1,                 // Primary / Over-Ear level (0..100%)
    val isCharging: Boolean = false,
    val isTws: Boolean = false,          // True if TWS multi-cell reported (Left, Right, Case)
    val leftLevel: Int = -1,             // Left earbud %
    val isLeftCharging: Boolean = false,
    val rightLevel: Int = -1,            // Right earbud %
    val isRightCharging: Boolean = false,
    val caseLevel: Int = -1,             // Charging case %
    val isCaseCharging: Boolean = false
) {
    val isAvailable: Boolean
        get() = level >= 0 || leftLevel >= 0 || rightLevel >= 0

    // Convenience aliases for TWS telemetry
    val levelLeft: Int get() = leftLevel
    val levelRight: Int get() = rightLevel
    val levelCase: Int get() = caseLevel

    val displayString: String
        get() {
            if (isTws && (leftLevel >= 0 || rightLevel >= 0)) {
                val parts = mutableListOf<String>()
                if (leftLevel >= 0) parts.add("L: $leftLevel%${if (isLeftCharging) "⚡" else ""}")
                if (rightLevel >= 0) parts.add("R: $rightLevel%${if (isRightCharging) "⚡" else ""}")
                if (caseLevel >= 0) parts.add("Case: $caseLevel%${if (isCaseCharging) "⚡" else ""}")
                return parts.joinToString(" • ")
            }
            return if (level >= 0) "$level%${if (isCharging) " ⚡" else ""}" else "Unknown"
        }
}

/**
 * Hardware Equalizer Presets.
 */
enum class EqPreset(val id: Int, val displayName: String) {
    OFF(0x00, "Off"),
    BRIGHT(0x01, "Bright"),
    EXCITED(0x02, "Excited"),
    MELLOW(0x03, "Mellow"),
    RELAXED(0x04, "Relaxed"),
    VOCAL(0x05, "Vocal"),
    TREBLE_BOOST(0x06, "Treble Boost"),
    BASS_BOOST(0x07, "Bass Boost"),
    SPEECH(0x08, "Speech"),
    MANUAL(0x09, "Manual"),
    CUSTOM_1(0xA0, "Custom 1"),
    CUSTOM_2(0xA1, "Custom 2");

    companion object {
        fun fromId(id: Int): EqPreset {
            return entries.firstOrNull { it.id == id } ?: OFF
        }
    }
}

/**
 * 5-Band Graphic Equalizer and Clear Bass state.
 * Bands: 400Hz, 1kHz, 2.5kHz, 6.3kHz, 16kHz (range: -10 to +10 dB).
 * Clear Bass: -10 to +10.
 */
data class EqualizerState(
    val preset: EqPreset = EqPreset.OFF,
    val clearBass: Int = 0,
    val band400Hz: Int = 0,
    val band1kHz: Int = 0,
    val band2_5kHz: Int = 0,
    val band6_3kHz: Int = 0,
    val band16kHz: Int = 0
) {
    val bands: List<Int>
        get() = listOf(band400Hz, band1kHz, band2_5kHz, band6_3kHz, band16kHz)
}

/**
 * Speak-to-Chat settings state.
 */
data class SpeakToChatState(
    val enabled: Boolean = false,
    val sensitivity: Int = 1, // 0 = High, 1 = Standard, 2 = Low
    val timeout: Int = 1      // 0 = Short (5s), 1 = Standard (15s), 2 = Long (30s)
)

/**
 * Comprehensive Sound Connect hardware features state.
 */
data class SoundFeaturesState(
    val dseeExtreme: Boolean = true,
    val speakToChat: SpeakToChatState = SpeakToChatState(),
    val wearingPause: Boolean = true,
    val autoPowerOffIndex: Int = 1, // 0 = Do Not Turn Off, 1 = 5m, 2 = 30m, 3 = 60m, 4 = 180m
    val touchSensorEnabled: Boolean = true,
    val soundQualityPriority: Boolean = true // true = Priority on Sound Quality (LDAC), false = Stable Connection
)

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
