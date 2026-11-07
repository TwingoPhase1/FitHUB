package com.twingo.fithub.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

import com.twingo.fithub.data.dao.ProfileDao
import com.twingo.fithub.data.dao.WorkoutDao
import com.twingo.fithub.data.entity.ProfileEntity
import com.twingo.fithub.data.entity.WorkoutPlanEntity

@Database(
    entities = [ProfileEntity::class, WorkoutPlanEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class FitHubDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val DATABASE_NAME = "fithub.db"

        @Volatile
        private var instance: FitHubDatabase? = null

        fun getInstance(context: Context): FitHubDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { database ->
                    instance = database
                }
            }

        private fun buildDatabase(context: Context): FitHubDatabase =
            Room.databaseBuilder(context, FitHubDatabase::class.java, DATABASE_NAME)
                .addCallback(SeedDatabaseCallback())
                .build()
    }
}

/**
 * Pré-remplit la base au tout premier lancement. Le callback écrit directement
 * dans SQLite car les DAO ne sont pas encore disponibles pendant la création
 * de la base.
 */
private class SeedDatabaseCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        SeedData.profiles().forEach { profile ->
            db.insert(
                TABLE_PROFILES,
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put(COLUMN_ID, profile.id)
                    put(COLUMN_NAME, profile.name)
                    put(COLUMN_WEIGHT_KG, profile.weightKg)
                    put(COLUMN_MAX_HEART_RATE, profile.maxHeartRate)
                    put(COLUMN_RESTING_HEART_RATE, profile.restingHeartRate)
                    put(COLUMN_IS_GUEST, if (profile.isGuest) 1 else 0)
                },
            )
        }

        SeedData.weeklyPlans().forEach { plan ->
            db.insert(
                TABLE_WORKOUT_PLANS,
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put(COLUMN_PROFILE_ID, plan.profileId)
                    put(COLUMN_DAY_OF_WEEK, plan.dayOfWeek)
                    put(COLUMN_TITLE, plan.title)
                    put(COLUMN_DESCRIPTION, plan.description)
                    put(COLUMN_STEPS_JSON, plan.stepsJson)
                    put(COLUMN_MACHINE, plan.machine)
                },
            )
        }
    }

    private companion object {
        const val TABLE_PROFILES = "profiles"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_WEIGHT_KG = "weight_kg"
        const val COLUMN_MAX_HEART_RATE = "max_heart_rate"
        const val COLUMN_RESTING_HEART_RATE = "resting_heart_rate"
        const val COLUMN_IS_GUEST = "is_guest"

        const val TABLE_WORKOUT_PLANS = "workout_plans"
        const val COLUMN_PROFILE_ID = "profile_id"
        const val COLUMN_DAY_OF_WEEK = "day_of_week"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_STEPS_JSON = "steps_json"
        const val COLUMN_MACHINE = "machine"
    }
}