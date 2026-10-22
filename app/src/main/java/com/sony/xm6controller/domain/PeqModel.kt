package com.sony.xm6controller.domain

import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Standard 10-band ISO octave center frequencies.
 */
val TEN_BAND_FREQUENCIES = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
val TEN_BAND_LABELS = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

/**
 * 10-Band Graphic Equalizer State (-12 dB to +12 dB).
 */
data class TenBandEqState(
    val bands: List<Float> = List(10) { 0.0f }, // Gains at 31, 63, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
    val clearBass: Int = 0,
    val profileName: String = "Manual",
    val preampDb: Float = 0.0f
) {
    fun withBand(index: Int, gainDb: Float): TenBandEqState {
        val next = bands.toMutableList()
        if (index in next.indices) {
            next[index] = gainDb.coerceIn(-12.0f, 12.0f)
        }
        return copy(bands = next, profileName = "Custom 10-Band")
    }

    fun withClearBass(level: Int): TenBandEqState {
        return copy(clearBass = level.coerceIn(-10, 10))
    }

    /**
     * Translates 10-band ISO gains into Sony hardware DSP parameters (Clear Bass + 5 Bands).
     * Sony Hardware:
     * - Clear Bass: -10 to +10 dB (affects < 100 Hz sub-bass)
     * - Band 1: 400 Hz (-10 to +10 dB)
     * - Band 2: 1.0 kHz (-10 to +10 dB)
     * - Band 3: 2.5 kHz (-10 to +10 dB)
     * - Band 4: 6.3 kHz (-10 to +10 dB)
     * - Band 5: 16.0 kHz (-10 to +10 dB)
     */
    fun toSonyHardwareEq(): SonyHardwareEqMapping {
        // Clear Bass: use explicit clearBass if set, else derive from 31Hz and 63Hz energy
        val effectiveClearBass = if (clearBass != 0) {
            clearBass.coerceIn(-10, 10)
        } else {
            val subBassAvg = (bands[0] * 0.6f + bands[1] * 0.4f)
            subBassAvg.coerceIn(-10f, 10f).toInt()
        }

        // 400 Hz band: interpolated from 250Hz, 500Hz
        val b400 = (bands[3] * 0.4f + bands[4] * 0.6f).coerceIn(-10f, 10f).toInt()

        // 1.0 kHz band: direct match with band 5 (1000 Hz)
        val b1k = bands[5].coerceIn(-10f, 10f).toInt()

        // 2.5 kHz band: interpolated from 2.0 kHz (band 6) and 4.0 kHz (band 7)
        val b2_5k = (bands[6] * 0.65f + bands[7] * 0.35f).coerceIn(-10f, 10f).toInt()

        // 6.3 kHz band: interpolated between 4.0 kHz (band 7) and 8.0 kHz (band 8)
        val b6_3k = (bands[7] * 0.3f + bands[8] * 0.7f).coerceIn(-10f, 10f).toInt()

        // 16.0 kHz band: direct match with band 9 (16000 Hz)
        val b16k = bands[9].coerceIn(-10f, 10f).toInt()

        return SonyHardwareEqMapping(
            clearBass = effectiveClearBass,
            band400Hz = b400,
            band1kHz = b1k,
            band2_5kHz = b2_5k,
            band6_3kHz = b6_3k,
            band16kHz = b16k
        )
    }
}

data class SonyHardwareEqMapping(
    val clearBass: Int,
    val band400Hz: Int,
    val band1kHz: Int,
    val band2_5kHz: Int,
    val band6_3kHz: Int,
    val band16kHz: Int
) {
    val bands: List<Int> get() = listOf(band400Hz, band1kHz, band2_5kHz, band6_3kHz, band16kHz)
}

/**
 * Filter types for Parametric Equalizers.
 */
enum class PeqFilterType {
    PEAK,       // PK / Peaking / Bell
    LOW_SHELF,  // LSC / Low Shelf
    HIGH_SHELF  // HSC / High Shelf
}

/**
 * Single parametric filter definition.
 */
