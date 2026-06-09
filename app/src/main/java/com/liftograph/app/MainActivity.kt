package com.liftograph.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.liftograph.app.data.ApiConnectorSettings
import com.liftograph.app.data.Exercise
import com.liftograph.app.data.GraphMetric
import com.liftograph.app.data.LiftEntry
import com.liftograph.app.data.LiftographState
import com.liftograph.app.data.LiftographStore
import com.liftograph.app.data.TrainingDay
import com.liftograph.app.data.TrainingPhase
import com.liftograph.app.data.UiSettings
import com.liftograph.app.data.parseLiftInput
import com.liftograph.app.data.parseWorkoutInput
import com.liftograph.app.data.summarizeWorkoutSets
import com.liftograph.app.integration.AiApiClient
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiftographApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun LiftographApp() {
    val context = LocalContext.current
    val store = remember { LiftographStore(context) }
    var state by remember { mutableStateOf(store.load()) }
    var showSettings by remember { mutableStateOf(false) }

    fun commit(next: LiftographState) {
        state = next
        store.save(next)
    }

    val selectedExercise = state.selectedExerciseForTrainingDay()
    val selectedDayExercises = state.exercises.filter { it.trainingDayId == state.selectedTrainingDayId }

    val activity = context as? Activity
    SideEffect {
        activity?.window?.let { window ->
            val barColor = if (state.uiSettings.darkMode) Color(0xFF313338) else Color(0xFFF4F6F4)
            window.statusBarColor = barColor.toArgb()
            window.navigationBarColor = barColor.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !state.uiSettings.darkMode
                isAppearanceLightNavigationBars = !state.uiSettings.darkMode
            }
        }
    }

    MaterialTheme(
        colorScheme = liftographColors(darkMode = state.uiSettings.darkMode)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Lift-O-Graph", fontWeight = FontWeight.Black)
                            Text(
                                "Scientific training log",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (showSettings) {
                        item {
                            SettingsPanel(
                                state = state,
                                onStateChanged = ::commit
                            )
                        }
                    }

                    item {
                        TrainingDayPicker(
                            trainingDays = state.trainingDays,
                            selectedTrainingDayId = state.selectedTrainingDayId,
                            onSelected = { dayId -> commit(state.withSelectedTrainingDay(dayId)) },
                            onRemove = { dayId ->
                                if (state.trainingDays.size > 1) {
                                    val remainingDays = state.trainingDays.filterNot { it.id == dayId }
                                    val nextTrainingDayId = if (state.selectedTrainingDayId == dayId) {
                                        remainingDays.firstOrNull()?.id.orEmpty()
                                    } else {
                                        state.selectedTrainingDayId
                                    }
                                    val remainingExercises = state.exercises.filterNot { it.trainingDayId == dayId }
                                    val nextExerciseId = remainingExercises
                                        .firstOrNull { it.trainingDayId == nextTrainingDayId }
                                        ?.id
                                        .orEmpty()
                                    commit(
                                        state.copy(
                                            trainingDays = remainingDays,
                                            selectedTrainingDayId = nextTrainingDayId,
                                            exercises = remainingExercises,
                                            entries = state.entries.filterNot { it.trainingDayId == dayId },
                                            selectedExerciseId = nextExerciseId
                                        )
                                    )
                                }
                            },
                            onAdd = { name ->
                                val day = TrainingDay(id = newId("training_day"), name = name.trim().toTitleCase())
                                commit(
                                    state.copy(
                                        trainingDays = state.trainingDays + day,
                                        selectedTrainingDayId = day.id,
                                        selectedExerciseId = ""
                                    )
                                )
                            }
                        )
                    }

                    item {
                        ExercisePicker(
                            exercises = selectedDayExercises,
                            selectedExerciseId = selectedExercise?.id.orEmpty(),
                            onSelected = { id -> commit(state.copy(selectedExerciseId = id)) },
                            onRemove = { exerciseId ->
                                val remainingExercises = state.exercises.filterNot { it.id == exerciseId }
                                val nextExerciseId = remainingExercises
                                    .firstOrNull { it.trainingDayId == state.selectedTrainingDayId }
                                    ?.id
                                    .orEmpty()
                                commit(
                                    state.copy(
                                        exercises = remainingExercises,
                                        entries = state.entries.filterNot { it.exerciseId == exerciseId },
                                        selectedExerciseId = nextExerciseId
                                    )
                                )
                            },
                            onAdd = { name ->
                                val exercise = Exercise(
                                    id = newId("exercise"),
                                    name = name.trim(),
                                    trainingDayId = state.selectedTrainingDayId
                                )
                                commit(
                                    state.copy(
                                        exercises = state.exercises + exercise,
                                        selectedExerciseId = exercise.id
                                    )
                                )
                            }
                        )
                    }

                    item {
                        LiftGraph(
                            entries = state.entries
                                .filter { it.exerciseId == selectedExercise?.id }
                                .sortedBy { it.dateEpochMillis },
                            currentPhase = state.currentPhase,
                            metric = state.uiSettings.graphMetric,
                            onMetricChanged = { metric ->
                                commit(
                                    state.copy(
                                        uiSettings = state.uiSettings.copy(graphMetric = metric)
                                    )
                                )
                            }
                        )
                    }

                    item {
                        LogLiftPanel(
                            state = state,
                            onStateChanged = ::commit
                        )
                    }

                    item {
                        AllLogsPanel(
                            entries = state.entries.sortedByDescending { it.dateEpochMillis },
                            exercises = state.exercises,
                            trainingDays = state.trainingDays,
                            onDelete = { entryId ->
                                commit(state.copy(entries = state.entries.filterNot { it.id == entryId }))
                            }
                        )
                    }

                    if (state.apiSettings.developerMode) {
                        item {
                            DeveloperPanel(
                                state = state,
                                onStateChanged = ::commit
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingDayPicker(
    trainingDays: List<TrainingDay>,
    selectedTrainingDayId: String,
    onSelected: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var newTrainingDay by remember { mutableStateOf("") }
    val selectedDay = trainingDays.firstOrNull { it.id == selectedTrainingDayId } ?: trainingDays.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Training Day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedDay?.name.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Training Day") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    trainingDays.forEach { day ->
                        DropdownMenuItem(
                            text = { Text(day.name) },
                            onClick = {
                                onSelected(day.id)
                                expanded = false
                            },
                            trailingIcon = {
                                IconButton(
                                    enabled = trainingDays.size > 1,
                                    onClick = {
                                        onRemove(day.id)
                                        expanded = false
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Remove,
                                        contentDescription = "Delete ${day.name}",
                                        tint = if (trainingDays.size > 1) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        if (adding) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTrainingDay,
                    onValueChange = { newTrainingDay = it },
                    label = { Text("Day name") },
                    placeholder = { Text("Push Day") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    newTrainingDay = ""
                    adding = false
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel training day")
                }
                FilledIconButton(
                    onClick = {
                        if (newTrainingDay.isNotBlank()) {
                            onAdd(newTrainingDay)
                            newTrainingDay = ""
                            adding = false
                        }
                    }
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save training day")
                }
            }
        } else {
            OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add training day")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisePicker(
    exercises: List<Exercise>,
    selectedExerciseId: String,
    onSelected: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: (String) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var newExercise by remember { mutableStateOf("") }
    val selectedExercise = exercises.firstOrNull { it.id == selectedExerciseId }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Exercises", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    enabled = selectedExercise != null,
                    onClick = { selectedExercise?.let { onRemove(it.id) } },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Delete selected exercise",
                        tint = if (selectedExercise != null) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                FilledIconButton(onClick = { adding = true }, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Add exercise")
                }
            }
        }

        if (exercises.isEmpty()) {
            Text(
                "No exercises on this training day yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercises.forEach { exercise ->
                    FilterChip(
                        selected = exercise.id == selectedExerciseId,
                        onClick = { onSelected(exercise.id) },
                        label = {
                            Text(
                                exercise.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }

        if (adding) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newExercise,
                    onValueChange = { newExercise = it },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    newExercise = ""
                    adding = false
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel exercise")
                }
                FilledIconButton(
                    onClick = {
                        if (newExercise.isNotBlank()) {
                            onAdd(newExercise)
                            newExercise = ""
                            adding = false
                        }
                    }
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save exercise")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiftGraph(
    entries: List<LiftEntry>,
    currentPhase: TrainingPhase,
    metric: GraphMetric,
    onMetricChanged: (GraphMetric) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text(currentPhase.label) })
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GraphMetric.entries.forEach { option ->
                    FilterChip(
                        selected = option == metric,
                        onClick = { onMetricChanged(option) },
                        label = { Text(option.label) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                if (entries.isEmpty()) {
                    Text(
                        "Log a lift to start the graph.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LiftCanvas(entries = entries, metric = metric)
                }
            }

            Text(
                graphMetricHint(metric),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            PhaseLegend()
        }
    }
}

@Composable
private fun LiftCanvas(entries: List<LiftEntry>, metric: GraphMetric) {
    val axisColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val pointCoreColor = MaterialTheme.colorScheme.surface
    val values = entries.map { metric.valueFor(it) }
    val minValue = (values.minOrNull() ?: 0.0).coerceAtLeast(0.0)
    val maxValue = (values.maxOrNull() ?: minValue + 1.0).let {
        if (it == minValue) it + 10.0 else it
    }
    val formatter = DateTimeFormatter.ofPattern("M/d", Locale.US).withZone(ZoneId.systemDefault())

    Canvas(modifier = Modifier.fillMaxSize()) {
        val left = 44f
        val right = size.width - 16f
        val top = 18f
        val bottom = size.height - 34f
        val chartWidth = right - left
        val chartHeight = bottom - top
        val labelPaint = android.graphics.Paint().apply {
            color = labelColor
            textSize = 28f
            isAntiAlias = true
        }

        repeat(4) { step ->
            val y = top + chartHeight * step / 3f
            drawLine(
                color = axisColor.copy(alpha = 0.32f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.5f
            )
            val value = maxValue - (maxValue - minValue) * step / 3f
            drawContext.canvas.nativeCanvas.drawText(
                metric.formatAxisValue(value),
                0f,
                y + 9f,
                labelPaint
            )
        }

        fun point(index: Int, entry: LiftEntry): Offset {
            val x = if (entries.size == 1) {
                left + chartWidth / 2f
            } else {
                left + chartWidth * index / (entries.lastIndex).toFloat()
            }
            val y = bottom - (((metric.valueFor(entry) - minValue) / (maxValue - minValue)).toFloat() * chartHeight)
            return Offset(x, y)
        }

        entries.zipWithNext().forEachIndexed { index, pair ->
            val start = point(index, pair.first)
            val end = point(index + 1, pair.second)
            val path = Path().apply {
                moveTo(start.x, start.y)
                lineTo(end.x, end.y)
            }
            drawPath(
                path = path,
                color = pair.second.phase.phaseColor(),
                style = Stroke(width = 7f, cap = StrokeCap.Round)
            )
        }

        entries.forEachIndexed { index, entry ->
            val point = point(index, entry)
            drawCircle(color = entry.phase.phaseColor(), radius = 8f, center = point)
            drawCircle(color = pointCoreColor, radius = 3.5f, center = point)
        }

        val firstDate = formatter.format(Instant.ofEpochMilli(entries.first().dateEpochMillis))
        val lastDate = formatter.format(Instant.ofEpochMilli(entries.last().dateEpochMillis))
        drawContext.canvas.nativeCanvas.drawText(firstDate, left, size.height - 6f, labelPaint)
        drawContext.canvas.nativeCanvas.drawText(lastDate, right - 64f, size.height - 6f, labelPaint)
    }
}

@Composable
private fun LogLiftPanel(
    state: LiftographState,
    onStateChanged: (LiftographState) -> Unit
) {
    val scope = rememberCoroutineScope()
    val client = remember { AiApiClient() }
    var transcript by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val selectedExercise = state.selectedExerciseForTrainingDay()
    val selectedExerciseName = selectedExercise?.name ?: "Exercise"

    fun syncNewEntries(nextState: LiftographState, entries: List<LiftEntry>, exercises: List<Exercise>) {
        if (!nextState.apiSettings.enabled || entries.isEmpty()) return

        scope.launch {
            val failures = entries.mapNotNull { entry ->
                val exercise = exercises.firstOrNull { it.id == entry.exerciseId }
                val trainingDay = nextState.trainingDays.firstOrNull { it.id == entry.trainingDayId }
                if (exercise == null || trainingDay == null) {
                    "Missing exercise or training day"
                } else {
                    client.sendLiftEntry(nextState.apiSettings, exercise, trainingDay, entry)
                        .exceptionOrNull()
                        ?.message
                }
            }
            message = if (failures.isEmpty()) {
                "Logged and synced."
            } else {
                "Logged, but sync failed: ${failures.first()}"
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            transcript = spoken
            val workoutSets = parseWorkoutInput(spoken, selectedExerciseName)
            val firstSet = workoutSets.firstOrNull()
            weight = firstSet?.weight?.cleanString().orEmpty()
            reps = firstSet?.reps?.toString().orEmpty()
            notes = spoken
            message = if (workoutSets.size > 1) {
                "Parsed ${workoutSets.size} sets: ${summarizeWorkoutSets(workoutSets)}"
            } else {
                "Voice captured."
            }
        }
    }

    fun parseText() {
        val workoutSets = parseWorkoutInput(transcript, selectedExerciseName)
        val firstSet = workoutSets.firstOrNull()
        weight = firstSet?.weight?.cleanString().orEmpty()
        reps = firstSet?.reps?.toString().orEmpty()
        notes = transcript
        message = if (workoutSets.isEmpty()) {
            "I could not find sets yet. Try `bench press 145 for 6 and 145 for 8`."
        } else {
            "Parsed ${workoutSets.size} set${if (workoutSets.size == 1) "" else "s"}: ${summarizeWorkoutSets(workoutSets)}"
        }
    }

    fun saveEntry() {
        val parsed = parseLiftInput(transcript)
        val parsedExercise = parsed.exerciseName
        val exercise = parsedExercise
            ?.let { name ->
                state.exercises.find {
                    it.trainingDayId == state.selectedTrainingDayId && it.name.equals(name, ignoreCase = true)
                } ?: Exercise(
                    id = newId("exercise"),
                    name = name.toTitleCase(),
                    trainingDayId = state.selectedTrainingDayId
                )
            }
            ?: selectedExercise
        if (exercise == null) {
            message = "Add an exercise for this training day first."
            return
        }
        val exerciseList = if (state.exercises.any { it.id == exercise.id }) state.exercises else state.exercises + exercise
        val entry = LiftEntry(
            id = newId("entry"),
            exerciseId = exercise.id,
            dateEpochMillis = System.currentTimeMillis(),
            weight = weight.toDoubleOrNull() ?: parsed.weight ?: 0.0,
            reps = reps.toIntOrNull() ?: parsed.reps ?: 1,
            notes = notes.ifBlank { transcript },
            phase = state.currentPhase,
            trainingDayId = state.selectedTrainingDayId
        )

        if (entry.weight <= 0.0) {
            message = "Add a weight first."
            return
        }

        val nextState = state.copy(
            exercises = exerciseList,
            entries = state.entries + entry,
            selectedExerciseId = exercise.id
        )
        onStateChanged(nextState)
        syncNewEntries(nextState, listOf(entry), exerciseList)
        transcript = ""
        weight = ""
        reps = ""
        notes = ""
        message = "Lift logged."
    }

    fun saveWorkout() {
        val workoutSets = parseWorkoutInput(transcript, selectedExerciseName)
        if (workoutSets.isEmpty()) {
            saveEntry()
            return
        }

        val exercises = state.exercises.toMutableList()
        val entries = mutableListOf<LiftEntry>()
        val baseTime = System.currentTimeMillis()
        var firstExerciseId = state.selectedExerciseId

        workoutSets.forEachIndexed { index, set ->
            val exercise = exercises.find {
                it.trainingDayId == state.selectedTrainingDayId && it.name.equals(set.exerciseName, ignoreCase = true)
            } ?: Exercise(
                id = newId("exercise"),
                name = set.exerciseName.toTitleCase(),
                trainingDayId = state.selectedTrainingDayId
            ).also { exercises.add(it) }
            if (index == 0) {
                firstExerciseId = exercise.id
            }
            entries.add(
                LiftEntry(
                    id = newId("entry"),
                    exerciseId = exercise.id,
                    dateEpochMillis = baseTime + index,
                    weight = set.weight,
                    reps = set.reps,
                    notes = notes.ifBlank { transcript },
                    phase = state.currentPhase,
                    trainingDayId = state.selectedTrainingDayId
                )
            )
        }

        val nextState = state.copy(
            exercises = exercises,
            entries = state.entries + entries,
            selectedExerciseId = firstExerciseId
        )
        onStateChanged(nextState)
        syncNewEntries(nextState, entries, exercises)

        transcript = ""
        weight = ""
        reps = ""
        notes = ""
        message = "Logged ${entries.size} set${if (entries.size == 1) "" else "s"}: ${summarizeWorkoutSets(workoutSets)}"
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Log Workout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = transcript,
                onValueChange = { transcript = it },
                label = { Text("Workout text or voice input") },
                placeholder = { Text("Bench press 145 for 6 and 145 for 8, curls 30 for 12") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the lift, weight, and reps")
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    }
                    runCatching { speechLauncher.launch(intent) }
                        .onFailure { message = "Speech input is not available on this device." }
                }) {
                    Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Voice")
                }
                OutlinedButton(onClick = ::parseText) {
                    Text("Parse")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            PhaseSelector(
                selected = state.currentPhase,
                onSelected = { onStateChanged(state.copy(currentPhase = it)) }
            )

            Button(onClick = ::saveEntry, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log set")
            }

            OutlinedButton(onClick = ::saveWorkout, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log full workout")
            }

            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhaseSelector(selected: TrainingPhase, onSelected: (TrainingPhase) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Phase", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainingPhase.entries.forEach { phase ->
                FilterChip(
                    selected = phase == selected,
                    onClick = { onSelected(phase) },
                    label = { Text(phase.label) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(phase.phaseColor(), CircleShape)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    state: LiftographState,
    onStateChanged: (LiftographState) -> Unit
) {
    fun updateUiSettings(transform: (UiSettings) -> UiSettings) {
        onStateChanged(state.copy(uiSettings = transform(state.uiSettings)))
    }

    fun updateApiSettings(transform: (ApiConnectorSettings) -> ApiConnectorSettings) {
        onStateChanged(state.copy(apiSettings = transform(state.apiSettings)))
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Appearance and advanced tools", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dark mode", fontWeight = FontWeight.SemiBold)
                    Text("Discord-style dark background", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.uiSettings.darkMode,
                    onCheckedChange = { enabled ->
                        updateUiSettings { it.copy(darkMode = enabled) }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("API connector", fontWeight = FontWeight.SemiBold)
                    Text("Optional endpoint for your own AI or backend", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.apiSettings.developerMode,
                    onCheckedChange = { enabled ->
                        updateApiSettings {
                            it.copy(
                                developerMode = enabled,
                                enabled = if (enabled) it.enabled else false
                            )
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    onStateChanged(
                        state.copy(
                            apiSettings = ApiConnectorSettings(),
                            uiSettings = UiSettings()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset settings to default")
            }

            OutlinedButton(
                onClick = { onStateChanged(LiftographState()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset app to default")
            }
        }
    }
}

@Composable
private fun DeveloperPanel(
    state: LiftographState,
    onStateChanged: (LiftographState) -> Unit
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    val client = remember { AiApiClient() }
    val selectedExercise = state.selectedExerciseForTrainingDay()
    val latestEntry = state.entries
        .filter { it.exerciseId == selectedExercise?.id }
        .maxByOrNull { it.dateEpochMillis }
    val latestExercise = state.exercises.firstOrNull { it.id == latestEntry?.exerciseId }
    val latestTrainingDay = state.trainingDays.firstOrNull { it.id == latestEntry?.trainingDayId }

    fun updateSettings(transform: (ApiConnectorSettings) -> ApiConnectorSettings) {
        onStateChanged(state.copy(apiSettings = transform(state.apiSettings)))
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("API Connector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Send lift events to your own AI or backend", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedTextField(
                value = state.apiSettings.endpoint,
                onValueChange = { endpoint -> updateSettings { it.copy(endpoint = endpoint) } },
                label = { Text("API endpoint") },
                placeholder = { Text("http://10.0.2.2:8765") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.apiSettings.apiKey,
                onValueChange = { apiKey -> updateSettings { it.copy(apiKey = apiKey) } },
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-send lifts")
                Switch(
                    checked = state.apiSettings.enabled,
                    onCheckedChange = { enabled -> updateSettings { it.copy(enabled = enabled) } }
                )
            }

            OutlinedButton(
                onClick = {
                    val entry = latestEntry
                    val exercise = latestExercise
                    val trainingDay = latestTrainingDay
                    if (entry == null || exercise == null || trainingDay == null) {
                        status = "Log a lift before syncing."
                        return@OutlinedButton
                    }
                    scope.launch {
                        status = "Sending latest lift..."
                        status = client.sendLiftEntry(state.apiSettings, exercise, trainingDay, entry)
                            .fold(
                                onSuccess = { "API connector accepted the lift." },
                                onFailure = { "API connector failed: ${it.message}" }
                            )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Send latest lift")
            }

            if (status.isNotBlank()) {
                Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AllLogsPanel(
    entries: List<LiftEntry>,
    exercises: List<Exercise>,
    trainingDays: List<TrainingDay>,
    onDelete: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text(entries.size.toString()) })
            }

            if (entries.isEmpty()) {
                Text(
                    "No logs yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.forEach { entry ->
                    val exerciseName = exercises.firstOrNull { it.id == entry.exerciseId }?.name ?: "Unknown Exercise"
                    val trainingDayName = trainingDays.firstOrNull { it.id == entry.trainingDayId }?.name ?: "Training Day"
                    LiftEntryRow(
                        entry = entry,
                        exerciseName = exerciseName,
                        trainingDayName = trainingDayName,
                        onDelete = { onDelete(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiftEntryRow(
    entry: LiftEntry,
    exerciseName: String,
    trainingDayName: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exerciseName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${entry.weight.cleanString()} x ${entry.reps}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "$trainingDayName | ${entry.phase.label}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                formatDate(entry.dateEpochMillis),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Est. 1RM ${GraphMetric.EstimatedOneRepMax.valueFor(entry).cleanString()} | Volume ${GraphMetric.Volume.valueFor(entry).cleanString()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            if (entry.notes.isNotBlank()) {
                Text(entry.notes, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete log")
        }
    }
}

@Composable
private fun PhaseLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TrainingPhase.entries.forEach { phase ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(phase.phaseColor(), CircleShape)
                )
                Text(phase.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun TrainingPhase.phaseColor(): Color = when (this) {
    TrainingPhase.Bulk -> Color(0xFF3BA55D)
    TrainingPhase.Maintain -> Color(0xFF4FA3FF)
    TrainingPhase.Cut -> Color(0xFFE58B6B)
}

private fun GraphMetric.valueFor(entry: LiftEntry): Double = when (this) {
    GraphMetric.Weight -> entry.weight
    GraphMetric.Reps -> entry.reps.toDouble()
    GraphMetric.EstimatedOneRepMax -> entry.weight * (1.0 + entry.reps / 30.0)
    GraphMetric.Volume -> entry.weight * entry.reps
}

private fun GraphMetric.formatAxisValue(value: Double): String = when (this) {
    GraphMetric.Reps -> value.toInt().toString()
    GraphMetric.Volume -> {
        if (value >= 1000.0) {
            String.format(Locale.US, "%.1fk", value / 1000.0)
        } else {
            value.toInt().toString()
        }
    }
    GraphMetric.Weight,
    GraphMetric.EstimatedOneRepMax -> value.toInt().toString()
}

private fun graphMetricHint(metric: GraphMetric): String = when (metric) {
    GraphMetric.Weight -> "Weight shows load moved for each set."
    GraphMetric.Reps -> "Reps shows how many reps you completed at any weight."
    GraphMetric.EstimatedOneRepMax -> "Est. 1RM combines weight and reps into one strength trend."
    GraphMetric.Volume -> "Volume is weight x reps for each set."
}

private fun LiftographState.selectedExerciseForTrainingDay(): Exercise? =
    exercises.firstOrNull { it.id == selectedExerciseId && it.trainingDayId == selectedTrainingDayId }
        ?: exercises.firstOrNull { it.trainingDayId == selectedTrainingDayId }

private fun LiftographState.withSelectedTrainingDay(trainingDayId: String): LiftographState {
    val nextExerciseId = exercises.firstOrNull { it.trainingDayId == trainingDayId }?.id.orEmpty()
    return copy(
        selectedTrainingDayId = trainingDayId,
        selectedExerciseId = nextExerciseId
    )
}

private fun liftographColors(darkMode: Boolean) =
    if (darkMode) {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF5865F2),
            onPrimary = Color.White,
            secondary = Color(0xFF3BA55D),
            tertiary = Color(0xFFE58B6B),
            background = Color(0xFF313338),
            onBackground = Color(0xFFF2F3F5),
            surface = Color(0xFF2B2D31),
            onSurface = Color(0xFFF2F3F5),
            surfaceVariant = Color(0xFF1E1F22),
            onSurfaceVariant = Color(0xFFB5BAC1),
            outline = Color(0xFF6D6F78),
            outlineVariant = Color(0xFF404249)
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF2E6F9E),
            onPrimary = Color.White,
            secondary = Color(0xFF2F7D5B),
            tertiary = Color(0xFFC56A4B),
            background = Color(0xFFF4F6F4),
            onBackground = Color(0xFF1E1F1C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1E1F1C),
            surfaceVariant = Color(0xFFE7EBE6),
            onSurfaceVariant = Color(0xFF5C5D57),
            outline = Color(0xFF8B8C84),
            outlineVariant = Color(0xFFD2D8D2)
        )
    }

private fun newId(prefix: String): String = "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}"

private fun Double.cleanString(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)

private fun String.toTitleCase(): String =
    lowercase(Locale.US).split(Regex("""\s+""")).joinToString(" ") { word ->
        word.replaceFirstChar { char -> char.titlecase(Locale.US) }
    }

private fun formatDate(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))
