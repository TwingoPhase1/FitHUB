package com.twingo.fithub.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Zones d'effort de Z1 à Z5, exprimées en pourcentage de la réserve cardiaque
 * (Karvonen). Les bornes basses et hautes de chaque zone sont incluses.
 */
@Serializable
enum class HeartRateZone(
    val label: String,
    private val minIntensity: Double,
    private val maxIntensity: Double,
) {
    Z1(label = "Z1", minIntensity = 0.50, maxIntensity = 0.60),
    Z2(label = "Z2", minIntensity = 0.60, maxIntensity = 0.70),
    Z3(label = "Z3", minIntensity = 0.70, maxIntensity = 0.80),
    Z4(label = "Z4", minIntensity = 0.80, maxIntensity = 0.90),
    Z5(label = "Z5", minIntensity = 0.90, maxIntensity = 1.00),
    ;

    /**
     * Calcule la plage de battements par minute pour une zone ou une plage de zones.
     */
    fun bpmRangeFor(profile: Profile, to: HeartRateZone = this): IntRange {
        val heartRateReserve = (profile.maxHeartRate - profile.restingHeartRate)
            .coerceAtLeast(0)
            .toDouble()

        val minBpm = profile.restingHeartRate + (heartRateReserve * minIntensity).roundToInt()
        val maxBpm = profile.restingHeartRate + (heartRateReserve * to.maxIntensity).roundToInt()
        return minBpm..maxBpm
    }

    companion object {
        /**
         * Retourne la zone dynamique correspondant à un BPM mesuré, ou `null`
         * si la valeur est sous la première zone. Les valeurs au-dessus de Z5
         * restent classées en Z5.
         */
        fun fromBpm(profile: Profile, bpm: Int): HeartRateZone? {
            if (bpm <= 0) return null
            return entries.lastOrNull { zone -> bpm >= zone.bpmRangeFor(profile).first }
        }
    }
}