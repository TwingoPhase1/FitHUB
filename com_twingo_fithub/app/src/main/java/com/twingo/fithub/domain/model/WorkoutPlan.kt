package com.twingo.fithub.domain.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
data class WorkoutPlan(
    val id: Long,
    val profileId: Long,
    val dayOfWeek: DayOfWeek,
    val title: String,
    val description: String,
    val steps: List<WorkoutStep>,
    val machine: WorkoutMachine = WorkoutMachine.OUTDOOR,
) {
    val totalDurationSeconds: Int
        get() = steps.sumOf(WorkoutStep::totalDurationSeconds)
}
