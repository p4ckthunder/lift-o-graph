"use strict";

const STORE_KEY = "liftograph_state_v1";

const phases = [
  { name: "Bulk", label: "Bulk", color: "#3ba55d" },
  { name: "Maintain", label: "Maintain", color: "#4fa3ff" },
  { name: "Cut", label: "Cut", color: "#e58b6b" }
];

const metrics = [
  { name: "Weight", label: "Weight", hint: "Weight shows load moved for each set." },
  { name: "Reps", label: "Reps", hint: "Reps shows how many reps you completed at any weight." },
  { name: "EstimatedOneRepMax", label: "Est. 1RM", hint: "Est. 1RM combines weight and reps into one strength trend." },
  { name: "Volume", label: "Volume", hint: "Volume is weight x reps for each set." }
];

const defaultTrainingDays = [
  { id: "push_day", name: "Push Day" },
  { id: "pull_day", name: "Pull Day" },
  { id: "leg_day", name: "Leg Day" },
  { id: "upper_day", name: "Upper Day" },
  { id: "lower_day", name: "Lower Day" },
  { id: "full_body", name: "Full Body" },
  { id: "cardio", name: "Cardio" }
];

const defaultState = {
  trainingDays: defaultTrainingDays,
  selectedTrainingDayId: "push_day",
  exercises: [{ id: "bench_press", name: "Bench Press", trainingDayId: "push_day" }],
  entries: [],
  selectedExerciseId: "bench_press",
  currentPhase: "Maintain",
  apiSettings: {
    developerMode: false,
    enabled: false,
    endpoint: "",
    apiKey: ""
  },
  uiSettings: {
    darkMode: false,
    graphMetric: "EstimatedOneRepMax"
  }
};

let state = loadState();
const els = {};

document.addEventListener("DOMContentLoaded", () => {
  bindElements();
  bindEvents();
  registerServiceWorker();
  render();
});

function bindElements() {
  [
    "settings-toggle",
    "settings-panel",
    "dark-mode",
    "developer-mode",
    "reset-settings",
    "settings-message",
    "training-day-select",
    "remove-training-day",
    "training-day-form",
    "training-day-input",
    "exercise-chips",
    "show-exercise-form",
    "exercise-form",
    "exercise-input",
    "cancel-exercise",
    "phase-pill",
    "metric-chips",
    "lift-chart",
    "empty-chart",
    "metric-hint",
    "phase-legend",
    "transcript",
    "voice-button",
    "parse-button",
    "weight-input",
    "reps-input",
    "notes-input",
    "phase-chips",
    "log-set-button",
    "log-workout-button",
    "message",
    "developer-panel",
    "api-endpoint",
    "api-key",
    "api-enabled",
    "send-latest",
    "api-status",
    "log-count",
    "logs-list"
  ].forEach((id) => {
    els[toCamel(id)] = document.getElementById(id);
  });
}

