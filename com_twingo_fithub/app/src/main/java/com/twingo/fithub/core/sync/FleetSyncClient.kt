package com.twingo.fithub.core.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.twingo.fithub.data.network.FleetApiService
import com.twingo.fithub.data.repository.FitnessRepository
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FleetSyncClient(
    private val context: Context,
    private val repository: FitnessRepository
) {

    private val TAG = "FleetSyncClient"
    private val prefs: SharedPreferences = context.getSharedPreferences("fleet_prefs", Context.MODE_PRIVATE)

    private var apiService: FleetApiService? = null
    private var serverUrl: String = "http://192.168.1.50:8080"

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Last sync time
    private val _lastSyncTime = MutableStateFlow<Long>(0)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    init {
        loadServerUrl()
    }

    private fun loadServerUrl() {
        serverUrl = prefs.getString("server_url", "http://192.168.1.50:8080")!!
        apiService = FleetApiService(serverUrl)
    }

    // Save server URL
    suspend fun setServerUrl(url: String) {
        prefs.edit().putString("server_url", url).apply()
        serverUrl = url
        apiService = FleetApiService(url)
    }

    // Get current server URL
    fun getServerUrl(): String = serverUrl

    // Register this tablet with the server
    suspend fun registerTablet(): Boolean {
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        return try {
            val success = apiService?.registerTablet(deviceId, "FitHub Tablet") ?: false
            if (success) {
                _connectionState.value = ConnectionState.Connected
            } else {
                _connectionState.value = ConnectionState.Error("Registration failed")
            }
            success
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
            false
        }
    }

    // Pull sync - fetch all profiles and plans from server
    suspend fun pullSync(): SyncResult {
        val result = apiService?.pullSync()
        if (result != null) {
            // Upsert profiles
            result.profiles.forEach { profile ->
                val p = profile.copy(userId = 1)
                try {
                    repository.insertProfile(p)
                } catch (e: Exception) {
                    // Profile might already exist
                }
            }

            // Upsert workout plans
            result.plans.forEach { plan ->
                repository.insertWorkout(plan)
            }

            _lastSyncTime.value = System.currentTimeMillis()
        }
        return SyncResult(
            profilesCount = result?.profiles?.size ?: 0,
            plansCount = result?.plans?.size ?: 0
        )
    }

    // Called when a new workout is saved locally - trigger sync
    fun onWorkoutSaved() {
        // In a real implementation, this would push to server
    }
}

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Connected : ConnectionState()
    object Disconnected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class SyncResult(
    val profilesCount: Int = 0,
    val plansCount: Int = 0
)