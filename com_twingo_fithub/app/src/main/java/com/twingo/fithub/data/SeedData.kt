package com.twingo.fithub.data

import com.twingo.fithub.data.entity.ProfileEntity
import com.twingo.fithub.data.entity.WorkoutPlanEntity
import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.WorkoutMachine
import com.twingo.fithub.domain.model.WorkoutStep
import com.twingo.fithub.domain.model.WorkoutStepKind
import java.time.DayOfWeek

/**
 * Données de démonstration insérées une seule fois, à la création de la base.
 * Les deux profils partagent le même programme hebdomadaire.
 */
internal object SeedData {

    const val YANIS_PROFILE_ID = 1L
    const val GUEST_PROFILE_ID = 2L

    private const val MINUTE = 60

    fun profiles(): List<ProfileEntity> = listOf(
        ProfileEntity(
            id = YANIS_PROFILE_ID,
            name = "Yanis",
            weightKg = 75.0,
            maxHeartRate = 190,
            restingHeartRate = 55,
            isGuest = false,
        ),
        ProfileEntity(
            id = GUEST_PROFILE_ID,
            name = "Invité",
            weightKg = 70.0,
            maxHeartRate = 185,
            restingHeartRate = 60,
            isGuest = true,
        ),
    )

    fun weeklyPlans(): List<WorkoutPlanEntity> =
        profiles().flatMap { profile -> plansForProfile(profile.id) }

    fun plansForProfile(profileId: Long): List<WorkoutPlanEntity> = listOf(
        WorkoutPlanEntity(
            profileId = profileId,
            dayOfWeek = DayOfWeek.MONDAY.value,
            title = "Marche inclinée sur tapis",
            description = "35 min de marche en côte pour construire l'endurance de base sans impact articulaire.",
            stepsJson = serializeSteps(mondaySteps()),
            machine = WorkoutMachine.TREADMILL.name,
        ),
        WorkoutPlanEntity(
            profileId = profileId,
            dayOfWeek = DayOfWeek.WEDNESDAY.value,
            title = "Home-trainer · accélérations courtes",
            description = "30 min de vélo avec 8 accélérations de 20 s et 40 s de récupération.",
            stepsJson = serializeSteps(wednesdaySteps()),
            machine = WorkoutMachine.BIKE.name,
        ),
        WorkoutPlanEntity(
            profileId = profileId,
            dayOfWeek = DayOfWeek.FRIDAY.value,
            title = "Rameur endurance",
            description = "25 min de rameur à allure régulière pour travailler le gainage et l'endurance.",
            stepsJson = serializeSteps(fridaySteps()),
            machine = WorkoutMachine.ROWER.name,
        ),
        WorkoutPlanEntity(
            profileId = profileId,
            dayOfWeek = DayOfWeek.SUNDAY.value,
            title = "Sortie longue facile",
            description = "40 min à allure conversationnelle pour développer la capacité aérobie.",
            stepsJson = serializeSteps(sundaySteps()),
            machine = WorkoutMachine.OUTDOOR.name,
        ),
    )

    private fun mondaySteps(): List<WorkoutStep> = listOf(
        WorkoutStep(
            title = "Échauffement progressif",
            kind = WorkoutStepKind.WARM_UP,
            durationSeconds = 5 * MINUTE,
            zoneFrom = HeartRateZone.Z1,
            zoneTo = HeartRateZone.Z2,
            targetDetail = "Tapis · 3 à 5 % · 4,5 km/h",
            instruction = "Marche souple, épaules relâchées. Monte progressivement l'inclinaison et la vitesse.",
            targetInclinePercent = 4.0f,
            targetSpeedKmh = 4.5f,
        ),
        WorkoutStep(
            title = "Marche inclinée continue",
            kind = WorkoutStepKind.ENDURANCE,
            durationSeconds = 25 * MINUTE,
            zoneFrom = HeartRateZone.Z2,
            targetDetail = "Tapis · 7 % · 5,5 km/h",
            instruction = "Garde le buste droit sans t'accrocher aux poignées. Respiration régulière, foulée active.",
            targetInclinePercent = 7.0f,
            targetSpeedKmh = 5.5f,
        ),
        WorkoutStep(
            title = "Retour au calme",
            kind = WorkoutStepKind.COOL_DOWN,
            durationSeconds = 5 * MINUTE,
            zoneFrom = HeartRateZone.Z1,
            targetDetail = "Tapis · 0 à 2 % · 4 km/h",
            instruction = "Réduis l'inclinaison puis la vitesse. Termine par quelques étirements des mollets.",
            targetInclinePercent = 1.0f,
            targetSpeedKmh = 4.0f,
        ),
    )

