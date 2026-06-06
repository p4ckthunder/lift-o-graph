# Lift-O-Graph Scientific Training Roadmap

## Direction

Lift-O-Graph should become a local-first training journal and analysis app: lift logs, workout notes, routines, cardio, nutrition, health signals, and optional AI guidance.

## Local AI Tiers

1. Structured parser first

   Use rules and small local models to turn phrases like `bench 185 for 5, felt easy` into structured workout entries. This is fast, private, and phone-friendly.

2. Small on-device model

   Run a small quantized model on the phone for lift parsing, workout summaries, and simple coaching. Good target use cases are short prompts, journaling summaries, and pattern detection. It will use more battery and memory than a parser.

3. Optional external model

   Keep an optional API connector for a user-owned AI or backend endpoint. This is useful for heavier reasoning, but it should stay opt-in because it needs networking and secrets.

## Training Journal

- Daily workout notes.
- Per-exercise history.
- Routine changes over time.
- Session-level context: energy, soreness, sleep, motivation, and notes.
- AI summary of what changed week to week.

## Routines

- Create routines like Push, Pull, Legs, Upper, Lower, Cardio, or custom.
- Attach exercises to routine days.
- Log a whole workout session instead of only individual lifts.
- Compare routine versions over time.

## Cardio

- Track duration, distance, pace, heart rate, and perceived effort.
- Support non-distance cardio like treadmill, bike, rowing, stair climber, jump rope, and circuits.
- Show cardio progress over time with phase context.

## Health Integrations

- Google Fit / Health Connect first for Android.
- Possible Fitbit integration later if the API path is worth it.
- Use heart-rate, sleep, steps, calories, and workout-session data when available.
- Keep health integrations opt-in.

## Nutrition

- Track calories, protein, carbs, fat, and meal timing.
- Connect intake to training output.
- Help discover patterns like better performance after higher-carb days or specific pre-workout timing.
- Support bulk, cut, and maintain phases.

## Analysis Ideas

- Best estimated one-rep max by exercise.
- Performance by phase.
- Performance by bodyweight, sleep, macros, and meal timing.
- Heart-rate response by lift or workout type.
- Fatigue trend and recovery warnings.
- Leaderboards as an optional online feature, separate from local private data.
