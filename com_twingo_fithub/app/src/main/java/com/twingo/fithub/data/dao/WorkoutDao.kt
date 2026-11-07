package com.twingo.fithub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import com.twingo.fithub.data.converter.WorkoutStepListConverter
import com.twingo.fithub.data.entity.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
@TypeConverters(WorkoutStepListConverter::class)
interface WorkoutDao {

    @Query("SELECT * FROM workout_plans WHERE profile_id = :profileId ORDER BY day_of_week ASC")
    fun observeWeekPlans(profileId: Long): Flow<List<WorkoutPlanEntity>>

    @Query(
        """
        SELECT * FROM workout_plans
        WHERE profile_id = :profileId AND day_of_week = :dayOfWeek
        LIMIT 1
        """,
    )
    fun observePlanForDay(profileId: Long, dayOfWeek: Int): Flow<WorkoutPlanEntity?>

    @Query(
        """
        SELECT * FROM workout_plans
        WHERE profile_id = :profileId AND day_of_week = :dayOfWeek
        LIMIT 1
        """,
    )
    suspend fun getPlanForDay(profileId: Long, dayOfWeek: Int): WorkoutPlanEntity?

    @Query("SELECT * FROM workout_plans WHERE id = :planId LIMIT 1")
    suspend fun getPlan(planId: Long): WorkoutPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlans(plans: List<WorkoutPlanEntity>)

    @Query("DELETE FROM workout_plans WHERE profile_id = :profileId")
    suspend fun deletePlansForProfile(profileId: Long)

    @Delete
    suspend fun delete(plan: WorkoutPlanEntity)

    @Query("DELETE FROM workout_plans WHERE id = :planId")
    suspend fun deleteById(planId: Long)
}
