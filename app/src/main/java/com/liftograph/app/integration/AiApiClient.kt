package com.liftograph.app.integration

import com.liftograph.app.data.ApiConnectorSettings
import com.liftograph.app.data.Exercise
import com.liftograph.app.data.LiftEntry
import com.liftograph.app.data.TrainingDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiApiClient {
    suspend fun sendLiftEntry(
        settings: ApiConnectorSettings,
        exercise: Exercise,
        trainingDay: TrainingDay,
        entry: LiftEntry
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(settings.enabled) { "AI API connector is disabled." }
            require(settings.endpoint.isNotBlank()) { "AI API endpoint is empty." }

            val endpoint = settings.endpoint.trimEnd('/') + "/liftograph/events"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (settings.apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
                }
            }

            val payload = JSONObject()
                .put("type", "lift_entry.created")
                .put("source", "Lift-O-Graph Android")
                .put("exercise", JSONObject().put("id", exercise.id).put("name", exercise.name))
                .put("trainingDay", JSONObject().put("id", trainingDay.id).put("name", trainingDay.name))
                .put(
                    "entry",
                    JSONObject()
                        .put("id", entry.id)
                        .put("dateEpochMillis", entry.dateEpochMillis)
                        .put("weight", entry.weight)
                        .put("reps", entry.reps)
                        .put("phase", entry.phase.name)
                        .put("trainingDayId", entry.trainingDayId)
                        .put("trainingDayName", trainingDay.name)
                        .put("routineDay", trainingDay.name)
                        .put("notes", entry.notes)
                )

            connection.outputStream.use { stream ->
                stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                val message = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "HTTP $code"
                error(message)
            }
        }
    }
}
