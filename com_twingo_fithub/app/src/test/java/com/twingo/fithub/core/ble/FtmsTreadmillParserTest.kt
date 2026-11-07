package com.twingo.fithub.core.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtmsTreadmillParserTest {

    @Test
    fun parsesSpeedDistanceAndIncline() {
        val frame = byteArrayOf(
            0x0C, 0x00, // distance + inclinaison/rampe
            0x26, 0x02, // 5,50 km/h
            0xD2.toByte(), 0x04, 0x00, // 1234 m
            0x46, 0x00, // 7,0 %
            0x28, 0x00, // 4,0°
        )
        val sample = FtmsTreadmillParser.parse(frame)

        assertEquals(5.5, sample?.speedKmh ?: 0.0, 0.0001)
        assertEquals(1234, sample?.totalDistanceMeters)
        assertEquals(7.0, sample?.inclinePercent ?: 0.0, 0.0001)
    }

    @Test
    fun parsesNegativeIncline() {
        val frame = byteArrayOf(
            0x08, 0x00,
            0xF4.toByte(), 0x01, // 5,00 km/h
            0xE7.toByte(), 0xFF.toByte(), // -2,5 %
            0x00, 0x00,
        )
        assertEquals(-2.5, FtmsTreadmillParser.parse(frame)?.inclinePercent ?: 0.0, 0.0001)
    }

    @Test
    fun skipsOptionalFieldsBeforeHeartRate() {
        val frame = byteArrayOf(
            0x2E, 0x01, // vit. moy., distance, inclinaison, allure, FC
            0x58, 0x02, // 6,00 km/h
            0x4E, 0x02, // 5,90 km/h
            0xF4.toByte(), 0x01, 0x00, // 500 m
            0x64, 0x00, // 10,0 %
            0x39, 0x00, // 5,7°
            0x1E, // allure
            0x91.toByte(), // 145 bpm
        )
        val sample = FtmsTreadmillParser.parse(frame)

        assertEquals(6.0, sample?.speedKmh ?: 0.0, 0.0001)
        assertEquals(500, sample?.totalDistanceMeters)
        assertEquals(10.0, sample?.inclinePercent ?: 0.0, 0.0001)
        assertEquals(145, sample?.heartRateBpm)
    }

    @Test
    fun parsesPowerOutputWhenPresent() {
        val frame = byteArrayOf(
            0x00, 0x10, // force sur bande + puissance
            0x20, 0x03, // 8,00 km/h
            0x78, 0x00, // force 120 N
            0xD2.toByte(), 0x00, // 210 W
        )
        assertEquals(210, FtmsTreadmillParser.parse(frame)?.powerWatts)
    }

    @Test
    fun acceptsRecordWithoutInstantaneousSpeed() {
        val frame = byteArrayOf(
            0x05, 0x00, // More Data = 1, distance
            0x64, 0x00, 0x00, // 100 m
        )
        val sample = FtmsTreadmillParser.parse(frame)

        assertNull(sample?.speedKmh)
        assertEquals(100, sample?.totalDistanceMeters)
    }

    @Test
    fun rejectsTruncatedField() {
        val frame = byteArrayOf(
            0x0C, 0x00,
            0x26, 0x02,
            0xD2.toByte(), 0x04, 0x00,
            0x46, // inclinaison incomplète
        )
        assertNull(FtmsTreadmillParser.parse(frame))
    }
}
