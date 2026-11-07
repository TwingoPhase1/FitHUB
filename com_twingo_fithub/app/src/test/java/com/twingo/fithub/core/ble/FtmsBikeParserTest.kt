package com.twingo.fithub.core.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtmsBikeParserTest {

    @Test
    fun parsesInstantaneousSpeedOnly() {
        val frame = byteArrayOf(0x00, 0x00, 0xC4.toByte(), 0x09)
        val sample = FtmsBikeParser.parse(frame)

        assertEquals(25.0, sample?.speedKmh ?: 0.0, 0.0001)
        assertNull(sample?.cadenceRpm)
        assertNull(sample?.powerWatts)
    }

    @Test
    fun parsesCadenceAndPowerWithLittleEndianValues() {
        val frame = byteArrayOf(
            0x44, 0x00, // flags : cadence + puissance, vitesse présente
            0xF0.toByte(), 0x0A, // 28,00 km/h
            0xB4.toByte(), 0x00, // 180 / 2 = 90 tr/min
            0xFA.toByte(), 0x00, // 250 W
        )
        val sample = FtmsBikeParser.parse(frame)

        assertEquals(28.0, sample?.speedKmh ?: 0.0, 0.0001)
        assertEquals(90, sample?.cadenceRpm)
        assertEquals(250, sample?.powerWatts)
    }

    @Test
    fun skipsEveryOptionalFieldInSpecificationOrder() {
        val frame = byteArrayOf(
            0x7E, 0x00, // vitesse, vit. moy., cadence, cad. moy., distance, résistance, puissance
            0xD0.toByte(), 0x07, // 20,00 km/h
            0x6C, 0x07, // 19,00 km/h
            0xAA.toByte(), 0x00, // 85 tr/min
            0xA0.toByte(), 0x00, // 80 tr/min
            0xD2.toByte(), 0x04, 0x00, // 1234 m
            0x05, 0x00, // résistance 5
            0xB4.toByte(), 0x00, // 180 W
        )
        val sample = FtmsBikeParser.parse(frame)

        assertEquals(20.0, sample?.speedKmh ?: 0.0, 0.0001)
        assertEquals(85, sample?.cadenceRpm)
        assertEquals(180, sample?.powerWatts)
        assertEquals(1234, sample?.totalDistanceMeters)
    }

    @Test
    fun acceptsFramesWithoutInstantaneousSpeed() {
        val frame = byteArrayOf(
            0x45, 0x00, // More Data = 1, cadence + puissance
            0xB4.toByte(), 0x00,
            0xFA.toByte(), 0x00,
        )
        val sample = FtmsBikeParser.parse(frame)

        assertNull(sample?.speedKmh)
        assertEquals(90, sample?.cadenceRpm)
        assertEquals(250, sample?.powerWatts)
    }

    @Test
    fun parsesSignedPower() {
        val frame = byteArrayOf(
            0x40, 0x00, // vitesse + puissance
            0x00, 0x00,
            0xFB.toByte(), 0xFF.toByte(), // -5 W
        )
        assertEquals(-5, FtmsBikeParser.parse(frame)?.powerWatts)
    }

    @Test
    fun rejectsTruncatedOptionalField() {
        val frame = byteArrayOf(
            0x44, 0x00, // cadence annoncée
            0xF0.toByte(), 0x0A,
            0xB4.toByte(), // cadence incomplète
        )
        assertNull(FtmsBikeParser.parse(frame))
    }
}
