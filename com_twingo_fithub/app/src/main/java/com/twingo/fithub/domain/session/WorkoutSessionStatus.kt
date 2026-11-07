package com.twingo.fithub.domain.session

/**
 * États de la machine à états d'une séance.
 *
 * Transitions autorisées :
 * - [IDLE] -> [RUNNING] au démarrage ;
 * - [RUNNING] <-> [PAUSED] via pause/reprise ;
 * - [RUNNING] ou [PAUSED] -> [COMPLETED] à la fin du dernier bloc ou lors d'un skip ;
 * - [RUNNING] ou [PAUSED] -> [IDLE] lors d'un arrêt confirmé.
 */
enum class WorkoutSessionStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
}