function bindEvents() {
  els.settingsToggle.addEventListener("click", () => {
    els.settingsPanel.classList.toggle("is-hidden");
  });

  els.darkMode.addEventListener("change", () => {
    commit({ uiSettings: { ...state.uiSettings, darkMode: els.darkMode.checked } });
  });

  els.developerMode.addEventListener("change", () => {
    commit({
      apiSettings: {
        ...state.apiSettings,
        developerMode: els.developerMode.checked,
        enabled: els.developerMode.checked ? state.apiSettings.enabled : false
      }
    });
  });

  els.resetSettings.addEventListener("click", () => {
    commit({
      apiSettings: { ...defaultState.apiSettings },
      uiSettings: { ...defaultState.uiSettings }
    });
    els.settingsMessage.textContent = "Settings reset to default.";
  });

  els.trainingDaySelect.addEventListener("change", () => {
    const trainingDayId = els.trainingDaySelect.value;
    const selectedExerciseId = state.exercises.find((exercise) => exercise.trainingDayId === trainingDayId)?.id ?? "";
    commit({ selectedTrainingDayId: trainingDayId, selectedExerciseId });
  });

  els.removeTrainingDay.addEventListener("click", () => {
    removeSelectedTrainingDay();
  });

  els.trainingDayForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const name = els.trainingDayInput.value.trim();
    if (!name) return;
    const day = { id: newId("training_day"), name: toTitleCase(name) };
    commit({
      trainingDays: [...state.trainingDays, day],
      selectedTrainingDayId: day.id,
      selectedExerciseId: ""
    });
    els.trainingDayInput.value = "";
  });

  els.showExerciseForm.addEventListener("click", () => {
    els.exerciseForm.classList.remove("is-hidden");
    els.exerciseInput.focus();
  });

  els.cancelExercise.addEventListener("click", () => {
    els.exerciseInput.value = "";
    els.exerciseForm.classList.add("is-hidden");
  });

  els.exerciseForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const name = els.exerciseInput.value.trim();
    if (!name) return;
    const exercise = {
      id: newId("exercise"),
      name,
      trainingDayId: state.selectedTrainingDayId
    };
    commit({
      exercises: [...state.exercises, exercise],
      selectedExerciseId: exercise.id
    });
    els.exerciseInput.value = "";
    els.exerciseForm.classList.add("is-hidden");
  });

  els.parseButton.addEventListener("click", parseTranscriptIntoFields);
  els.voiceButton.addEventListener("click", listenForWorkout);
  els.logSetButton.addEventListener("click", saveEntry);
  els.logWorkoutButton.addEventListener("click", saveWorkout);

  els.apiEndpoint.addEventListener("input", () => {
    commit({ apiSettings: { ...state.apiSettings, endpoint: els.apiEndpoint.value } }, false);
  });

  els.apiKey.addEventListener("input", () => {
    commit({ apiSettings: { ...state.apiSettings, apiKey: els.apiKey.value } }, false);
  });

  els.apiEnabled.addEventListener("change", () => {
    commit({ apiSettings: { ...state.apiSettings, enabled: els.apiEnabled.checked } });
  });

  els.sendLatest.addEventListener("click", sendLatestLift);

  window.addEventListener("resize", () => drawChart());
}

function commit(patch, shouldRender = true) {
  state = normalizeState({ ...state, ...patch });
  saveState(state);
  if (shouldRender) render();
}

function render() {
  document.body.classList.toggle("dark", state.uiSettings.darkMode);
  document.querySelector('meta[name="theme-color"]')?.setAttribute(
    "content",
    state.uiSettings.darkMode ? "#313338" : "#f4f6f4"
  );

  els.darkMode.checked = state.uiSettings.darkMode;
  els.developerMode.checked = state.apiSettings.developerMode;
  els.developerPanel.classList.toggle("is-hidden", !state.apiSettings.developerMode);
  els.apiEndpoint.value = state.apiSettings.endpoint;
  els.apiKey.value = state.apiSettings.apiKey;
  els.apiEnabled.checked = state.apiSettings.enabled;

  renderTrainingDays();
  renderExercises();
  renderMetrics();
  renderPhases();
  renderLogs();
  drawChart();
}

function renderTrainingDays() {
  els.trainingDaySelect.innerHTML = state.trainingDays
    .map((day) => `<option value="${escapeHtml(day.id)}">${escapeHtml(day.name)}</option>`)
    .join("");
  els.trainingDaySelect.value = state.selectedTrainingDayId;
  els.removeTrainingDay.disabled = state.trainingDays.length <= 1;
}

function renderExercises() {
  const exercises = currentDayExercises();
  if (exercises.length === 0) {
    els.exerciseChips.innerHTML = `<p class="hint">No exercises on this training day yet.</p>`;
    return;
  }

  els.exerciseChips.innerHTML = "";
  exercises.forEach((exercise) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "chip";
    button.textContent = exercise.name;
    button.setAttribute("aria-pressed", String(exercise.id === selectedExercise()?.id));
    button.addEventListener("click", () => commit({ selectedExerciseId: exercise.id }));
    els.exerciseChips.appendChild(button);
  });
}

