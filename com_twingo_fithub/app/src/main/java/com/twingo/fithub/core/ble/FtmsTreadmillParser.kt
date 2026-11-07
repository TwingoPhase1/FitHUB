package com.twingo.fithub.core.ble

data class FtmsTreadmillSample(
    val speedKmh: Double?,
    val totalDistanceMeters: Int?,
    val inclinePercent: Double?,
    val powerWatts: Int?,
    val heartRateBpm: Int?,
)

/**
 * Treadmill Data (0x2ACD).
 *
 * La vitesse instantanée est obligatoire quand le bit 0 (More Data) vaut 0.
 * Les champs optionnels sont lus dans l'ordre imposé par la spécification :
 * vitesse moyenne, distance uint24, inclinaison/rampe, dénivelé, allures,
 * énergie, FC, MET, temps, force/puissance, pas.
 */
object FtmsTreadmillParser {

    fun parse(data: ByteArray): FtmsTreadmillSample? {
        val reader = BleByteReader(data)
        val flags = reader.u16() ?: return null

        var speedKmh: Double? = null
        var distanceMeters: Int? = null
        var inclinePercent: Double? = null
        var powerWatts: Int? = null
        var heartRateBpm: Int? = null

        if (flags and 0x0001 == 0) {
            speedKmh = reader.u16()?.div(100.0) ?: return null
        }
        if (flags and 0x0002 != 0 && !reader.skip(2)) return null
        if (flags and 0x0004 != 0) {
            distanceMeters = reader.u24() ?: return null
        }
        if (flags and 0x0008 != 0) {
            inclinePercent = reader.s16()?.div(10.0) ?: return null
            if (!reader.skip(2)) return null
        }
        if (flags and 0x0010 != 0 && !reader.skip(4)) return null
        if (flags and 0x0020 != 0 && !reader.skip(1)) return null
        if (flags and 0x0040 != 0 && !reader.skip(1)) return null
        if (flags and 0x0080 != 0 && !reader.skip(5)) return null
        if (flags and 0x0100 != 0) {
            heartRateBpm = reader.u8() ?: return null
        }
        if (flags and 0x0200 != 0 && !reader.skip(1)) return null
        if (flags and 0x0400 != 0 && !reader.skip(2)) return null
        if (flags and 0x0800 != 0 && !reader.skip(2)) return null
        if (flags and 0x1000 != 0) {
            if (!reader.skip(2)) return null
            powerWatts = reader.s16() ?: return null
        }
        if (flags and 0x2000 != 0 && !reader.skip(3)) return null

        if (speedKmh == null && distanceMeters == null && inclinePercent == null &&
            powerWatts == null && heartRateBpm == null
        ) {
            return null
        }
        return FtmsTreadmillSample(
            speedKmh = speedKmh,
            totalDistanceMeters = distanceMeters,
            inclinePercent = inclinePercent,
            powerWatts = powerWatts,
            heartRateBpm = heartRateBpm,
        )
    }
}
