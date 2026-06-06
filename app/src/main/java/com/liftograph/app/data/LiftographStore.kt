package com.liftograph.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LiftographStore(context: Context) {
    private val preferences = context.getSharedPreferences("liftograph_store", Context.MODE_PRIVATE)

    fun load(): LiftographState {
        val raw = preferences.getString(KEY_STATE, null) ?: return LiftographState()
        return runCatching { decodeState(JSONObject(raw)) }.getOrElse { LiftographState() }
    }

    fun save(state: LiftographState) {
        preferences.edit().putString(KEY_STATE, encodeState(state).toString()).apply()
    }

    private fun encodeState(state: LiftographState): JSONObject = JSONObject()
        .put("trainingDays", JSONArray().also { array ->
            state.trainingDays.forEach { day ->
                array.put(JSONObject().put("id", day.id).put("name", day.name))
            }
        })
        .put("exercises", JSONArray().also { array ->
            state.exercises.forEach { exercise ->
                array.put(
                    JSONObject()
                        .put("id", exercise.id)
                        .put("name", exercise.name)
                        .put("trainingDayId", exercise.trainingDayId)
                )
            }
        })
        .put("entries", JSONArray().also { array ->
            state.entries.forEach { entry ->
                array.put(
                    JSONObject()
                        .put("id", entry.id)
                        .put("exerciseId", entry.exerciseId)
                        .put("dateEpochMillis", entry.dateEpochMillis)
                        .put("weight", entry.weight)
                        .put("reps", entry.reps)
                        .put("notes", entry.notes)
                        .put("phase", entry.phase.name)
                        .put("trainingDayId", entry.trainingDayId)
                )
            }
        })
        .put("selectedTrainingDayId", state.selectedTrainingDayId)
        .put("selectedExerciseId", state.selectedExerciseId)
        .put("currentPhase", state.currentPhase.name)
        .put(
            "apiConnectorSettings",
            JSONObject()
                .put("developerMode", state.apiSettings.developerMode)
                .put("enabled", state.apiSettings.enabled)
                .put("endpoint", state.apiSettings.endpoint)
                .put("apiKey", state.apiSettings.apiKey)
        )
        .put(
            "uiSettings",
            JSONObject()
                .put("darkMode", state.uiSettings.darkMode)
                .put("graphMetric", state.uiSettings.graphMetric.name)
        )

    private fun decodeState(json: JSONObject): LiftographState {
        val trainingDays = json.optJSONArray("trainingDays").toTrainingDayList().ifEmpty { DefaultTrainingDays }
        val selectedTrainingDayId = json.optString("selectedTrainingDayId")
            .takeIf { id -> trainingDays.any { it.id == id } }
            ?: json.optString("currentRoutineDay").legacyRoutineDayId()
                .takeIf { id -> trainingDays.any { it.id == id } }
            ?: trainingDays.first().id
        val exercises = json.optJSONArray("exercises").toExerciseList(selectedTrainingDayId)
        val entries = json.optJSONArray("entries").toEntryList(selectedTrainingDayId)
        val selectedExerciseId = json.optString(
            "selectedExerciseId",
            exercises.firstOrNull()?.id ?: "bench_press"
        ).takeIf { id -> exercises.any { it.id == id } }
            ?: exercises.firstOrNull { it.trainingDayId == selectedTrainingDayId }?.id
            ?: exercises.firstOrNull()?.id
            ?: ""
        val settings = json.optJSONObject("apiConnectorSettings") ?: json.optJSONObject("rmsSettings")
        val uiSettings = json.optJSONObject("uiSettings")

        return LiftographState(
            trainingDays = trainingDays,
            selectedTrainingDayId = selectedTrainingDayId,
            exercises = exercises.ifEmpty { LiftographState().exercises },
            entries = entries,
            selectedExerciseId = selectedExerciseId,
            currentPhase = json.optString("currentPhase", TrainingPhase.Maintain.name).toPhase(),
            apiSettings = ApiConnectorSettings(
                developerMode = settings?.optBoolean("developerMode") ?: false,
                enabled = settings?.optBoolean("enabled") ?: false,
                endpoint = settings?.optString("endpoint").orEmpty(),
                apiKey = settings?.optString("apiKey").orEmpty()
            ),
            uiSettings = UiSettings(
                darkMode = uiSettings?.optBoolean("darkMode") ?: false,
                graphMetric = uiSettings?.optString("graphMetric", GraphMetric.EstimatedOneRepMax.name)
                    .orEmpty()
                    .toGraphMetric()
            )
        )
    }

    private fun JSONArray?.toTrainingDayList(): List<TrainingDay> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            TrainingDay(
                id = item.optString("id"),
                name = item.optString("name")
            )
        }.filter { it.id.isNotBlank() && it.name.isNotBlank() }
    }

    private fun JSONArray?.toExerciseList(defaultTrainingDayId: String): List<Exercise> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            Exercise(
                id = item.optString("id"),
                name = item.optString("name"),
                trainingDayId = item.optString("trainingDayId", defaultTrainingDayId)
            )
        }.filter { it.id.isNotBlank() && it.name.isNotBlank() && it.trainingDayId.isNotBlank() }
    }

    private fun JSONArray?.toEntryList(defaultTrainingDayId: String): List<LiftEntry> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            val trainingDayId = item.optString("trainingDayId")
                .ifBlank { item.optString("routineDay").legacyRoutineDayId() }
                .ifBlank { defaultTrainingDayId }
            LiftEntry(
                id = item.optString("id"),
                exerciseId = item.optString("exerciseId"),
                dateEpochMillis = item.optLong("dateEpochMillis"),
                weight = item.optDouble("weight"),
                reps = item.optInt("reps"),
                notes = item.optString("notes"),
                phase = item.optString("phase", TrainingPhase.Maintain.name).toPhase(),
                trainingDayId = trainingDayId
            )
        }.filter { it.id.isNotBlank() && it.exerciseId.isNotBlank() && it.weight > 0.0 && it.trainingDayId.isNotBlank() }
    }

    private fun String.toPhase(): TrainingPhase =
        TrainingPhase.entries.firstOrNull { it.name == this } ?: TrainingPhase.Maintain

    private fun String.toGraphMetric(): GraphMetric =
        GraphMetric.entries.firstOrNull { it.name == this } ?: GraphMetric.EstimatedOneRepMax

    private fun String.legacyRoutineDayId(): String = when (this) {
        "Push", "Push Day" -> "push_day"
        "Pull", "Pull Day" -> "pull_day"
        "Legs", "Leg", "Leg Day" -> "leg_day"
        "Upper", "Upper Day" -> "upper_day"
        "Lower", "Lower Day" -> "lower_day"
        "FullBody", "Full Body" -> "full_body"
        "Cardio" -> "cardio"
        else -> ""
    }

    private companion object {
        const val KEY_STATE = "state_json"
    }
}