function renderMetrics() {
  els.metricChips.innerHTML = "";
  metrics.forEach((metric) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "chip";
    button.textContent = metric.label;
    button.setAttribute("aria-pressed", String(metric.name === state.uiSettings.graphMetric));
    button.addEventListener("click", () => {
      commit({ uiSettings: { ...state.uiSettings, graphMetric: metric.name } });
    });
    els.metricChips.appendChild(button);
  });

  const activeMetric = metrics.find((metric) => metric.name === state.uiSettings.graphMetric) ?? metrics[2];
  els.metricHint.textContent = activeMetric.hint;
}

function renderPhases() {
  const activePhase = phases.find((phase) => phase.name === state.currentPhase) ?? phases[1];
  els.phasePill.textContent = activePhase.label;

  els.phaseChips.innerHTML = "";
  els.phaseLegend.innerHTML = "";

  phases.forEach((phase) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "chip phase-chip";
    button.setAttribute("aria-pressed", String(phase.name === state.currentPhase));
    button.innerHTML = `<span class="dot" style="background:${phase.color}"></span>${escapeHtml(phase.label)}`;
    button.addEventListener("click", () => commit({ currentPhase: phase.name }));
    els.phaseChips.appendChild(button);

    const legend = document.createElement("span");
    legend.className = "legend-item";
    legend.innerHTML = `<span class="dot" style="background:${phase.color}"></span>${escapeHtml(phase.label)}`;
    els.phaseLegend.appendChild(legend);
  });
}

function renderLogs() {
  const sortedEntries = [...state.entries].sort((a, b) => b.dateEpochMillis - a.dateEpochMillis);
  els.logCount.textContent = String(sortedEntries.length);

  if (sortedEntries.length === 0) {
    els.logsList.innerHTML = `<p class="hint">No logs yet.</p>`;
    return;
  }

  els.logsList.innerHTML = "";
  sortedEntries.forEach((entry) => {
    const exercise = state.exercises.find((item) => item.id === entry.exerciseId);
    const trainingDay = state.trainingDays.find((item) => item.id === entry.trainingDayId);
    const row = document.createElement("article");
    row.className = "log-row";
    row.innerHTML = `
      <div>
        <strong>${escapeHtml(exercise?.name ?? "Unknown Exercise")}</strong>
        <p class="log-main">${cleanNumber(entry.weight)} x ${entry.reps}</p>
        <p>${escapeHtml(trainingDay?.name ?? "Training Day")} | ${escapeHtml(labelForPhase(entry.phase))}</p>
        <p>${formatDate(entry.dateEpochMillis)}</p>
        <p>Est. 1RM ${cleanNumber(metricValue(entry, "EstimatedOneRepMax"))} | Volume ${cleanNumber(metricValue(entry, "Volume"))}</p>
        ${entry.notes ? `<p>${escapeHtml(entry.notes)}</p>` : ""}
      </div>
    `;
    const button = document.createElement("button");
    button.type = "button";
    button.className = "icon-button delete-button";
    button.title = "Delete log";
    button.setAttribute("aria-label", "Delete log");
    button.textContent = "×";
    button.addEventListener("click", () => {
      commit({ entries: state.entries.filter((item) => item.id !== entry.id) });
    });
    row.appendChild(button);
    els.logsList.appendChild(row);
  });
}

function removeSelectedTrainingDay() {
  if (state.trainingDays.length <= 1) {
    setMessage("Keep at least one training day.");
    return;
  }

  const dayId = state.selectedTrainingDayId;
  const remainingDays = state.trainingDays.filter((day) => day.id !== dayId);
  const selectedTrainingDayId = remainingDays[0]?.id ?? "";
  const exercises = state.exercises.filter((exercise) => exercise.trainingDayId !== dayId);
  const selectedExerciseId = exercises.find((exercise) => exercise.trainingDayId === selectedTrainingDayId)?.id ?? "";

  commit({
    trainingDays: remainingDays,
    selectedTrainingDayId,
    exercises,
    entries: state.entries.filter((entry) => entry.trainingDayId !== dayId),
    selectedExerciseId
  });
  setMessage("Training day removed.");
}

