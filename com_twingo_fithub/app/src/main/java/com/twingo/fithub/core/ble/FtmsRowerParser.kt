package com.twingo.fithub.core.ble

import kotlin.math.roundToInt

data class FtmsRowerSample(
    val strokeRateSpm: Int?,
    val paceSecondsPer500m: Int?,
    val powerWatts: Int?,
    val totalDistanceMeters: Int?,
    val heartRateBpm: Int?,
)

/**
 * Rower Data (0x2AD1).
 *
 * Attention : le bit 0 (More Data) utilise une logique inversée. La cadence de
 * rame et le nombre de coups sont présents quand le bit vaut 0. L'allure
 * instantanée (bit 3) est exprimée en secondes par 500 m.
 */
object FtmsRowerParser {

    fun parse(data: ByteArray): FtmsRowerSample? {
        val reader = BleByteReader(data)
        val flags = reader.u16() ?: return null

        var strokeRateSpm: Int? = null
        var paceSecondsPer500m: Int? = null
        var powerWatts: Int? = null
        var distanceMeters: Int? = null
        var heartRateBpm: Int? = null

        if (flags and 0x0001 == 0) {
            val rawStrokeRate = reader.u8() ?: return null
            strokeRateSpm = (rawStrokeRate / 2.0).roundToInt()
            if (!reader.skip(2)) return null
        }
        if (flags and 0x0002 != 0 && !reader.skip(1)) return null
        if (flags and 0x0004 != 0) {
            distanceMeters = reader.u24() ?: return null
        }
        if (flags and 0x0008 != 0) {
            paceSecondsPer500m = reader.u16() ?: return null
        }
        if (flags and 0x0010 != 0 && !reader.skip(2)) return null
        if (flags and 0x0020 != 0) {
            powerWatts = reader.s16() ?: return null
        }
        if (flags and 0x0040 != 0 && !reader.skip(2)) return null
        if (flags and 0x0080 != 0 && !reader.skip(2)) return null
        if (flags and 0x0100 != 0 && !reader.skip(5)) return null
        if (flags and 0x0200 != 0) {
            heartRateBpm = reader.u8() ?: return null
        }
        if (flags and 0x0400 != 0 && !reader.skip(1)) return null
        if (flags and 0x0800 != 0 && !reader.skip(2)) return null
        if (flags and 0x1000 != 0 && !reader.skip(2)) return null

        if (strokeRateSpm == null && paceSecondsPer500m == null && powerWatts == null &&
            distanceMeters == null && heartRateBpm == null
        ) {
            return null
        }
        return FtmsRowerSample(
            strokeRateSpm = strokeRateSpm,
            paceSecondsPer500m = paceSecondsPer500m,
            powerWatts = powerWatts,
            totalDistanceMeters = distanceMeters,
            heartRateBpm = heartRateBpm,
        )
    }
}
