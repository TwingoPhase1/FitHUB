package com.twingo.fithub.core.ble

/**
 * Heart Rate Measurement (0x2A37).
 *
 * Le bit 0 du champ Flags indique la largeur de la mesure :
 * 0 = uint8, 1 = uint16 little-endian.
 */
object HeartRateParser {

    fun parse(data: ByteArray): Int? {
        if (data.size < 2) return null
        val flags = data[0].toInt() and 0xFF
        return if (flags and 0x01 == 0) {
            data[1].toInt() and 0xFF
        } else {
            if (data.size < 3) {
                null
            } else {
                (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            }
        }
    }
}