function drawChart() {
  const canvas = els.liftChart;
  const context = canvas.getContext("2d");
  const entries = state.entries
    .filter((entry) => entry.exerciseId === selectedExercise()?.id)
    .sort((a, b) => a.dateEpochMillis - b.dateEpochMillis);

  const rect = canvas.getBoundingClientRect();
  const ratio = window.devicePixelRatio || 1;
  canvas.width = Math.max(1, Math.floor(rect.width * ratio));
  canvas.height = Math.max(1, Math.floor(rect.height * ratio));
  context.scale(ratio, ratio);
  context.clearRect(0, 0, rect.width, rect.height);

  els.emptyChart.classList.toggle("is-hidden", entries.length > 0);
  if (entries.length === 0) return;

  const styles = getComputedStyle(document.body);
  const axisColor = styles.getPropertyValue("--outline").trim();
  const labelColor = styles.getPropertyValue("--muted").trim();
  const surfaceColor = styles.getPropertyValue("--surface").trim();
  const values = entries.map((entry) => metricValue(entry, state.uiSettings.graphMetric));
  const minValue = Math.max(0, Math.min(...values));
  const rawMax = Math.max(...values);
  const maxValue = rawMax === minValue ? rawMax + 10 : rawMax;
  const padding = { left: 42, right: 16, top: 22, bottom: 42 };
  const width = rect.width - padding.left - padding.right;
  const height = rect.height - padding.top - padding.bottom;

  const xFor = (index) => padding.left + (entries.length === 1 ? width / 2 : (index / (entries.length - 1)) * width);
  const yFor = (value) => padding.top + height - ((value - minValue) / (maxValue - minValue)) * height;

  context.strokeStyle = axisColor;
  context.lineWidth = 1;
  context.beginPath();
  context.moveTo(padding.left, padding.top);
  context.lineTo(padding.left, padding.top + height);
  context.lineTo(padding.left + width, padding.top + height);
  context.stroke();

  context.fillStyle = labelColor;
  context.font = "12px system-ui, -apple-system, sans-serif";
  context.textAlign = "right";
  context.fillText(formatAxisValue(maxValue, state.uiSettings.graphMetric), padding.left - 8, padding.top + 4);
  context.fillText(formatAxisValue(minValue, state.uiSettings.graphMetric), padding.left - 8, padding.top + height);

  context.textAlign = "center";
  context.fillText(shortDate(entries[0].dateEpochMillis), padding.left, padding.top + height + 26);
  if (entries.length > 1) {
    context.fillText(shortDate(entries.at(-1).dateEpochMillis), padding.left + width, padding.top + height + 26);
  }

  entries.slice(1).forEach((entry, index) => {
    const from = entries[index];
    context.strokeStyle = colorForPhase(entry.phase || from.phase);
    context.lineWidth = 3;
    context.lineCap = "round";
    context.beginPath();
    context.moveTo(xFor(index), yFor(metricValue(from, state.uiSettings.graphMetric)));
    context.lineTo(xFor(index + 1), yFor(metricValue(entry, state.uiSettings.graphMetric)));
    context.stroke();
  });

  if (entries.length === 1) {
    const entry = entries[0];
    context.strokeStyle = colorForPhase(entry.phase);
    context.lineWidth = 3;
    context.beginPath();
    context.moveTo(xFor(0) - 24, yFor(values[0]));
    context.lineTo(xFor(0) + 24, yFor(values[0]));
    context.stroke();
  }

  entries.forEach((entry, index) => {
    const x = xFor(index);
    const y = yFor(metricValue(entry, state.uiSettings.graphMetric));
    context.fillStyle = colorForPhase(entry.phase);
    context.beginPath();
    context.arc(x, y, 6, 0, Math.PI * 2);
    context.fill();
    context.fillStyle = surfaceColor;
    context.beginPath();
    context.arc(x, y, 2.5, 0, Math.PI * 2);
    context.fill();
  });
}

