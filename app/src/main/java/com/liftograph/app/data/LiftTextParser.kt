package com.liftograph.app.data

import java.util.Locale

data class ParsedLiftInput(
    val exerciseName: String?,
    val weight: Double?,
    val reps: Int?,
    val notes: String
)

data class ParsedWorkoutSet(
    val exerciseName: String,
    val weight: Double,
    val reps: Int
)

fun parseLiftInput(input: String): ParsedLiftInput {
    val normalized = input.trim()
    if (normalized.isBlank()) {
        return ParsedLiftInput(exerciseName = null, weight = null, reps = null, notes = "")
    }

    val numberMatches = Regex("""\d+(?:\.\d+)?""").findAll(normalized).toList()
    val firstNumber = numberMatches.getOrNull(0)
    val secondNumber = numberMatches.getOrNull(1)
    val rawName = firstNumber
        ?.let { normalized.substring(0, it.range.first) }
        ?: normalized

    val exerciseName = rawName
        .replace(Regex("""\b(log|add|did|for|at|lbs?|pounds?|kg|kilos?)\b""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .takeIf { it.isNotBlank() }

    return ParsedLiftInput(
        exerciseName = exerciseName,
        weight = firstNumber?.value?.toDoubleOrNull(),
        reps = secondNumber?.value?.toDoubleOrNull()?.toInt(),
        notes = normalized
    )
}

fun parseWorkoutInput(input: String, fallbackExerciseName: String): List<ParsedWorkoutSet> {
    val normalized = input.trim()
    if (normalized.isBlank()) return emptyList()

    val setPattern = Regex(
        pattern = """(\d+(?:\.\d+)?)\s*(?:lb|lbs|pounds|kg|kilos)?\s*(?:x|for)\s*(\d+)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )
    val matches = setPattern.findAll(normalized).toList()
    if (matches.isEmpty()) {
        val single = parseLiftInput(normalized)
        val weight = single.weight ?: return emptyList()
        val reps = single.reps ?: return emptyList()
        return listOf(
            ParsedWorkoutSet(
                exerciseName = single.exerciseName?.toTitleCase() ?: fallbackExerciseName,
                weight = weight,
                reps = reps
            )
        )
    }

    var cursor = 0
    var currentExercise = fallbackExerciseName
    return matches.mapNotNull { match ->
        val prefix = normalized.substring(cursor, match.range.first).cleanExerciseName()
        if (prefix.isNotBlank()) {
            currentExercise = prefix.toTitleCase()
        }
        cursor = match.range.last + 1

        val weight = match.groupValues[1].toDoubleOrNull()
        val reps = match.groupValues[2].toIntOrNull()
        if (weight == null || reps == null || weight <= 0.0 || reps <= 0) {
            null
        } else {
            ParsedWorkoutSet(
                exerciseName = currentExercise,
                weight = weight,
                reps = reps
            )
        }
    }
}

fun summarizeWorkoutSets(sets: List<ParsedWorkoutSet>): String =
    sets
        .groupBy { it.exerciseName }
        .entries
        .joinToString("; ") { (exercise, exerciseSets) ->
            val setSummary = exerciseSets.joinToString(", ") { "${it.weight.cleanString()} x ${it.reps}" }
            "$exercise: $setSummary"
        }

private fun String.cleanExerciseName(): String =
    replace(Regex("""[,.;:]+"""), " ")
        .replace(
            Regex(
                """\b(log|add|did|i|then|and|also|next|set|sets|of|for|at|with|lb|lbs|pounds|kg|kilos|reps?)\b""",
                RegexOption.IGNORE_CASE
            ),
            " "
        )
        .replace(Regex("""\s+"""), " ")
        .trim()

private fun String.toTitleCase(): String =
    lowercase(Locale.US).split(Regex("""\s+""")).joinToString(" ") { word ->
        word.replaceFirstChar { char -> char.titlecase(Locale.US) }
    }

private fun Double.cleanString(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)