data class PeqFilter(
    val type: PeqFilterType,
    val frequency: Float,
    val gainDb: Float,
    val qFactor: Float = 1.41f
) {
    /**
     * Calculates the approximate gain in dB of this biquad filter at arbitrary frequency f.
     */
    fun evaluateGainDb(f: Float): Float {
        if (f <= 0f) return 0f
        val ratio = f / frequency
        return when (type) {
            PeqFilterType.PEAK -> {
                // Bell curve response model
                val octDiff = kotlin.math.abs(log10(ratio) / log10(2.0)).toFloat()
                val bandwidth = 1.0f / qFactor.coerceAtLeast(0.1f)
                val factor = kotlin.math.exp(-0.5 * (octDiff / (bandwidth * 0.5f)).pow(2)).toFloat()
                gainDb * factor
            }
            PeqFilterType.LOW_SHELF -> {
                if (f <= frequency) {
                    gainDb
                } else {
                    val octDiff = (log10(ratio) / log10(2.0)).toFloat()
                    val falloff = kotlin.math.max(0.0f, 1.0f - octDiff / (1.5f / qFactor.coerceAtLeast(0.5f)))
                    gainDb * falloff
                }
            }
            PeqFilterType.HIGH_SHELF -> {
                if (f >= frequency) {
                    gainDb
                } else {
                    val octDiff = (log10(frequency / f) / log10(2.0)).toFloat()
                    val falloff = kotlin.math.max(0.0f, 1.0f - octDiff / (1.5f / qFactor.coerceAtLeast(0.5f)))
                    gainDb * falloff
                }
            }
        }
    }
}

/**
 * Complete Parametric EQ profile (from peqdb, AutoEq, or Peace/EqualizerAPO).
 */
data class PeqProfile(
    val name: String,
    val preampDb: Float = 0.0f,
    val filters: List<PeqFilter>
) {
    /**
     * Computes the combined response gain in dB at frequency f.
     */
    fun totalGainAt(f: Float): Float {
        var gain = preampDb
        for (filter in filters) {
            gain += filter.evaluateGainDb(f)
        }
        return gain
    }

    /**
     * Samples this continuous parametric profile at the 10 ISO center frequencies.
     */
    fun sampleToTenBand(): TenBandEqState {
        val gains = TEN_BAND_FREQUENCIES.map { f ->
            totalGainAt(f.toFloat()).coerceIn(-12.0f, 12.0f)
        }
        return TenBandEqState(
            bands = gains,
            profileName = name,
            preampDb = preampDb
        )
    }
}

/**
 * Universal Parser for PEQDB, AutoEq, and EqualizerAPO files.
 */
object PeqParser {

    /**
     * Auto-detects and parses JSON (.peqdb, .json) or Parametric text (.txt).
     */
    fun parse(content: String, fallbackName: String = "Imported PEQ"): PeqProfile? {
        val trimmed = content.trim()
        if (trimmed.startsWith("{")) {
            val jsonResult = parseJson(trimmed, fallbackName)
            if (jsonResult != null) return jsonResult
        }
        return parseParametricText(trimmed, fallbackName)
    }

    /**
     * Parses standard PEQDB JSON format:
     * {
     *   "name": "Sony WH-1000XM6 Harman",
     *   "preamp": -3.5,
     *   "filters": [
     *     {"type": "PK", "freq": 105, "gain": -3.2, "q": 1.41}
     *   ]
     * }
     */
    fun parseJson(jsonString: String, defaultName: String): PeqProfile? {
        try {
            val obj = JSONObject(jsonString)
            val name = obj.optString("name", defaultName)
            val preamp = obj.optDouble("preamp", 0.0).toFloat()
            val filtersList = mutableListOf<PeqFilter>()

            val filterArray = obj.optJSONArray("filters")
            if (filterArray != null) {
                for (i in 0 until filterArray.length()) {
                    val fObj = filterArray.getJSONObject(i)
                    val typeStr = fObj.optString("type", "PK").uppercase()
                    val freq = fObj.optDouble("freq", 1000.0).toFloat()
                    val gain = fObj.optDouble("gain", 0.0).toFloat()
                    val q = fObj.optDouble("q", 1.41).toFloat()

                    val type = when {
                        typeStr.contains("LSC") || typeStr.contains("LOW_SHELF") || typeStr == "LS" -> PeqFilterType.LOW_SHELF
                        typeStr.contains("HSC") || typeStr.contains("HIGH_SHELF") || typeStr == "HS" -> PeqFilterType.HIGH_SHELF
                        else -> PeqFilterType.PEAK
                    }
                    filtersList.add(PeqFilter(type, freq, gain, q))
                }
            }

            if (filtersList.isNotEmpty()) {
                return PeqProfile(name, preamp, filtersList)
            }
        } catch (_: Throwable) {
            // Fallback for environments where android.jar JSONObject is a mock/stub
        }

        return parseJsonFallback(jsonString, defaultName)
    }