function parseTranscriptIntoFields() {
  const parsed = parseWorkoutInput(els.transcript.value, selectedExercise()?.name ?? "Exercise");
  const first = parsed[0];
  if (!first) {
    setMessage("Add weight and reps, or write a lift like bench press 145 for 6.");
    return;
  }
  els.weightInput.value = cleanNumber(first.weight);
  els.repsInput.value = String(first.reps);
  els.notesInput.value = els.transcript.value.trim();
  setMessage(parsed.length > 1 ? `Parsed ${parsed.length} sets: ${summarizeWorkoutSets(parsed)}` : "Parsed one set.");
}

function listenForWorkout() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    setMessage("Speech input is not available in this browser.");
    return;
  }

  const recognition = new SpeechRecognition();
  recognition.lang = navigator.language || "en-US";
  recognition.interimResults = false;
  recognition.maxAlternatives = 1;
  recognition.onresult = (event) => {
    const spoken = event.results?.[0]?.[0]?.transcript ?? "";
    els.transcript.value = spoken;
    parseTranscriptIntoFields();
  };
  recognition.onerror = () => setMessage("Speech input stopped before a lift was captured.");
  recognition.start();
}

function saveEntry() {
  const selected = selectedExercise();
  const weight = Number.parseFloat(els.weightInput.value);
  const reps = Number.parseInt(els.repsInput.value, 10);

  if (!selected) {
    setMessage("Add an exercise first.");
    return;
  }
  if (!Number.isFinite(weight) || weight <= 0 || !Number.isFinite(reps) || reps <= 0) {
    setMessage("Weight and reps need to be greater than zero.");
    return;
  }

  const entry = {
    id: newId("entry"),
    exerciseId: selected.id,
    dateEpochMillis: Date.now(),
    weight,
    reps,
    notes: els.notesInput.value.trim() || els.transcript.value.trim(),
    phase: state.currentPhase,
    trainingDayId: state.selectedTrainingDayId
  };

  commit({ entries: [...state.entries, entry] });
  clearLiftFields();
  syncNewEntries([entry]);
  setMessage(state.apiSettings.enabled ? "Logged and queued for sync." : "Logged.");
}

function saveWorkout() {
  const selectedName = selectedExercise()?.name ?? "Exercise";
  const sets = parseWorkoutInput(els.transcript.value, selectedName);
  if (sets.length === 0) {
    setMessage("Write at least one lift like bench press 145 for 6.");
    return;
  }

  let exercises = [...state.exercises];
  const entries = sets.map((set) => {
    let exercise = exercises.find(
      (item) => item.trainingDayId === state.selectedTrainingDayId && item.name.toLowerCase() === set.exerciseName.toLowerCase()
    );
    if (!exercise) {
      exercise = {
        id: newId("exercise"),
        name: set.exerciseName,
        trainingDayId: state.selectedTrainingDayId
      };
      exercises = [...exercises, exercise];
    }
    return {
      id: newId("entry"),
      exerciseId: exercise.id,
      dateEpochMillis: Date.now(),
      weight: set.weight,
      reps: set.reps,
      notes: els.transcript.value.trim(),
      phase: state.currentPhase,
      trainingDayId: state.selectedTrainingDayId
    };
  });

  commit({
    exercises,
    entries: [...state.entries, ...entries],
    selectedExerciseId: entries.at(-1)?.exerciseId ?? state.selectedExerciseId
  });
  clearLiftFields();
  syncNewEntries(entries);
  setMessage(`Logged ${entries.length} sets: ${summarizeWorkoutSets(sets)}`);
}

async function syncNewEntries(entries) {
  if (!state.apiSettings.enabled || entries.length === 0) return;
  const failures = [];
  for (const entry of entries) {
    try {
      await sendLiftEntry(entry);
    } catch (error) {
      failures.push(error.message);
    }
  }
  if (failures.length > 0) {
    setMessage(`Logged, but sync failed: ${failures[0]}`);
  } else {
    setMessage("Logged and synced.");
  }
}

