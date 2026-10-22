package com.sony.xm6controller.data.protocol

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
        // Test vector from reverse-engineered HCI trace:
        // Type: 0x0C, Seq: 0x00, Len: 0x00 00 00 04, Payload: [F8, 05, 01, 01]
        // Sum = 12 + 0 + 4 + 248 + 5 + 1 + 1 = 271 = 0x10F -> 0x0F
        val body = byteArrayOf(
            0x0c, 0x00, 0x00, 0x00, 0x00, 0x04,
            0xf8.toByte(), 0x05, 0x01, 0x01
        )
        val chk = MdrChecksum.calculate(body)
        assertEquals(0x0F.toByte(), chk)
    }

    @Test
    fun testAckPacketGeneration() {
        // Test ACK for seq 0
        val ack0 = MdrFrameEncoder.buildAck(seq = 0)
        // Expected: 0x3E, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x3C
        val expectedAck0 = byteArrayOf(
            0x3E.toByte(), 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x3C.toByte()
        )
        assertArrayEquals(expectedAck0, ack0)

        // Test ACK for seq 1
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

        // 0x3E -> 0x3D, 0x2E
        // 0x3C -> 0x3D, 0x2C
        // 0x3D -> 0x3D, 0x2D
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
    fun testSetNoiseControlPayloads() {
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
    fun testParseAscStatePacket() {
        // Synthetic notify from headset when physical NC button is pressed:
        // [0x69, 0x17, 0x01, on=0x01, amb=0x01, wind=0x02, voice=0x00, level=0x14]
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
        // NC <-> Ambient scheme
        val nc = NoiseControlMode.NOISE_CANCELLING
        val amb = nc.next(com.sony.xm6controller.domain.TileCycleScheme.NC_AMBIENT)
        assertEquals(NoiseControlMode.AMBIENT_SOUND, amb)
        val backToNc = amb.next(com.sony.xm6controller.domain.TileCycleScheme.NC_AMBIENT)
        assertEquals(NoiseControlMode.NOISE_CANCELLING, backToNc)

        // NC <-> Off scheme
        val off = nc.next(com.sony.xm6controller.domain.TileCycleScheme.NC_OFF)
        assertEquals(NoiseControlMode.OFF, off)
        val backFromOff = off.next(com.sony.xm6controller.domain.TileCycleScheme.NC_OFF)
        assertEquals(NoiseControlMode.NOISE_CANCELLING, backFromOff)
    }

    @Test
    fun testBatteryFrameEncoding() {
        val frame = MdrCommandBuilder.buildGetBatteryFrame(seq = 0)
        assertTrue(frame.isNotEmpty())
        assertEquals(MdrConstants.START_BYTE, frame[0])
        assertEquals(MdrConstants.STOP_BYTE, frame[frame.size - 1])

        val decoder = MdrFrameDecoder()
        val packets = decoder.feed(frame)
        assertEquals(1, packets.size)
        assertEquals(MdrConstants.TYPE_COMMAND_1, packets[0].type)
        assertEquals(MdrConstants.OP_BATTERY_GET, packets[0].payload[0])
    }

    @Test
    fun testWindNoiseReductionState() {
        val payloadWithWind = byteArrayOf(
            0x67, 0x17, 0x01, 0x01, 0x00, 0x03, 0x00, 0x01
        )
        val state = MdrResponseParser.parseAscState(payloadWithWind)
        assertNotNull(state)
        assertTrue(state!!.windNoiseReduction)
        assertEquals(NoiseControlMode.NOISE_CANCELLING, state.mode)
    }
}
