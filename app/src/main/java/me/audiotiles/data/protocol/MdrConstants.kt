package me.audiotiles.data.protocol

import java.util.UUID

/**
 * Sony MDR (Main Device Radio) Protocol Constants.
 *
 * Reverse-engineered from official OEM Sound Connect / Headphones Connect app
 * and open-source references (mos9527/SonyHeadphonesClient, marconvcm/sony-device-center, Gadgetbridge).
 *
 * Packet Framing Structure:
 * 0x3E [ESCAPE(type:1B, seq:1B, len:4B_BE, payload:NB, checksum:1B)] 0x3C
 */
object MdrConstants {
    // --- Frame Framing Markers ---
    const val START_BYTE: Byte = 0x3E.toByte() // '>' Start of frame
    const val STOP_BYTE: Byte = 0x3C.toByte()  // '<' End of frame
    const val ESCAPE_BYTE: Byte = 0x3D.toByte()// '=' Escape marker
    const val ESCAPE_MASK: Byte = 0xEF.toByte()// b & ESCAPE_MASK (e.g. 0x3E -> 0x2E, 0x3C -> 0x2C, 0x3D -> 0x2D)
    const val UNESCAPE_MASK: Byte = 0x10.toByte() // b | 0x10

    // --- Message Types (First byte of unescaped payload header) ---
    const val TYPE_ACK: Byte = 0x01.toByte()       // Alternating sequence ACK packet
    const val TYPE_COMMAND_1: Byte = 0x0C.toByte() // Primary command/response/notify channel
    const val TYPE_COMMAND_2: Byte = 0x0E.toByte() // Secondary command channel

    // --- Handshake / Identity OpCodes ---
    const val OP_INIT_REQ: Byte = 0x00.toByte()    // Init request payload: [0x00, 0x00]
    const val OP_INIT_RESP: Byte = 0x01.toByte()   // Init reply from device: [0x01, ...]

    // --- Ambient Sound Control (ASC) / Noise Control OpCodes ---
    const val OP_ASC_GET: Byte = 0x66.toByte()     // Query noise control state: [0x66, subtype]
    const val OP_ASC_RET: Byte = 0x67.toByte()     // Response to ASC GET: [0x67, subtype, 0x01, on, amb, wind?, voice, level]
    const val OP_ASC_SET: Byte = 0x68.toByte()     // Set noise control state: [0x68, subtype, 0x01, on, amb, wind?, voice, level]
    const val OP_ASC_NOTIFY: Byte = 0x69.toByte()  // Unsolicited state notify (e.g. physical NC button pressed)

    // --- Battery Telemetry OpCodes ---
    const val OP_BATTERY_GET: Byte = 0x22.toByte()     // Query battery state on V2: [0x22, 0x00] (NOTE: Never send to V1!)
    const val OP_BATTERY_RET: Byte = 0x23.toByte()     // Battery response: [0x23, 0x00, level, charging] or TWS [0x23, 0x01, L, L_c, R, R_c, C, C_c]
    const val OP_BATTERY_NOTIFY: Byte = 0x24.toByte()  // Unsolicited battery update

    // --- Equalizer & Clear Bass OpCodes (5-band EQ + Clear Bass) ---
    const val OP_EQ_GET: Byte = 0x56.toByte()          // Query EQ params: [0x56, 0x00]
    const val OP_EQ_RET: Byte = 0x57.toByte()          // EQ response
    const val OP_EQ_SET: Byte = 0x58.toByte()          // Set EQ preset or custom bands
    const val OP_EQ_NOTIFY: Byte = 0x59.toByte()       // Unsolicited EQ update

    // --- Speak-to-Chat OpCodes (Smart Talking Mode) ---
    const val OP_STC_GET: Byte = 0xF6.toByte()         // Query Speak-to-Chat status
    const val OP_STC_RET: Byte = 0xF7.toByte()         // Speak-to-Chat response
    const val OP_STC_SET: Byte = 0xF8.toByte()         // Set Speak-to-Chat params
    const val OP_STC_NOTIFY: Byte = 0xF9.toByte()      // Unsolicited Speak-to-Chat update

