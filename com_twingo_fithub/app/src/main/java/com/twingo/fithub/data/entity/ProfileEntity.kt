package com.twingo.fithub.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "user_id")
    val userId: Long = 1,
    val name: String,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,
    @ColumnInfo(name = "max_heart_rate")
    val maxHeartRate: Int,
    @ColumnInfo(name = "resting_heart_rate")
    val restingHeartRate: Int,
    @ColumnInfo(name = "is_guest")
    val isGuest: Boolean = false,
)
