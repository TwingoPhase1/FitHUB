package com.twingo.fithub.core.ble

/**
 * Lecteur little-endian avec contrôle de bornes. Toute lecture hors trame
 * retourne null plutôt que de lever une exception.
 */
internal class BleByteReader(private val data: ByteArray) {

    var offset: Int = 0
        private set

    val remaining: Int
        get() = data.size - offset

    fun u8(): Int? {
        if (remaining < 1) return null
        return data[offset++].toInt() and 0xFF
    }

    fun u16(): Int? {
        if (remaining < 2) return null
        val value = (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)
        offset += 2
        return value
    }

    fun s16(): Int? {
        val raw = u16() ?: return null
        return if (raw >= 0x8000) raw - 0x1_0000 else raw
    }

    fun u24(): Int? {
        if (remaining < 3) return null
        val value = (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16)
        offset += 3
        return value
    }

    fun skip(count: Int): Boolean {
        if (count < 0 || remaining < count) return false
        offset += count
        return true
    }
}