async function sendLatestLift() {
  const selected = selectedExercise();
  const latestEntry = [...state.entries]
    .filter((entry) => entry.exerciseId === selected?.id)
    .sort((a, b) => b.dateEpochMillis - a.dateEpochMillis)[0];
  if (!latestEntry) {
    els.apiStatus.textContent = "Log a lift before syncing.";
    return;
  }
  try {
    els.apiStatus.textContent = "Sending latest lift...";
    await sendLiftEntry(latestEntry);
    els.apiStatus.textContent = "API connector accepted the lift.";
  } catch (error) {
    els.apiStatus.textContent = `API connector failed: ${error.message}`;
  }
}

async function sendLiftEntry(entry) {
  if (!state.apiSettings.endpoint.trim()) {
    throw new Error("Missing API endpoint");
  }
  const exercise = state.exercises.find((item) => item.id === entry.exerciseId);
  const trainingDay = state.trainingDays.find((item) => item.id === entry.trainingDayId);
  if (!exercise || !trainingDay) {
    throw new Error("Missing exercise or training day");
  }
  const base = state.apiSettings.endpoint.replace(/\/+$/, "");
  const response = await fetch(`${base}/liftograph/events`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(state.apiSettings.apiKey ? { Authorization: `Bearer ${state.apiSettings.apiKey}` } : {})
    },
    body: JSON.stringify({
      type: "lift_entry.created",
      source: "Lift-O-Graph Web",
      exercise: { id: exercise.id, name: exercise.name },
      trainingDay: { id: trainingDay.id, name: trainingDay.name },
      entry: {
        ...entry,
        trainingDayName: trainingDay.name,
        routineDay: trainingDay.name
      }
    })
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
}

function parseLiftInput(input) {
  const normalized = input.trim();
  if (!normalized) {
    return { exerciseName: null, weight: null, reps: null, notes: "" };
  }
  const matches = [...normalized.matchAll(/\d+(?:\.\d+)?/g)];
  const first = matches[0];
  const second = matches[1];
  const rawName = first ? normalized.slice(0, first.index) : normalized;
  const exerciseName = rawName
    .replace(/\b(log|add|did|for|at|lbs?|pounds?|kg|kilos?)\b/gi, " ")
    .replace(/\s+/g, " ")
    .trim();
  return {
    exerciseName: exerciseName || null,
    weight: first ? Number.parseFloat(first[0]) : null,
    reps: second ? Number.parseInt(second[0], 10) : null,
    notes: normalized
  };
}

function parseWorkoutInput(input, fallbackExerciseName) {
  const normalized = input.trim();
  if (!normalized) return [];
  const setPattern = /(\d+(?:\.\d+)?)\s*(?:lb|lbs|pounds|kg|kilos)?\s*(?:x|for)\s*(\d+)/gi;
  const matches = [...normalized.matchAll(setPattern)];
  if (matches.length === 0) {
    const single = parseLiftInput(normalized);
    if (!single.weight || !single.reps) return [];
    return [{
      exerciseName: single.exerciseName ? toTitleCase(single.exerciseName) : fallbackExerciseName,
      weight: single.weight,
      reps: single.reps
    }];
  }

  let cursor = 0;
  let currentExercise = fallbackExerciseName;
  return matches.map((match) => {
    const prefix = cleanExerciseName(normalized.slice(cursor, match.index));
    if (prefix) currentExercise = toTitleCase(prefix);
    cursor = match.index + match[0].length;
    return {
      exerciseName: currentExercise,
      weight: Number.parseFloat(match[1]),
      reps: Number.parseInt(match[2], 10)
    };
  }).filter((set) => set.weight > 0 && set.reps > 0);
}

function summarizeWorkoutSets(sets) {
  const grouped = new Map();
  sets.forEach((set) => {
    grouped.set(set.exerciseName, [...(grouped.get(set.exerciseName) ?? []), set]);
  });
  return [...grouped.entries()]
    .map(([exercise, exerciseSets]) => {
      const summary = exerciseSets.map((set) => `${cleanNumber(set.weight)} x ${set.reps}`).join(", ");
      return `${exercise}: ${summary}`;
    })
    .join("; ");
}

