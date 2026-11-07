package com.twingo.fithub.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HeartRateZoneTest {

    private val yanis = Profile(
        id = 1L,
        name = "Yanis",
        weightKg = 75.0,
        maxHeartRate = 190,
        restingHeartRate = 55,
        isGuest = false,
    )

    @Test
    fun zoneTwoUsesKarvonenHeartRateReserve() {
        assertEquals(136..150, HeartRateZone.Z2.bpmRangeFor(yanis))
    }

    @Test
    fun multiZoneTargetSpansFromFirstToLastZone() {
        assertEquals(123..150, HeartRateZone.Z1.bpmRangeFor(yanis, HeartRateZone.Z2))
    }

    @Test
    fun zoneFiveReachesMaximumHeartRate() {
        assertEquals(177..190, HeartRateZone.Z5.bpmRangeFor(yanis))
    }

    @Test
    fun dynamicZoneIsFoundFromMeasuredBpm() {
        assertEquals(HeartRateZone.Z1, HeartRateZone.fromBpm(yanis, 130))
        assertEquals(HeartRateZone.Z2, HeartRateZone.fromBpm(yanis, 140))
        assertEquals(HeartRateZone.Z5, HeartRateZone.fromBpm(yanis, 185))
        assertEquals(HeartRateZone.Z5, HeartRateZone.fromBpm(yanis, 200))
    }

    @Test
    fun bpmBelowFirstZoneHasNoDynamicZone() {
        assertEquals(null, HeartRateZone.fromBpm(yanis, 90))
        assertEquals(null, HeartRateZone.fromBpm(yanis, 0))
    }
}
