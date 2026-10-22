package com.sony.xm6controller.data.protocol

import com.sony.xm6controller.domain.EqPreset
import com.sony.xm6controller.domain.NoiseControlMode
import com.sony.xm6controller.domain.NoiseControlState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MdrProtocolTest {

    @Test
    fun testChecksumCalculation() {
        val body = byteArrayOf(
            0x0c, 0x00, 0x00, 0x00, 0x00, 0x04,
            0xf8.toByte(), 0x05, 0x01, 0x01
        )
        val chk = MdrChecksum.calculate(body)
        assertEquals(0x0F.toByte(), chk)
    }

    @Test
    fun testAckPacketGeneration() {
        val ack0 = MdrFrameEncoder.buildAck(seq = 0)
        val expectedAck0 = byteArrayOf(
            0x3E.toByte(), 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x3C.toByte()
        )
        assertArrayEquals(expectedAck0, ack0)

        val ack1 = MdrFrameEncoder.buildAck(seq = 1)
        val expectedAck1 = byteArrayOf(
            0x3E.toByte(), 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x02, 0x3C.toByte()
        )
        assertArrayEquals(expectedAck1, ack1)
    }

    @Test
    fun testEscapeAndUnescape() {
        val original = byteArrayOf(
            0x10, 0x3E.toByte(), 0x20, 0x3C.toByte(), 0x3D.toByte(), 0x50
        )
        val escaped = MdrFrameEncoder.escape(original)

        val expectedEscaped = byteArrayOf(
            0x10,
            0x3D.toByte(), 0x2E.toByte(),
            0x20,
            0x3D.toByte(), 0x2C.toByte(),
            0x3D.toByte(), 0x2D.toByte(),
            0x50
        )
        assertArrayEquals(expectedEscaped, escaped)

        val unescaped = MdrFrameDecoder.unescape(escaped)
        assertArrayEquals(original, unescaped)
    }

    @Test
    fun testFrameEncodingAndDecodingRoundTrip() {
        val payload = byteArrayOf(0x68, 0x17, 0x01, 0x01, 0x00, 0x02, 0x00, 0x01)
        val encoded = MdrFrameEncoder.encode(
            type = MdrConstants.TYPE_COMMAND_1,
            seq = 1,
            payload = payload
        )

        val decoder = MdrFrameDecoder()
        val decodedPackets = decoder.feed(encoded)

        assertEquals(1, decodedPackets.size)
        val packet = decodedPackets[0]
        assertEquals(MdrConstants.TYPE_COMMAND_1, packet.type)
        assertEquals(1, packet.sequence)
        assertArrayEquals(payload, packet.payload)
    }

    @Test
    fun testSetNoiseControlPayloadsV2() {
        // Noise Cancelling
        val ncPayload = MdrCommandBuilder.buildSetNoiseControlPayload(
            targetMode = NoiseControlMode.NOISE_CANCELLING
        )
        assertEquals(MdrConstants.OP_ASC_SET, ncPayload[0]) // 0x68
        assertEquals(MdrConstants.SUBTYPE_NC_ASM_V2, ncPayload[1]) // 0x17
        assertEquals(MdrConstants.ASC_SWITCH_ON, ncPayload[3]) // on = 0x01
        assertEquals(MdrConstants.ASM_MODE_NC, ncPayload[4]) // amb = 0x00

        // Ambient Sound
        val ambientPayload = MdrCommandBuilder.buildSetNoiseControlPayload(
            targetMode = NoiseControlMode.AMBIENT_SOUND,
            ambientLevel = 15,
            focusOnVoice = true
        )
        assertEquals(MdrConstants.ASC_SWITCH_ON, ambientPayload[3]) // on = 0x01
        assertEquals(MdrConstants.ASM_MODE_AMBIENT, ambientPayload[4]) // amb = 0x01
        assertEquals(1.toByte(), ambientPayload[6]) // voice = 1
        assertEquals(15.toByte(), ambientPayload[7]) // level = 15

        // Off
        val offPayload = MdrCommandBuilder.buildSetNoiseControlPayload(
            targetMode = NoiseControlMode.OFF
        )
        assertEquals(MdrConstants.ASC_SWITCH_OFF, offPayload[3]) // on = 0x00
    }

    @Test
    fun testSetNoiseControlPayloadsV1Legacy() {
        // Legacy MDR v1 (XM4 / XM3) uses 7-byte payload without wind byte
        val legacyState = NoiseControlState(
            subtype = MdrConstants.SUBTYPE_NC_ASM_V1,
            hasWindByte = false
        )
        val ncPayloadV1 = MdrCommandBuilder.buildSetNoiseControlPayload(
            targetMode = NoiseControlMode.NOISE_CANCELLING,
            currentState = legacyState
        )
        assertEquals(7, ncPayloadV1.size)
        assertEquals(MdrConstants.SUBTYPE_NC_ASM_V1, ncPayloadV1[1])
        assertEquals(MdrConstants.ASC_SWITCH_ON, ncPayloadV1[3])
    }

    @Test
    fun testTwsMultiCellBatteryParsing() {
        // TWS: L=90% (not charging), R=85% (charging), Case=100% (charging)
        val twsPayload = byteArrayOf(
            0x23.toByte(), 0x01.toByte(),
            90.toByte(), 0x00.toByte(),
            85.toByte(), 0x01.toByte(),
            100.toByte(), 0x01.toByte()
        )
        val batt = MdrResponseParser.parseBatteryState(twsPayload)
        assertNotNull(batt)
        assertTrue(batt!!.isTws)
        assertEquals(90, batt.leftLevel)
        assertFalse(batt.isLeftCharging)
        assertEquals(85, batt.rightLevel)
        assertTrue(batt.isRightCharging)
        assertEquals(100, batt.caseLevel)
        assertTrue(batt.isCaseCharging)
        assertTrue(batt.displayString.contains("L: 90%"))
        assertTrue(batt.displayString.contains("R: 85%⚡"))
        assertTrue(batt.displayString.contains("Case: 100%⚡"))
    }

    @Test
    fun testEqualizerCommandBuilders() {
        // Preset
        val presetFrame = MdrCommandBuilder.buildSetEqualizerPresetFrame(0, EqPreset.BASS_BOOST)
        assertTrue(presetFrame.isNotEmpty())

        // Custom Bands + Clear Bass (+5 dB clear bass, and +2, +1, 0, -1, -2)
        val bandsFrame = MdrCommandBuilder.buildSetEqualizerBandsFrame(
            seq = 0,
            preset = EqPreset.CUSTOM_1,
            clearBass = 5,
            bands = listOf(2, 1, 0, -1, -2)
        )
        val decoder = MdrFrameDecoder()
        val packets = decoder.feed(bandsFrame)
        assertEquals(1, packets.size)
        val pl = packets[0].payload
        assertEquals(MdrConstants.OP_EQ_SET, pl[0])
        assertEquals(EqPreset.CUSTOM_1.id.toByte(), pl[2])
        // Offset +10 verification
        assertEquals(15.toByte(), pl[4]) // clear bass: 5 + 10 = 15
        assertEquals(12.toByte(), pl[5]) // b0: 2 + 10 = 12
        assertEquals(11.toByte(), pl[6]) // b1: 1 + 10 = 11
        assertEquals(10.toByte(), pl[7]) // b2: 0 + 10 = 10
        assertEquals(9.toByte(), pl[8])  // b3: -1 + 10 = 9
        assertEquals(8.toByte(), pl[9])  // b4: -2 + 10 = 8
    }

    @Test
    fun testSpeakToChatAndDseeBuilders() {
        val stcFrame = MdrCommandBuilder.buildSetSpeakToChatFrame(0, enabled = true, sensitivity = 0, timeout = 2)
        assertTrue(stcFrame.isNotEmpty())

        val dseeFrame = MdrCommandBuilder.buildSetDseeFrame(1, enabled = true)
        val decoder = MdrFrameDecoder()
        val packets = decoder.feed(dseeFrame)
        assertEquals(1, packets.size)
        assertEquals(MdrConstants.OP_DSEE_SET, packets[0].payload[0])
        assertEquals(0x01.toByte(), packets[0].payload[2])
    }

    @Test
    fun testParseAscStatePacket() {
        val notifyPayload = byteArrayOf(
            0x69.toByte(), 0x17, 0x01, 0x01, 0x01, 0x02, 0x00, 0x14
        )

        assertTrue(MdrResponseParser.isAscStatePacket(notifyPayload))
        val state = MdrResponseParser.parseAscState(notifyPayload)
        assertNotNull(state)
        assertEquals(NoiseControlMode.AMBIENT_SOUND, state!!.mode)
        assertEquals(20, state.ambientLevel)
        assertFalse(state.focusOnVoice)
    }

    @Test
    fun testStrictCyclingOrder() {
        var mode = NoiseControlMode.NOISE_CANCELLING
        mode = mode.next()
        assertEquals(NoiseControlMode.AMBIENT_SOUND, mode)
        mode = mode.next()
        assertEquals(NoiseControlMode.OFF, mode)
        mode = mode.next()
        assertEquals(NoiseControlMode.NOISE_CANCELLING, mode)
    }

    @Test
    fun testCycleSchemes() {
        val nc = NoiseControlMode.NOISE_CANCELLING
        val amb = nc.next(com.sony.xm6controller.domain.TileCycleScheme.NC_AMBIENT)
        assertEquals(NoiseControlMode.AMBIENT_SOUND, amb)
        val backToNc = amb.next(com.sony.xm6controller.domain.TileCycleScheme.NC_AMBIENT)
        assertEquals(NoiseControlMode.NOISE_CANCELLING, backToNc)

        val off = nc.next(com.sony.xm6controller.domain.TileCycleScheme.NC_OFF)
        assertEquals(NoiseControlMode.OFF, off)
        val backFromOff = off.next(com.sony.xm6controller.domain.TileCycleScheme.NC_OFF)
        assertEquals(NoiseControlMode.NOISE_CANCELLING, backFromOff)
    }

    @Test
    fun testPeqParserJsonAndText() {
        val sampleAutoEqText = """
            Preamp: -4.5 dB
            Filter 1: ON PK Fc 28 Hz Gain 4.8 dB Q 1.41
            Filter 2: ON LSC Fc 105 Hz Gain -3.2 dB Q 0.71
            Filter 3: ON PK Fc 1200 Hz Gain 2.1 dB Q 2.0
            Filter 4: ON HSC Fc 8000 Hz Gain -1.5 dB Q 0.71
        """.trimIndent()

        val profile = com.sony.xm6controller.domain.PeqParser.parse(sampleAutoEqText, "Test AutoEq")
        assertNotNull(profile)
        assertEquals(-4.5f, profile!!.preampDb)
        assertEquals(4, profile.filters.size)

        val tenBand = profile.sampleToTenBand()
        assertEquals(10, tenBand.bands.size)
        val hw = tenBand.toSonyHardwareEq()
        assertTrue(hw.clearBass in -10..10)
        assertTrue(hw.band400Hz in -10..10)
        assertTrue(hw.band1kHz in -10..10)

        // Test JSON parsing
        val sampleJson = """
            {
              "name": "WH-1000XM6 Harman",
              "preamp": -3.2,
              "filters": [
                {"type": "PK", "freq": 105.0, "gain": -3.5, "q": 1.41},
                {"type": "LSC", "freq": 80.0, "gain": 5.0, "q": 0.71}
              ]
            }
        """.trimIndent()
        val jsonProfile = com.sony.xm6controller.domain.PeqParser.parse(sampleJson, "Test JSON")
        assertNotNull(jsonProfile)
        assertEquals("WH-1000XM6 Harman", jsonProfile!!.name)
        assertEquals(-3.2f, jsonProfile.preampDb)
        assertEquals(2, jsonProfile.filters.size)
    }

    @Test
    fun testDeviceCapabilitiesResolution() {
        val xm6 = com.sony.xm6controller.domain.DeviceCapabilities.resolve("WH-1000XM6")
        assertEquals(com.sony.xm6controller.domain.DeviceFamily.OVER_EAR_FLAGSHIP, xm6.family)
        assertTrue(xm6.hasSpeakToChat)
        assertTrue(xm6.hasDsee)
        assertFalse(xm6.hasMultiBattery)

        val xm3 = com.sony.xm6controller.domain.DeviceCapabilities.resolve("WH-1000XM3")
        assertFalse(xm3.hasSpeakToChat) // XM3 does not have Speak-to-Chat

        val tws = com.sony.xm6controller.domain.DeviceCapabilities.resolve("WF-1000XM5")
        assertEquals(com.sony.xm6controller.domain.DeviceFamily.TWS_FLAGSHIP, tws.family)
        assertTrue(tws.hasMultiBattery)

        val ult = com.sony.xm6controller.domain.DeviceCapabilities.resolve("WH-ULT900N")
        assertEquals(com.sony.xm6controller.domain.DeviceFamily.BASS_SERIES, ult.family)
    }

    @Test
    fun testAutoEqCatalogAndUserPresets() {
        val catalog = com.sony.xm6controller.domain.AutoEqCatalog.entries
        assertTrue(catalog.isNotEmpty())
        val xm5Profile = catalog.find { it.modelName == "WH-1000XM5" }
        assertNotNull(xm5Profile)

        val preset = com.sony.xm6controller.domain.UserEqPreset(
            name = "Test Bass Curve",
            tenBandGains = listOf(8f, 7f, 5f, 2f, 0f, 0f, 1f, 2f, 2f, 1f),
            clearBass = 7
        )
        val state = preset.toTenBandEqState()
        assertEquals(7, state.clearBass)
        assertEquals(10, state.bands.size)
        assertEquals(8f, state.bands[0].gainDb)
    }
}