    private fun wednesdaySteps(): List<WorkoutStep> = listOf(
        WorkoutStep(
            title = "Échauffement",
            kind = WorkoutStepKind.WARM_UP,
            durationSeconds = 12 * MINUTE,
            zoneFrom = HeartRateZone.Z2,
            targetDetail = "Home-trainer · 80 à 90 tr/min",
            instruction = "Pédalage fluide, résistance légère. Prépare les jambes sans monter dans les tours.",
            targetPowerWatts = 100,
            targetCadenceRpm = 85,
        ),
        WorkoutStep(
            title = "Accélérations courtes",
            kind = WorkoutStepKind.INTERVAL,
            durationSeconds = 20,
            repetitions = 8,
            recoverySeconds = 40,
            zoneFrom = HeartRateZone.Z4,
            zoneTo = HeartRateZone.Z5,
            targetDetail = "Home-trainer · plus de 100 tr/min · braquet léger",
            instruction = "Accélération explosive en restant assis. Récupère complètement pendant les 40 s.",
            targetPowerWatts = 250,
            targetCadenceRpm = 105,
        ),
        WorkoutStep(
            title = "Retour au calme",
            kind = WorkoutStepKind.COOL_DOWN,
            durationSeconds = 10 * MINUTE,
            zoneFrom = HeartRateZone.Z1,
            targetDetail = "Home-trainer · 70 à 80 tr/min",
            instruction = "Récupération active, hydratation et respiration calme.",
            targetPowerWatts = 80,
            targetCadenceRpm = 75,
        ),
    )

    private fun fridaySteps(): List<WorkoutStep> = listOf(
        WorkoutStep(
            title = "Échauffement",
            kind = WorkoutStepKind.WARM_UP,
            durationSeconds = 5 * MINUTE,
            zoneFrom = HeartRateZone.Z1,
            zoneTo = HeartRateZone.Z2,
            targetDetail = "Rameur · 18 à 20 coups/min",
            instruction = "Rame lentement en décomposant le mouvement : jambes, bassin, bras.",
            targetCadenceRpm = 19,
        ),
        WorkoutStep(
            title = "Endurance continue",
            kind = WorkoutStepKind.ENDURANCE,
            durationSeconds = 15 * MINUTE,
            zoneFrom = HeartRateZone.Z3,
            targetDetail = "Rameur · 24 à 26 coups/min · résistance 4",
            instruction = "Garde un rythme régulier et un dos gainé. Ne bloque pas la respiration.",
            targetCadenceRpm = 25,
            targetResistanceLevel = 4,
        ),
        WorkoutStep(
            title = "Retour au calme",
            kind = WorkoutStepKind.COOL_DOWN,
            durationSeconds = 5 * MINUTE,
            zoneFrom = HeartRateZone.Z1,
            targetDetail = "Rameur · 16 à 18 coups/min",
            instruction = "Allonge le coup de rame et laisse redescendre la fréquence cardiaque.",
            targetCadenceRpm = 17,
        ),
    )

    private fun sundaySteps(): List<WorkoutStep> = listOf(
        WorkoutStep(
            title = "Mise en route",
            kind = WorkoutStepKind.WARM_UP,
            durationSeconds = 5 * MINUTE,
            zoneFrom = HeartRateZone.Z1,
            targetDetail = "Extérieur ou tapis · allure très facile",
            instruction = "Démarre lentement, relâche les épaules et trouve une respiration régulière.",
            targetSpeedKmh = 6.0f,
        ),
        WorkoutStep(
            title = "Sortie longue facile",
            kind = WorkoutStepKind.ENDURANCE,
            durationSeconds = 30 * MINUTE,
            zoneFrom = HeartRateZone.Z1,
            zoneTo = HeartRateZone.Z2,
            targetDetail = "Allure conversationnelle · 1 à 3 % si tapis",
            instruction = "Tu dois pouvoir tenir une conversation. Garde cette allure sans chercher la performance.",
            targetSpeedKmh = 7.0f,
            targetInclinePercent = 2.0f,
        ),
        WorkoutStep(
            title = "Retour au calme",
            kind = WorkoutStepKind.COOL_DOWN,
            durationSeconds = 5 * MINUTE,
            zoneFrom = HeartRateZone.Z1,
            targetDetail = "Marche lente · respiration profonde",
            instruction = "Ralentis progressivement et termine par des étirements doux des jambes.",
            targetSpeedKmh = 4.0f,
        ),
    )
}

private fun serializeSteps(steps: List<com.twingo.fithub.domain.model.WorkoutStep>): String {
    val json = org.json.JSONArray()
    steps.forEach { step ->
        json.put(
            org.json.JSONObject().apply {
                put("title", step.title)
                put("kind", step.kind.name)
                put("durationSeconds", step.durationSeconds)
                put("repetitions", step.repetitions)
                put("recoverySeconds", step.recoverySeconds)
                put("zoneFrom", step.zoneFrom.name)
                put("zoneTo", step.zoneTo.name)
                put("targetDetail", step.targetDetail)
                put("instruction", step.instruction)
                put("targetPowerWatts", step.targetPowerWatts ?: 0)
                put("targetResistanceLevel", step.targetResistanceLevel ?: 0)
                put("targetInclinePercent", step.targetInclinePercent ?: 0f)
                put("targetCadenceRpm", step.targetCadenceRpm ?: 0)
                put("targetSpeedKmh", step.targetSpeedKmh ?: 0f)
            },
        )
    }
    return json.toString()
}