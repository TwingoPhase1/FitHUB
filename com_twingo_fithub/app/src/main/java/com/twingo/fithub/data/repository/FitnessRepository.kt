package com.twingo.fithub.data.repository

import com.twingo.fithub.data.dao.ProfileDao
import com.twingo.fithub.data.dao.WorkoutDao
import com.twingo.fithub.data.mapper.toDomain
import com.twingo.fithub.data.mapper.toEntity
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.data.entity.WorkoutPlanEntity
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FitnessRepository(
    private val profileDao: ProfileDao,
    private val workoutDao: WorkoutDao,
) {

    fun observeProfiles(): Flow<List<Profile>> =
        profileDao.observeProfiles().map { profiles -> profiles.map { it.toDomain() } }

    fun observeWorkoutForDay(profileId: Long, dayOfWeek: DayOfWeek): Flow<WorkoutPlan?> =
        workoutDao.observePlanForDay(profileId, dayOfWeek.value)
            .map { plan -> plan?.toDomain() }

    fun observeWeekPlan(profileId: Long): Flow<List<WorkoutPlan>> =
        workoutDao.observeWeekPlans(profileId)
            .map { plans -> plans.map { it.toDomain() } }

    suspend fun getProfile(profileId: Long): Profile? =
        profileDao.getProfile(profileId)?.toDomain()

    suspend fun getWorkout(planId: Long): WorkoutPlan? =
        workoutDao.getPlan(planId)?.toDomain()

    // Profile mutations
    suspend fun insertProfile(profile: Profile): Long =
        profileDao.insert(profile.toEntity())

    suspend fun updateProfile(profile: Profile) =
        profileDao.update(profile.toEntity())

    suspend fun deleteProfile(profile: Profile) =
        profileDao.delete(profile.toEntity())

    // Workout mutations
    suspend fun insertWorkout(plan: WorkoutPlan): Long =
        workoutDao.insert(plan.toEntity())

    suspend fun updateWorkout(plan: WorkoutPlan) =
        workoutDao.insert(plan.toEntity())

    suspend fun deleteWorkout(plan: WorkoutPlan) =
        workoutDao.delete(plan.toEntity())

    suspend fun deleteWorkoutById(planId: Long) =
        workoutDao.deleteById(planId)
}