    private fun parseJsonFallback(jsonString: String, defaultName: String): PeqProfile? {
        val nameMatch = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(jsonString)
        val name = nameMatch?.groupValues?.get(1) ?: defaultName

        val preampMatch = Regex("\"preamp\"\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+)").find(jsonString)
        val preamp = preampMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.0f

        val filters = mutableListOf<PeqFilter>()
        val blockPattern = Regex("\\{([^{}]+)\\}")
        for (m in blockPattern.findAll(jsonString)) {
            val block = m.groupValues[1]
            if (block.contains("freq") && block.contains("gain")) {
                val tMatch = Regex("\"type\"\\s*:\\s*\"([^\"]+)\"").find(block)
                val fMatch = Regex("\"freq\"\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+)").find(block)
                val gMatch = Regex("\"gain\"\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+)").find(block)
                val qMatch = Regex("\"q\"\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+)").find(block)

                if (fMatch != null && gMatch != null) {
                    val typeStr = (tMatch?.groupValues?.get(1) ?: "PK").uppercase()
                    val freq = fMatch.groupValues[1].toFloatOrNull() ?: 1000f
                    val gain = gMatch.groupValues[1].toFloatOrNull() ?: 0f
                    val q = qMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 1.41f

                    val type = when {
                        typeStr.contains("LSC") || typeStr.contains("LOW_SHELF") || typeStr == "LS" -> PeqFilterType.LOW_SHELF
                        typeStr.contains("HSC") || typeStr.contains("HIGH_SHELF") || typeStr == "HS" -> PeqFilterType.HIGH_SHELF
                        else -> PeqFilterType.PEAK
                    }
                    filters.add(PeqFilter(type, freq, gain, q))
                }
            }
        }

        return if (filters.isNotEmpty()) PeqProfile(name, preamp, filters) else null
    }

    /**
     * Parses AutoEq / EqualizerAPO / Squiglink parametric text format:
     * Preamp: -4.5 dB
     * Filter 1: ON PK Fc 28 Hz Gain 4.8 dB Q 1.41
     * Filter 2: ON LSC Fc 105 Hz Gain -3.2 dB Q 0.71
     */
    fun parseParametricText(text: String, defaultName: String): PeqProfile? {
        val filters = mutableListOf<PeqFilter>()
        var preamp = 0.0f
        var detectedName = defaultName

        val lines = text.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            if (line.startsWith("Preamp:", ignoreCase = true)) {
                val parts = line.split(":")
                if (parts.size >= 2) {
                    val pStr = parts[1].replace("dB", "").trim()
                    preamp = pStr.toFloatOrNull() ?: 0.0f
                }
                continue
            }

            if (line.startsWith("Name:", ignoreCase = true)) {
                detectedName = line.substringAfter(":").trim()
                continue
            }

            // Match parametric filter line: Filter X: ON [TYPE] Fc [FREQ] Hz Gain [GAIN] dB Q [Q]
            if (line.contains("Fc", ignoreCase = true) && line.contains("Gain", ignoreCase = true)) {
                try {
                    val upper = line.uppercase()
                    val type = when {
                        upper.contains("LSC") || upper.contains("LOWSHELF") || upper.contains("LOW SHELF") -> PeqFilterType.LOW_SHELF
                        upper.contains("HSC") || upper.contains("HIGHSHELF") || upper.contains("HIGH SHELF") -> PeqFilterType.HIGH_SHELF
                        else -> PeqFilterType.PEAK
                    }

                    // Extract Fc
                    val fcRegex = Regex("""FC\s+([\d.]+)\s*HZ""", RegexOption.IGNORE_CASE)
                    val freq = fcRegex.find(line)?.groupValues?.get(1)?.toFloatOrNull() ?: continue

                    // Extract Gain
                    val gainRegex = Regex("""GAIN\s+([+-]?[\d.]+)\s*DB""", RegexOption.IGNORE_CASE)
                    val gain = gainRegex.find(line)?.groupValues?.get(1)?.toFloatOrNull() ?: continue

                    // Extract Q
                    val qRegex = Regex("""Q\s+([\d.]+)""", RegexOption.IGNORE_CASE)
                    val q = qRegex.find(line)?.groupValues?.get(1)?.toFloatOrNull() ?: 1.41f

                    filters.add(PeqFilter(type, freq, gain, q))
                } catch (_: Exception) {}
            }
        }

