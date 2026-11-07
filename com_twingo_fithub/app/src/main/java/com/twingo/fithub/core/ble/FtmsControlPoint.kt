package com.twingo.fithub.core.ble

import kotlin.math.roundToInt

/**
 * Encodeur/Décodeur pour le FTMS Control Point (0x2AD9).
 *
 * Toutes les commandes sont écrites sur la caractéristique 0x2AD9.
 * Les réponses arrivent comme des INDICATIONS sur la même caractéristique.
 *
 * Référence : Bluetooth SIG FTMS 1.0, §4.13
 */
object FtmsControlPoint {

    enum class OpCode(val value: Byte) {
        REQUEST_CONTROL(0x00.toByte()),
        RESET(0x01.toByte()),
        SET_TARGET_SPEED(0x02.toByte()),
        SET_TARGET_INCLINE(0x03.toByte()),
        SET_TARGET_RESISTANCE(0x04.toByte()),
        SET_TARGET_POWER(0x05.toByte()),
        SET_TARGET_HEART_RATE(0x06.toByte()),
        START_OR_RESUME(0x07.toByte()),
        PAUSE(0x08.toByte()),
        SET_TARGETED_EXPENDED_ENERGY(0x09.toByte()),
        SET_TARGETED_STEPS(0x0A.toByte()),
        SET_TARGETED_STRIDES(0x0B.toByte()),
        SET_TARGETED_DISTANCE(0x0C.toByte()),
        SET_TARGETED_CADENCE(0x0D.toByte()),
        INDICATE_CONTROL_POINT(0x80.toByte()),
    }

    enum class ResponseCode(val value: Byte) {
        SUCCESS(0x01.toByte()),
        NOT_SUPPORTED(0x02.toByte()),
        INVALID_PARAMETER(0x03.toByte()),
        OPERATION_FAILED(0x04.toByte()),
        CONTROL_NOT_PERMITTED(0x05.toByte()),
    }

    /** Structure d'une réponse INDICATE_CONTROL_POINT (0x80). */
    data class ControlPointResponse(
        val requestOpCode: OpCode,
        val result: ResponseCode,
    )

    /**
     * Décode une indication reçue sur 0x2AD9.
     * Format : [0x80, requestOpCode, responseCode]
     */
    fun parseResponse(data: ByteArray): ControlPointResponse? {
        if (data.size < 3) return null
        if (data[0] != OpCode.INDICATE_CONTROL_POINT.value) return null

        val requestOpCode = OpCode.values().firstOrNull { it.value == data[1] }
        val result = ResponseCode.values().firstOrNull { it.value == data[2] }
        return if (requestOpCode != null && result != null) {
            ControlPointResponse(requestOpCode, result)
        } else null
    }

    /** Encode la commande Request Control (0x00). */
    fun encodeRequestControl(): ByteArray = byteArrayOf(OpCode.REQUEST_CONTROL.value)

    /** Encode la commande Reset (0x01). */
    fun encodeReset(): ByteArray = byteArrayOf(OpCode.RESET.value)

    /** Encode Start or Resume (0x07). */
    fun encodeStartOrResume(): ByteArray = byteArrayOf(OpCode.START_OR_RESUME.value)

    /** Encode Pause (0x08). */
    fun encodePause(): ByteArray = byteArrayOf(OpCode.PAUSE.value)

    /**
     * Encode Set Target Power (0x05) : watts en sint16 Little-Endian.
     * Mode ERG pour home-trainer (Elite Suito-T).
     */
    fun encodeSetTargetPower(watts: Int): ByteArray {
        val clamped = watts.coerceIn(Int.MIN_VALUE, Int.MAX_VALUE)
        return byteArrayOf(
            OpCode.SET_TARGET_POWER.value,
            (clamped and 0xFF).toByte(),
            ((clamped shr 8) and 0xFF).toByte(),
        )
    }

    /**
     * Encode Set Target Resistance (0x04) : niveau en uint8.
     */
    fun encodeSetTargetResistance(level: Int): ByteArray {
        val clamped = level.coerceIn(0, 255)
        return byteArrayOf(
            OpCode.SET_TARGET_RESISTANCE.value,
            clamped.toByte(),
        )
    }

    /**
     * Encode Set Target Incline (0x03) : pourcentage × 10 en sint16 LE.
     * Ex: 5,5 % → 55 → 0x37 0x00
     */
    fun encodeSetTargetIncline(percent: Float): ByteArray {
        val raw = (percent * 10.0f).roundToInt().coerceIn(-32768, 32767)
        return byteArrayOf(
            OpCode.SET_TARGET_INCLINE.value,
            (raw and 0xFF).toByte(),
            ((raw shr 8) and 0xFF).toByte(),
        )
    }

    /** Encode Set Target Speed (0x02) : vitesse en km/h × 100 en uint16 LE. */
    fun encodeSetTargetSpeed(kmh: Float): ByteArray {
        val raw = (kmh * 100.0f).roundToInt().coerceIn(0, 65535)
        return byteArrayOf(
            OpCode.SET_TARGET_SPEED.value,
            (raw and 0xFF).toByte(),
            ((raw shr 8) and 0xFF).toByte(),
        )
    }
}