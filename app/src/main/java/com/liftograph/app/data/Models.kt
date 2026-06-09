package com.liftograph.app.data

enum class TrainingPhase(val label: String) {
    Bulk("Bulk"),
    Maintain("Maintain"),
    Cut("Cut")
}

enum class GraphMetric(val label: String) {
    Weight("Weight"),
    Reps("Reps"),
    EstimatedOneRepMax("Est. 1RM"),
    Volume("Volume")
}

data class TrainingDay(
    val id: String,
    val name: String
)

val DefaultTrainingDays = listOf(
    TrainingDay(id = "push_day", name = "Push Day"),
    TrainingDay(id = "pull_day", name = "Pull Day"),
    TrainingDay(id = "leg_day", name = "Leg Day"),
    TrainingDay(id = "upper_day", name = "Upper Day"),
    TrainingDay(id = "lower_day", name = "Lower Day"),
    TrainingDay(id = "full_body", name = "Full Body"),
    TrainingDay(id = "cardio", name = "Cardio")
)

data class Exercise(
    val id: String,
    val name: String,
    val trainingDayId: String = DefaultTrainingDays.first().id
)

data class LiftEntry(
    val id: String,
    val exerciseId: String,
    val dateEpochMillis: Long,
    val weight: Double,
    val reps: Int,
    val notes: String,
    val phase: TrainingPhase,
    val trainingDayId: String = DefaultTrainingDays.first().id
)

data class ApiConnectorSettings(
    val developerMode: Boolean = false,
    val enabled: Boolean = false,
    val endpoint: String = "",
    val apiKey: String = ""
)

data class UiSettings(
    val darkMode: Boolean = false,
    val graphMetric: GraphMetric = GraphMetric.EstimatedOneRepMax
)

data class LiftographState(
    val trainingDays: List<TrainingDay> = DefaultTrainingDays,
    val selectedTrainingDayId: String = DefaultTrainingDays.first().id,
    val exercises: List<Exercise> = listOf(
        Exercise(id = "bench_press", name = "Bench Press", trainingDayId = DefaultTrainingDays.first().id)
    ),
    val entries: List<LiftEntry> = emptyList(),
    val selectedExerciseId: String = "bench_press",
    val currentPhase: TrainingPhase = TrainingPhase.Maintain,
    val apiSettings: ApiConnectorSettings = ApiConnectorSettings(),
    val uiSettings: UiSettings = UiSettings()
)

fun defaultLiftographState(): LiftographState = LiftographState(
    trainingDays = DefaultTrainingDays.map { it.copy() },
    selectedTrainingDayId = DefaultTrainingDays.first().id,
    exercises = listOf(
        Exercise(id = "bench_press", name = "Bench Press", trainingDayId = DefaultTrainingDays.first().id)
    ),
    entries = emptyList(),
    selectedExerciseId = "bench_press",
    currentPhase = TrainingPhase.Maintain,
    apiSettings = ApiConnectorSettings(),
    uiSettings = UiSettings()
)

fun LiftographState.withDefaultTrainingDaysPreservingLogs(): LiftographState {
    val defaultDayIds = DefaultTrainingDays.map { it.id }.toSet()
    val fallbackTrainingDayId = DefaultTrainingDays.first().id
    fun normalizedTrainingDayId(trainingDayId: String): String =
        trainingDayId.takeIf { it in defaultDayIds } ?: fallbackTrainingDayId

    val nextExercises = exercises
        .map { exercise -> exercise.copy(trainingDayId = normalizedTrainingDayId(exercise.trainingDayId)) }
        .ifEmpty { defaultLiftographState().exercises }
    val nextSelectedExerciseId = nextExercises
        .firstOrNull { it.trainingDayId == fallbackTrainingDayId }
        ?.id
        ?: nextExercises.firstOrNull()?.id.orEmpty()

    return copy(
        trainingDays = DefaultTrainingDays.map { it.copy() },
        selectedTrainingDayId = fallbackTrainingDayId,
        exercises = nextExercises,
        entries = entries.map { entry -> entry.copy(trainingDayId = normalizedTrainingDayId(entry.trainingDayId)) },
        selectedExerciseId = nextSelectedExerciseId
    )
}
