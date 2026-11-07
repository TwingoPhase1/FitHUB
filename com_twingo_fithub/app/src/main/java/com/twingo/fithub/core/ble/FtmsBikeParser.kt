package com.twingo.fithub.core.ble

import kotlin.math.roundToInt

data class FtmsBikeSample(
    val speedKmh: Double?,
    val cadenceRpm: Int?,
    val powerWatts: Int?,
    val totalDistanceMeters: Int?,
)

/**
 * Indoor Bike Data (0x2AD2).
 *
 * Flags (uint16 LE) :
 * - bit 0 : More Data, logique inversée ; la vitesse instantanée est présente
 *   quand le bit vaut 0 ;
 * - bit 1 : vitesse moyenne ;
 * - bit 2 : cadence instantanée (résolution 0,5 tr/min) ;
 * - bit 3 : cadence moyenne ;
 * - bit 4 : distance totale (uint24) ;
 * - bit 5 : niveau de résistance (sint16) ;
 * - bit 6 : puissance instantanée (sint16, watts).
 */
object FtmsBikeParser {

    fun parse(data: ByteArray): FtmsBikeSample? {
        val reader = BleByteReader(data)
        val flags = reader.u16() ?: return null

        var speedKmh: Double? = null
        var cadenceRpm: Int? = null
        var powerWatts: Int? = null
        var totalDistanceMeters: Int? = null

        if (flags and 0x0001 == 0) {
            speedKmh = reader.u16()?.div(100.0) ?: return null
        }
        if (flags and 0x0002 != 0 && !reader.skip(2)) return null
        if (flags and 0x0004 != 0) {
            val raw = reader.u16() ?: return null
            cadenceRpm = (raw / 2.0).roundToInt()
        }
        if (flags and 0x0008 != 0 && !reader.skip(2)) return null
        if (flags and 0x0010 != 0) {
            totalDistanceMeters = reader.u24() ?: return null
        }
        if (flags and 0x0020 != 0 && !reader.skip(2)) return null
        if (flags and 0x0040 != 0) {
            powerWatts = reader.s16() ?: return null
        }

        if (speedKmh == null && cadenceRpm == null && powerWatts == null &&
            totalDistanceMeters == null
        ) {
            return null
        }
        return FtmsBikeSample(
            speedKmh = speedKmh,
            cadenceRpm = cadenceRpm,
            powerWatts = powerWatts,
            totalDistanceMeters = totalDistanceMeters,
        )
    }
}
