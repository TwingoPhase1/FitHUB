package com.twingo.fithub.data.mapper

import com.twingo.fithub.data.entity.ProfileEntity
import com.twingo.fithub.data.entity.WorkoutPlanEntity
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutMachine
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.domain.model.WorkoutStep
import com.twingo.fithub.domain.model.WorkoutStepKind
import com.twingo.fithub.domain.model.HeartRateZone
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

fun ProfileEntity.toDomain(): Profile = Profile(
    id = id,
    userId = userId,
    name = name,
    weightKg = weightKg,
    maxHeartRate = maxHeartRate,
    restingHeartRate = restingHeartRate,
    isGuest = isGuest,
)

fun Profile.toEntity(): ProfileEntity = ProfileEntity(
    id = if (id == 0L) 0L else id,
    userId = userId,
    name = name,
    weightKg = weightKg,
    maxHeartRate = maxHeartRate,
    restingHeartRate = restingHeartRate,
    isGuest = isGuest,
)

fun WorkoutPlanEntity.toDomain(): WorkoutPlan = WorkoutPlan(
    id = id,
    profileId = profileId,
    dayOfWeek = DayOfWeek.of(dayOfWeek.coerceIn(1, 7)),
    title = title,
    description = description,
    steps = parseSteps(stepsJson),
    machine = WorkoutMachine.values().firstOrNull { it.name == machine } ?: WorkoutMachine.OUTDOOR,
)

fun WorkoutPlan.toEntity(): WorkoutPlanEntity = WorkoutPlanEntity(
    id = if (id == 0L) 0L else id,
    profileId = profileId,
    dayOfWeek = dayOfWeek.value,
    title = title,
    description = description,
    stepsJson = serializeSteps(steps),
    machine = machine.name,
)

private fun serializeSteps(steps: List<com.twingo.fithub.domain.model.WorkoutStep>): String {
    val json = JSONArray()
    steps.forEach { step ->
        json.put(
            JSONObject().apply {
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

private fun parseSteps(json: String): List<com.twingo.fithub.domain.model.WorkoutStep> {
    if (json.isBlank()) return emptyList()
    val array = JSONArray(json)
    return buildList(array.length()) {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(
                com.twingo.fithub.domain.model.WorkoutStep(
                    title = item.getString("title"),
                    kind = com.twingo.fithub.domain.model.WorkoutStepKind.values().firstOrNull { it.name == item.optString("kind") } ?: com.twingo.fithub.domain.model.WorkoutStepKind.ENDURANCE,
                    durationSeconds = item.getInt("durationSeconds"),
                    repetitions = item.optInt("repetitions", 1).coerceAtLeast(1),
                    recoverySeconds = item.optInt("recoverySeconds", 0).coerceAtLeast(0),
                    zoneFrom = com.twingo.fithub.domain.model.HeartRateZone.values().firstOrNull { it.name == item.optString("zoneFrom") } ?: com.twingo.fithub.domain.model.HeartRateZone.Z2,
                    zoneTo = com.twingo.fithub.domain.model.HeartRateZone.values().firstOrNull { it.name == item.optString("zoneTo") } ?: com.twingo.fithub.domain.model.HeartRateZone.Z2,
                    targetDetail = item.optString("targetDetail"),
                    instruction = item.optString("instruction"),
                    targetPowerWatts = item.optInt("targetPowerWatts").takeIf { it != 0 },
                    targetResistanceLevel = item.optInt("targetResistanceLevel").takeIf { it != 0 },
                    targetInclinePercent = item.optDouble("targetInclinePercent").takeIf { it != 0.0 }?.toFloat(),
                    targetCadenceRpm = item.optInt("targetCadenceRpm").takeIf { it != 0 },
                    targetSpeedKmh = item.optDouble("targetSpeedKmh").takeIf { it != 0.0 }?.toFloat(),
                )
            )
        }
    }
}