function cleanExerciseName(value) {
  return value
    .replace(/[,.;:]+/g, " ")
    .replace(/\b(log|add|did|i|then|and|also|next|set|sets|of|for|at|with|lb|lbs|pounds|kg|kilos|reps?)\b/gi, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function metricValue(entry, metricName) {
  if (metricName === "Weight") return entry.weight;
  if (metricName === "Reps") return entry.reps;
  if (metricName === "Volume") return entry.weight * entry.reps;
  return entry.weight * (1 + entry.reps / 30);
}

function formatAxisValue(value, metricName) {
  if (metricName === "Reps") return String(Math.round(value));
  if (metricName === "Volume" && value >= 1000) return `${(value / 1000).toFixed(1)}k`;
  return String(Math.round(value));
}

function currentDayExercises() {
  return state.exercises.filter((exercise) => exercise.trainingDayId === state.selectedTrainingDayId);
}

function selectedExercise() {
  return state.exercises.find(
    (exercise) => exercise.id === state.selectedExerciseId && exercise.trainingDayId === state.selectedTrainingDayId
  ) ?? currentDayExercises()[0] ?? null;
}

function normalizeState(next) {
  const trainingDays = Array.isArray(next.trainingDays) && next.trainingDays.length ? next.trainingDays : defaultTrainingDays;
  const selectedTrainingDayId = trainingDays.some((day) => day.id === next.selectedTrainingDayId)
    ? next.selectedTrainingDayId
    : trainingDays[0].id;
  const exercises = Array.isArray(next.exercises) ? next.exercises : defaultState.exercises;
  const selectedExerciseId = exercises.some((exercise) => exercise.id === next.selectedExerciseId)
    ? next.selectedExerciseId
    : exercises.find((exercise) => exercise.trainingDayId === selectedTrainingDayId)?.id ?? "";
  return {
    ...defaultState,
    ...next,
    trainingDays,
    selectedTrainingDayId,
    exercises,
    entries: Array.isArray(next.entries) ? next.entries : [],
    selectedExerciseId,
    currentPhase: phases.some((phase) => phase.name === next.currentPhase) ? next.currentPhase : "Maintain",
    apiSettings: { ...defaultState.apiSettings, ...(next.apiSettings ?? {}) },
    uiSettings: { ...defaultState.uiSettings, ...(next.uiSettings ?? {}) }
  };
}

function loadState() {
  try {
    return normalizeState(JSON.parse(localStorage.getItem(STORE_KEY)) ?? defaultState);
  } catch {
    return structuredClone(defaultState);
  }
}

function saveState(next) {
  localStorage.setItem(STORE_KEY, JSON.stringify(next));
}

function clearLiftFields() {
  els.transcript.value = "";
  els.weightInput.value = "";
  els.repsInput.value = "";
  els.notesInput.value = "";
}

function setMessage(message) {
  els.message.textContent = message;
}

function labelForPhase(phaseName) {
  return phases.find((phase) => phase.name === phaseName)?.label ?? "Maintain";
}

function colorForPhase(phaseName) {
  return phases.find((phase) => phase.name === phaseName)?.color ?? "#4fa3ff";
}

function cleanNumber(value) {
  return Number.isInteger(value) ? String(value) : Number(value).toFixed(1).replace(/\.0$/, "");
}

function toTitleCase(value) {
  return value.toLowerCase().split(/\s+/).map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(" ");
}

function formatDate(epochMillis) {
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit"
  }).format(new Date(epochMillis));
}

function shortDate(epochMillis) {
  return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric" }).format(new Date(epochMillis));
}

function newId(prefix) {
  return `${prefix}_${Date.now()}_${Math.floor(1000 + Math.random() * 9000)}`;
}

function toCamel(value) {
  return value.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  }[char]));
}

function registerServiceWorker() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("./service-worker.js").catch(() => {});
  }
}
