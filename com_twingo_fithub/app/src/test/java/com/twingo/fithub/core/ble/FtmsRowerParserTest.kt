package com.twingo.fithub.core.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtmsRowerParserTest {

    @Test
    fun parsesStrokeRatePaceAndPower() {
        val frame = byteArrayOf(
            0x2C, 0x02, // distance + allure + puissance + FC
            0x34, // 52 / 2 = 26 coups/min
            0x7B, 0x00, // 123 coups
            0xF4.toByte(), 0x01, 0x00, // 500 m
            0x7D, 0x00, // 125 s / 500 m
            0xDC.toByte(), 0x00, // 220 W
            0x96.toByte(), // 150 bpm
        )
        val sample = FtmsRowerParser.parse(frame)

        assertEquals(26, sample?.strokeRateSpm)
        assertEquals(125, sample?.paceSecondsPer500m)
        assertEquals(220, sample?.powerWatts)
        assertEquals(500, sample?.totalDistanceMeters)
        assertEquals(150, sample?.heartRateBpm)
    }

    @Test
    fun respectsInvertedMoreDataBit() {
        val frame = byteArrayOf(
            0x27, 0x00, // More Data = 1, cadence moy., distance, puissance
            0x32, // cadence moyenne 25
            0xE8.toByte(), 0x03, 0x00, // 1000 m
            0xB4.toByte(), 0x00, // 180 W
        )
        val sample = FtmsRowerParser.parse(frame)

        assertNull(sample?.strokeRateSpm)
        assertEquals(180, sample?.powerWatts)
        assertEquals(1000, sample?.totalDistanceMeters)
    }

    @Test
    fun skipsAllOptionalFieldsInOrder() {
        val frame = byteArrayOf(
            0xFE.toByte(), 0x1F, // tous les champs sauf More Data
            0x3C, // 30 coups/min
            0xC8.toByte(), 0x00, // 200 coups
            0x3A, // cadence moyenne 29
            0xD0.toByte(), 0x07, 0x00, // 2000 m
            0x78, 0x00, // 120 s / 500 m
            0x7A, 0x00, // 122 s / 500 m
            0xC8.toByte(), 0x00, // 200 W
            0xC6.toByte(), 0x00, // 198 W
            0x04, 0x00, // résistance 4
            0x64, 0x00, // 100 kcal
            0x58, 0x02, // 600 kcal/h
            0x0A, // 10 kcal/min
            0x9B.toByte(), // 155 bpm
            0x5A, // MET 9,0
            0x58, 0x02, // 600 s
            0x2C, 0x01, // 300 s restantes
        )
        val sample = FtmsRowerParser.parse(frame)

        assertEquals(30, sample?.strokeRateSpm)
        assertEquals(120, sample?.paceSecondsPer500m)
        assertEquals(200, sample?.powerWatts)
        assertEquals(2000, sample?.totalDistanceMeters)
        assertEquals(155, sample?.heartRateBpm)
    }

    @Test
    fun parsesSignedPower() {
        val frame = byteArrayOf(
            0x21, 0x00, // More Data = 1, puissance
            0xF6.toByte(), 0xFF.toByte(), // -10 W
        )
        val sample = FtmsRowerParser.parse(frame)

        assertNull(sample?.strokeRateSpm)
        assertEquals(-10, sample?.powerWatts)
    }

    @Test
    fun rejectsTruncatedField() {
        val frame = byteArrayOf(
            0x2C, 0x02,
            0x34,
            0x7B, 0x00,
            0xF4.toByte(), 0x01, 0x00,
            0x7D, 0x00,
            0xDC.toByte(), // puissance incomplète
        )
        assertNull(FtmsRowerParser.parse(frame))
    }
}
