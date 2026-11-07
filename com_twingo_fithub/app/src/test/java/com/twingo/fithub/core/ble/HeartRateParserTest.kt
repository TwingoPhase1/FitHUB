package com.twingo.fithub.core.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateParserTest {

    @Test
    fun parsesEightBitHeartRate() {
        assertEquals(72, HeartRateParser.parse(byteArrayOf(0x00, 72)))
    }

    @Test
    fun parsesSixteenBitLittleEndianHeartRate() {
        // Flags bit 0 = 1, valeur 0x00B4 = 180 bpm.
        val frame = byteArrayOf(0x01, 0xB4.toByte(), 0x00)
        assertEquals(180, HeartRateParser.parse(frame))
    }

    @Test
    fun ignoresOptionalFieldsAfterHeartRate() {
        // Contact capteur + énergie dépensée + intervalles RR.
        val frame = byteArrayOf(0x19, 0x59, 0x00, 0x2C, 0x00)
        assertEquals(89, HeartRateParser.parse(frame))
    }

    @Test
    fun rejectsTruncatedFrames() {
        assertNull(HeartRateParser.parse(byteArrayOf()))
        assertNull(HeartRateParser.parse(byteArrayOf(0x00)))
        assertNull(HeartRateParser.parse(byteArrayOf(0x01, 0x64)))
    }
}
