package com.twingo.fithub.domain.model

import kotlinx.serialization.Serializable

/**
 * Profil local utilisé pour personnaliser les cibles cardiaques.
 *
 * Les zones sont calculées avec la formule de Karvonen :
 * FC cible = FC repos + (FC max - FC repos) × intensité.
 */
@Serializable
data class Profile(
    val id: Long,
    val userId: Long = 1,
    val name: String,
    val weightKg: Double,
    val maxHeartRate: Int,
    val restingHeartRate: Int,
    val isGuest: Boolean,
)
