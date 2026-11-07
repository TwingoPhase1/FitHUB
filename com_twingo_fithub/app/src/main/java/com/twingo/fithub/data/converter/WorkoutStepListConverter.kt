package com.twingo.fithub.data.converter

import androidx.room.TypeConverter
import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.WorkoutStep
import com.twingo.fithub.domain.model.WorkoutStepKind
import org.json.JSONArray
import org.json.JSONObject

/**
 * TypeConverter for List<WorkoutStep> that serializes to JSON string.
 */
class WorkoutStepListConverter {

    @TypeConverter
    fun fromWorkoutSteps(steps: List<WorkoutStep>): String {
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

    @TypeConverter
    fun toWorkoutSteps(json: String): List<WorkoutStep> {
        if (json.isBlank()) return emptyList()

        val array = JSONArray(json)
        val result = mutableListOf<WorkoutStep>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            result.add(
                WorkoutStep(
                    title = item.getString("title"),
                    kind = WorkoutStepKind.values().firstOrNull { it.name == item.optString("kind") } ?: WorkoutStepKind.ENDURANCE,
                    durationSeconds = item.getInt("durationSeconds"),
                    repetitions = item.optInt("repetitions", 1).coerceAtLeast(1),
                    recoverySeconds = item.optInt("recoverySeconds", 0).coerceAtLeast(0),
                    zoneFrom = HeartRateZone.values().firstOrNull { it.name == item.optString("zoneFrom") } ?: HeartRateZone.Z2,
                    zoneTo = HeartRateZone.values().firstOrNull { it.name == item.optString("zoneTo") } ?: HeartRateZone.Z2,
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
        return result
    }
}