package com.sony.xm6controller.domain

enum class DeviceFamily(val displayName: String) {
    OVER_EAR_FLAGSHIP("Flagship Over-Ear"),
    TWS_FLAGSHIP("Flagship True Wireless"),
    TWS_LINKBUDS("LinkBuds Series"),
    BASS_SERIES("ULT WEAR & Extra Bass"),
    OVER_EAR_MIDRANGE("Mid-Range Over-Ear"),
    TWS_MIDRANGE("Mid-Range True Wireless"),
    NECKBAND("Neckband Series"),
    GENERIC_SONY("Sony Wireless Audio")
}

data class DeviceCapabilities(
    val modelName: String = "Sony Audio",
    val family: DeviceFamily = DeviceFamily.GENERIC_SONY,
    val hasSpeakToChat: Boolean = true,
    val hasDsee: Boolean = true,
    val hasWindNoiseReduction: Boolean = true,
    val hasClearBass: Boolean = true,
    val hasEqualizer: Boolean = true,
    val hasMultiBattery: Boolean = false,
    val hasWearingDetection: Boolean = true,
    val hasTouchSensorControl: Boolean = true
) {
    companion object {
        fun resolve(deviceName: String?): DeviceCapabilities {
            val name = (deviceName ?: "").uppercase()
            return when {
                name.contains("WH-1000XM6") || name.contains("1000XM6") -> DeviceCapabilities(
                    modelName = "WH-1000XM6",
                    family = DeviceFamily.OVER_EAR_FLAGSHIP,
                    hasSpeakToChat = true,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = false,
                    hasWearingDetection = true,
                    hasTouchSensorControl = true
                )
                name.contains("WH-1000XM5") || name.contains("1000XM5") -> DeviceCapabilities(
                    modelName = "WH-1000XM5",
                    family = DeviceFamily.OVER_EAR_FLAGSHIP,
                    hasSpeakToChat = true,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = false,
                    hasWearingDetection = true,
                    hasTouchSensorControl = true
                )
                name.contains("WH-1000XM4") || name.contains("1000XM4") -> DeviceCapabilities(
                    modelName = "WH-1000XM4",
                    family = DeviceFamily.OVER_EAR_FLAGSHIP,
                    hasSpeakToChat = true,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = false,
                    hasWearingDetection = true,
                    hasTouchSensorControl = true
                )
                name.contains("WH-1000XM3") || name.contains("1000XM3") -> DeviceCapabilities(
                    modelName = "WH-1000XM3",
                    family = DeviceFamily.OVER_EAR_FLAGSHIP,
                    hasSpeakToChat = false,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = false,
                    hasWearingDetection = false,
                    hasTouchSensorControl = true
                )
                name.contains("WF-1000XM5") -> DeviceCapabilities(
                    modelName = "WF-1000XM5",
                    family = DeviceFamily.TWS_FLAGSHIP,
                    hasSpeakToChat = true,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = true,
                    hasWearingDetection = true,
                    hasTouchSensorControl = true
                )
                name.contains("WF-1000XM4") -> DeviceCapabilities(
                    modelName = "WF-1000XM4",
                    family = DeviceFamily.TWS_FLAGSHIP,
                    hasSpeakToChat = true,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = true,
                    hasWearingDetection = true,
                    hasTouchSensorControl = true
                )
                name.contains("WF-1000XM3") -> DeviceCapabilities(
                    modelName = "WF-1000XM3",
                    family = DeviceFamily.TWS_FLAGSHIP,
                    hasSpeakToChat = false,
                    hasDsee = true,
                    hasWindNoiseReduction = false,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = true,
                    hasWearingDetection = true,
                    hasTouchSensorControl = true
                )
                name.contains("LINKBUDS S") || name.contains("WF-LS900") -> DeviceCapabilities(
                    modelName = "LinkBuds S",
                    family = DeviceFamily.TWS_LINKBUDS,
                    hasSpeakToChat = true,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = true,
                    hasWearingDetection = true,
                    hasTouchSensorControl = true
                )
                name.contains("LINKBUDS") || name.contains("WF-L900") -> DeviceCapabilities(
                    modelName = "LinkBuds",
                    family = DeviceFamily.TWS_LINKBUDS,
                    hasSpeakToChat = true,
                    hasDsee = true,
                    hasWindNoiseReduction = false,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = true,
                    hasWearingDetection = true,
                    hasTouchSensorControl = true
                )
                name.contains("ULT900") || name.contains("ULT WEAR") -> DeviceCapabilities(
                    modelName = "WH-ULT900N",
                    family = DeviceFamily.BASS_SERIES,
                    hasSpeakToChat = false,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = false,
                    hasWearingDetection = true,
                    hasTouchSensorControl = true
                )
                name.contains("XB910") || name.contains("XB900") -> DeviceCapabilities(
                    modelName = "WH-XB910N",
                    family = DeviceFamily.BASS_SERIES,
                    hasSpeakToChat = false,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = false,
                    hasWearingDetection = false,
                    hasTouchSensorControl = true
                )
                name.contains("CH720") -> DeviceCapabilities(
                    modelName = "WH-CH720N",
                    family = DeviceFamily.OVER_EAR_MIDRANGE,
                    hasSpeakToChat = false,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = false,
                    hasWearingDetection = false,
                    hasTouchSensorControl = false
                )
                name.contains("CH520") -> DeviceCapabilities(
                    modelName = "WH-CH520",
                    family = DeviceFamily.OVER_EAR_MIDRANGE,
                    hasSpeakToChat = false,
                    hasDsee = true,
                    hasWindNoiseReduction = false,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = false,
                    hasWearingDetection = false,
                    hasTouchSensorControl = false
                )
                name.contains("C700") -> DeviceCapabilities(
                    modelName = "WF-C700N",
                    family = DeviceFamily.TWS_MIDRANGE,
                    hasSpeakToChat = false,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = true,
                    hasWearingDetection = false,
                    hasTouchSensorControl = false
                )
                name.contains("C510") || name.contains("C500") -> DeviceCapabilities(
                    modelName = "WF-C510",
                    family = DeviceFamily.TWS_MIDRANGE,
                    hasSpeakToChat = false,
                    hasDsee = true,
                    hasWindNoiseReduction = false,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = true,
                    hasWearingDetection = false,
                    hasTouchSensorControl = false
                )
                name.contains("WI-1000") || name.contains("WI-C") -> DeviceCapabilities(
                    modelName = "Sony Neckband",
                    family = DeviceFamily.NECKBAND,
                    hasSpeakToChat = false,
                    hasDsee = true,
                    hasWindNoiseReduction = true,
                    hasClearBass = true,
                    hasEqualizer = true,
                    hasMultiBattery = false,
                    hasWearingDetection = false,
                    hasTouchSensorControl = false
                )
                name.startsWith("WF-") -> DeviceCapabilities(
                    modelName = deviceName ?: "Sony TWS",
                    family = DeviceFamily.TWS_FLAGSHIP,
                    hasMultiBattery = true
                )
                else -> DeviceCapabilities(
                    modelName = deviceName ?: "Sony Audio",
                    family = DeviceFamily.GENERIC_SONY
                )
            }
        }
    }
}
