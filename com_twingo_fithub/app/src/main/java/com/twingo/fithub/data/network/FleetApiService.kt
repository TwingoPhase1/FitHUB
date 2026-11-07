package com.twingo.fithub.data.network

import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class FleetSyncResponse(
    val profiles: List<Profile> = emptyList(),
    val plans: List<WorkoutPlan> = emptyList()
)

class FleetApiService(private val baseUrl: String) {

    private val client = java.net.HttpURLConnection::class.java
    private val json = Json { ignoreUnknownKeys = true }

    private fun openConnection(path: String, method: String = "GET", body: String? = null): HttpURLConnection {
        val url = URL("$baseUrl$path")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 10000
        conn.readTimeout = 30000
        conn.doOutput = body != null
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        if (body != null) {
            conn.outputStream.use { it.write(body.toByteArray()) }
        }
        return conn
    }

    @Serializable
    data class RegisterRequest(
        val device_id: String,
        val name: String
    )

    suspend fun registerTablet(deviceId: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val body = json.encodeToString(RegisterRequest.serializer(), RegisterRequest(deviceId, name))
        val conn = openConnection("/api/fleet/register", "POST", body)
        try {
            conn.responseCode == java.net.HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }

    suspend fun pullSync(): FleetSyncResponse? = withContext(Dispatchers.IO) {
        val conn = openConnection("/api/fleet/sync")
        try {
            if (conn.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val jsonString = conn.inputStream.bufferedReader().readText()
                json.decodeFromString<FleetSyncResponse>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}