    // --- DSEE Extreme / Upscaling OpCodes ---
    const val OP_DSEE_GET: Byte = 0xE6.toByte()        // Query DSEE status: [0xE6, 0x01]
    const val OP_DSEE_RET: Byte = 0xE7.toByte()        // DSEE response
    const val OP_DSEE_SET: Byte = 0xE8.toByte()        // Set DSEE: [0xE8, 0x01, 0x01 auto / 0x00 off]
    const val OP_DSEE_NOTIFY: Byte = 0xE9.toByte()     // Unsolicited DSEE update

    // --- System / Hardware Sensors OpCodes ---
    const val OP_SYSTEM_GET: Byte = 0xD6.toByte()      // Query system features (Wearing detection, auto power off)
    const val OP_SYSTEM_RET: Byte = 0xD7.toByte()
    const val OP_SYSTEM_SET: Byte = 0xD8.toByte()      // Set wearing pause (0x01) or auto-off (0x02)
    const val OP_SYSTEM_NOTIFY: Byte = 0xD9.toByte()

    const val OP_TOUCH_SENSOR_SET: Byte = 0xC8.toByte() // Touch control panel toggle: [0xC8, 0x01, on/off]

    // --- Noise Control Subtypes ---
    const val SUBTYPE_NC_ASM_V2: Byte = 0x17.toByte()  // MDR v2 standard (WH-1000XM6, XM5, WF-1000XM5, LinkBuds, ULT WEAR)
    const val SUBTYPE_NC_ASM_V1: Byte = 0x07.toByte()  // MDR v1 legacy (WH-1000XM4, WH-1000XM3, WF-1000XM3)
    const val SUBTYPE_NC_ASM_ALT: Byte = 0x15.toByte() // Alternate subtype used on select firmware versions (7-byte payload)

    // --- ASC Field Values ---
    const val ASC_SWITCH_OFF: Byte = 0x00.toByte()
    const val ASC_SWITCH_ON: Byte = 0x01.toByte()

    const val ASM_MODE_NC: Byte = 0x00.toByte()      // Noise Cancelling
    const val ASM_MODE_AMBIENT: Byte = 0x01.toByte() // Ambient Sound

    const val WIND_NOISE_REDUCTION_OFF: Byte = 0x02.toByte() // Normal mode (no wind noise reduction)
    const val WIND_NOISE_REDUCTION_ON: Byte = 0x03.toByte()  // Wind noise reduction active

    const val VOICE_FOCUS_OFF: Byte = 0x00.toByte()
    const val VOICE_FOCUS_ON: Byte = 0x01.toByte()

    const val AMBIENT_LEVEL_MIN = 1
    const val AMBIENT_LEVEL_MAX = 20
    const val AMBIENT_LEVEL_DEFAULT = 20

    // --- Service UUIDs for RFCOMM / SPP ---
    // Standard Bluetooth Serial Port Profile (SPP)
    val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Sony MDR Protocol V2 Custom Service UUIDs (WH-1000XM5, WH-1000XM6, LinkBuds, ULT WEAR)
    val UUID_SONY_V2_PRIMARY: UUID = UUID.fromString("956c7b26-d49a-4ba8-b03f-b17d393cb6e2")
    val UUID_SONY_V2_SECONDARY: UUID = UUID.fromString("956C7B26-B496-4447-AD7B-3A47900D8C86")
    val UUID_SONY_ALT: UUID = UUID.fromString("ba69e0f5-16e3-2db3-ad46-68503e20cc96")

    // Sony MDR Protocol V1 Service UUID (Legacy: WH-1000XM3, WH-1000XM4)
    val UUID_SONY_V1: UUID = UUID.fromString("96cc203e-5068-46ad-b32d-e316f5e069ba")
}