        return if (filters.isEmpty()) null else PeqProfile(detectedName, preamp, filters)
    }

    /**
     * Built-in curated audiophile targets.
     */
    val TARGET_PROFILES: List<PeqProfile> = listOf(
        PeqProfile(
            name = "Harman Target 2020 (Over-Ear)",
            preampDb = -3.5f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105f, 4.8f, 0.71f),
                PeqFilter(PeqFilterType.PEAK, 220f, -2.4f, 1.2f),
                PeqFilter(PeqFilterType.PEAK, 1250f, 1.8f, 1.8f),
                PeqFilter(PeqFilterType.PEAK, 3150f, 3.2f, 2.0f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 8500f, 1.5f, 0.71f)
            )
        ),
        PeqProfile(
            name = "Harman Target 2019 (In-Ear / TWS)",
            preampDb = -4.0f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 80f, 5.5f, 0.71f),
                PeqFilter(PeqFilterType.PEAK, 350f, -2.0f, 1.4f),
                PeqFilter(PeqFilterType.PEAK, 2800f, 4.0f, 1.5f),
                PeqFilter(PeqFilterType.PEAK, 5500f, -1.8f, 2.2f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000f, 2.0f, 0.71f)
            )
        ),
        PeqProfile(
            name = "Oratory1990 Optimum Hi-Fi",
            preampDb = -3.8f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 90f, 4.2f, 0.71f),
                PeqFilter(PeqFilterType.PEAK, 240f, -1.8f, 1.1f),
                PeqFilter(PeqFilterType.PEAK, 1000f, 1.2f, 1.4f),
                PeqFilter(PeqFilterType.PEAK, 2700f, 2.8f, 1.7f),
                PeqFilter(PeqFilterType.PEAK, 6000f, -1.2f, 2.5f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 9000f, 1.2f, 0.71f)
            )
        ),
        PeqProfile(
            name = "Basshead 808 (Sub-Bass Slam)",
            preampDb = -6.0f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 65f, 7.5f, 0.71f),
                PeqFilter(PeqFilterType.PEAK, 120f, 3.5f, 1.2f),
                PeqFilter(PeqFilterType.PEAK, 300f, -2.5f, 1.4f),
                PeqFilter(PeqFilterType.PEAK, 3500f, 1.5f, 1.5f)
            )
        ),
        PeqProfile(
            name = "Vocal Clarity & Acoustic",
            preampDb = -2.0f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 100f, -1.5f, 0.71f),
                PeqFilter(PeqFilterType.PEAK, 1200f, 2.8f, 1.5f),
                PeqFilter(PeqFilterType.PEAK, 3200f, 3.5f, 1.8f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 7500f, 2.2f, 0.71f)
            )
        ),
        PeqProfile(
            name = "Studio Flat (0 dB Reference)",
            preampDb = 0.0f,
            filters = emptyList()
        )
    )
}

/**
 * Built-in AutoEq Catalog entry representing indexed measurement compensations.
 */
data class AutoEqCatalogEntry(
    val id: String,
    val modelName: String,
    val targetName: String,
    val labSource: String,
    val description: String,
    val profile: PeqProfile
)

/**
 * Indexed catalog of AutoEq measurement profiles for top Sony headphones and earbuds.
 * Sourced directly from official AutoEq / PEQDB repositories (oratory1990, Rtings B&K 5128, Kuulokenurkka, DHRME).
 */
object AutoEqCatalog {
    val entries: List<AutoEqCatalogEntry> = AUTOEQ_OFFICIAL_ENTRIES
}

/**
 * User-created custom Equalizer preset stored in local device storage.
 */
data class UserEqPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val tenBandGains: List<Float>,
    val clearBass: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toTenBandEqState(): TenBandEqState {
        val bands = tenBandGains.take(10).let { list ->
            if (list.size == 10) list else list + List(10 - list.size) { 0f }
        }
        return TenBandEqState(bands = bands, clearBass = clearBass, profileName = name)
    }

    companion object {
        val DEFAULT_PRESETS = listOf(
            UserEqPreset(
                id = "default_neutral",
                name = "Studio Reference Flat",
                tenBandGains = List(10) { 0.0f },
                clearBass = 0
            ),
            UserEqPreset(
                id = "default_harman",
                name = "Harman Balanced",
                tenBandGains = listOf(6.0f, 5.0f, 3.0f, 0.0f, -1.0f, 0.5f, 3.5f, 7.0f, 2.0f, -1.0f),
                clearBass = 5
            ),
            UserEqPreset(
                id = "default_basshead",
                name = "Basshead 808 Slam",
                tenBandGains = listOf(10.0f, 9.0f, 7.0f, 3.5f, 1.0f, -1.0f, 0.0f, 1.5f, 2.0f, 3.0f),
                clearBass = 9
            ),
            UserEqPreset(
                id = "default_vocal",
                name = "Vocal Clarity & Podcasts",
                tenBandGains = listOf(-1.0f, -1.0f, 0.0f, 1.5f, 2.5f, 3.5f, 4.0f, 2.0f, 1.0f, 0.0f),
                clearBass = 0
            ),
            UserEqPreset(
                id = "default_cinema",
                name = "Cinematic Immersion",
                tenBandGains = listOf(7.0f, 6.0f, 3.0f, -0.5f, -1.0f, 1.0f, 3.0f, 5.0f, 4.0f, 2.0f),
                clearBass = 6
            )
        )
    }
}
