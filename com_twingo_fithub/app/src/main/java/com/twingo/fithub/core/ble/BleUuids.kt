package com.twingo.fithub.core.ble

import java.util.Locale
import java.util.UUID

/**
 * UUID 16 bits normalisés vers le format Bluetooth SIG complet.
 */
object BleUuids {
    val HEART_RATE_SERVICE: UUID = uuid16(0x180D)
    val HEART_RATE_MEASUREMENT: UUID = uuid16(0x2A37)

    val FTMS_SERVICE: UUID = uuid16(0x1826)
    val TREADMILL_DATA: UUID = uuid16(0x2ACD)
    val ROWER_DATA: UUID = uuid16(0x2AD1)
    val INDOOR_BIKE_DATA: UUID = uuid16(0x2AD2)
    val FTMS_CONTROL_POINT: UUID = uuid16(0x2AD9)

    val CLIENT_CHARACTERISTIC_CONFIG: UUID = uuid16(0x2902)

    private fun uuid16(value: Int): UUID = UUID.fromString(
        String.format(Locale.ROOT, "0000%04x-0000-1000-8000-00805f9b34fb", value),
    )
}
