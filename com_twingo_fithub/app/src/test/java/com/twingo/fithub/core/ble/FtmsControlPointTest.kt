package com.twingo.fithub.core.ble

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtmsControlPointTest {

    @Test
    fun encodesRequestControl() {
        assertArrayEquals(byteArrayOf(0x00), FtmsControlPoint.encodeRequestControl())
    }

    @Test
    fun encodesReset() {
        assertArrayEquals(byteArrayOf(0x01), FtmsControlPoint.encodeReset())
    }

    @Test
    fun encodesStartOrResume() {
        assertArrayEquals(byteArrayOf(0x07), FtmsControlPoint.encodeStartOrResume())
    }

    @Test
    fun encodesPause() {
        assertArrayEquals(byteArrayOf(0x08), FtmsControlPoint.encodePause())
    }

    @Test
    fun encodesSetTargetPowerPositive() {
        // 250 W = 0x00FA → little-endian FA 00
        assertArrayEquals(
            byteArrayOf(0x05, 0xFA.toByte(), 0x00),
            FtmsControlPoint.encodeSetTargetPower(250),
        )
    }

    @Test
    fun encodesSetTargetPowerNegative() {
        // -50 W = 0xFFCE → little-endian CE FF
        assertArrayEquals(
            byteArrayOf(0x05, 0xCE.toByte(), 0xFF.toByte()),
            FtmsControlPoint.encodeSetTargetPower(-50),
        )
    }

    @Test
    fun encodesSetTargetPowerZero() {
        assertArrayEquals(
            byteArrayOf(0x05, 0x00, 0x00),
            FtmsControlPoint.encodeSetTargetPower(0),
        )
    }

    @Test
    fun encodesSetTargetResistance() {
        assertArrayEquals(byteArrayOf(0x04, 0x04), FtmsControlPoint.encodeSetTargetResistance(4))
        assertArrayEquals(byteArrayOf(0x04, 0x00), FtmsControlPoint.encodeSetTargetResistance(0))
        assertArrayEquals(byteArrayOf(0x04, 0xFF.toByte()), FtmsControlPoint.encodeSetTargetResistance(255))
    }

    @Test
    fun encodesSetTargetIncline() {
        // 5.5% → 55 → 0x0037 → little-endian 37 00
        assertArrayEquals(
            byteArrayOf(0x03, 0x37, 0x00),
            FtmsControlPoint.encodeSetTargetIncline(5.5f),
        )
        // -2.5% → -25 → 0xFFE7 → little-endian E7 FF
        assertArrayEquals(
            byteArrayOf(0x03, 0xE7.toByte(), 0xFF.toByte()),
            FtmsControlPoint.encodeSetTargetIncline(-2.5f),
        )
        // 0%
        assertArrayEquals(
            byteArrayOf(0x03, 0x00, 0x00),
            FtmsControlPoint.encodeSetTargetIncline(0f),
        )
    }

    @Test
    fun parsesSuccessResponseForRequestControl() {
        val response = FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0x00.toByte(), 0x01.toByte()))
        assertEquals(FtmsControlPoint.OpCode.REQUEST_CONTROL, response?.requestOpCode)
        assertEquals(FtmsControlPoint.ResponseCode.SUCCESS, response?.result)
    }

    @Test
    fun parsesSuccessResponseForStartOrResume() {
        val response = FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0x07.toByte(), 0x01.toByte()))
        assertEquals(FtmsControlPoint.OpCode.START_OR_RESUME, response?.requestOpCode)
        assertEquals(FtmsControlPoint.ResponseCode.SUCCESS, response?.result)
    }

    @Test
    fun parsesNotSupportedResponse() {
        val response = FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0x05.toByte(), 0x02.toByte()))
        assertEquals(FtmsControlPoint.OpCode.SET_TARGET_POWER, response?.requestOpCode)
        assertEquals(FtmsControlPoint.ResponseCode.NOT_SUPPORTED, response?.result)
    }

    @Test
    fun parsesControlNotPermitted() {
        val response = FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0x00.toByte(), 0x05.toByte()))
        assertEquals(FtmsControlPoint.ResponseCode.CONTROL_NOT_PERMITTED, response?.result)
    }

    @Test
    fun rejectsMalformedResponses() {
        assertNull(FtmsControlPoint.parseResponse(byteArrayOf()))
        assertNull(FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte())))
        assertNull(FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0x00.toByte())))
        assertNull(FtmsControlPoint.parseResponse(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x01.toByte()))) // pas 0x80
        assertNull(FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0xFF.toByte(), 0x01.toByte()))) // opcode inconnu
        assertNull(FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0x00.toByte(), 0xFF.toByte()))) // response code inconnu
    }
}