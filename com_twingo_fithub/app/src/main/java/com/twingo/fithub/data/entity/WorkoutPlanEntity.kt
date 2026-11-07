package com.twingo.fithub.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["profile_id", "day_of_week"], unique = true),
    ],
)
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    /**
     * Jour ISO : 1 = lundi, 7 = dimanche.
     */
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,
    val title: String,
    val description: String,
    @ColumnInfo(name = "steps_json")
    val stepsJson: String,
    val machine: String = "OUTDOOR",
)
