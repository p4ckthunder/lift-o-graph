# Lift-O-Graph

Standalone Android-first lift tracker for local workout logging, progress graphs, phase coloring, and an optional AI/API connector.

## MVP

- Add any exercise, not just a fixed preset list.
- Log lifts with text, manual fields, or Android speech recognition.
- Log multiple sets from one workout sentence.
- Create custom Training Days, such as Push Day or Pull Day, and keep exercises grouped inside each day.
- View and delete individual logs from the All Logs list.
- Store data locally on the phone with no required internet connection.
- Graph weight, reps, estimated one-rep max, or volume over date for the selected exercise.
- Color graph segments by training phase: bulk, maintain, or cut.
- Enable the optional API connector to send lift events to your own AI or backend.
- Send newly logged lifts or the latest selected lift as JSON events to `POST /liftograph/events`.

## Android Build

Requirements:

- Android Studio or a JDK available as `java`
- Android SDK with API 35 installed

This folder is a standard Android Gradle project with two installable variants:

| Variant | App label | Package | Purpose |
| --- | --- | --- | --- |
| `dev` | `Lift-O-Graph Dev` | `com.liftograph.app.dev` | Your phone testing build. Installs beside the normal app. |
| `prod` | `Lift-O-Graph` | `com.liftograph.app` | Normal build to share by APK later. |

To build, install, and launch the dev app on a connected phone:

```powershell
.\tools\run-on-android.ps1
```

To install the normal app instead:

```powershell
.\tools\run-on-android.ps1 prod
```

To make the normal shareable APK:

```powershell
.\tools\build-debug-apk.ps1
```

The normal debug APK will be copied to:

```text
output\apks\lift-o-graph-prod-debug.apk
```

That APK can be shared directly for side-loading while the app is in private testing. A signed release APK can be added later when the normal app is ready to distribute more broadly.

## Workout Text Examples

Lift-O-Graph can parse one set:

```text
bench press 145 for 6
```

Or a whole workout:

```text
bench press 145 for 6 and 145 for 8, curls 30 for 12, squat 225 for 5
```

Each set is stored with both weight and reps. The graph can then show:

- `Weight`: load used on each set
- `Reps`: reps completed on each set
- `Est. 1RM`: combined strength estimate from weight and reps
- `Volume`: weight x reps

## API Event Shape

The optional connector posts this payload to the configured endpoint:

```json
{
  "type": "lift_entry.created",
  "source": "Lift-O-Graph Android",
  "exercise": {
    "id": "exercise_...",
    "name": "Bench Press"
  },
  "trainingDay": {
    "id": "push_day",
    "name": "Push Day"
  },
  "entry": {
    "id": "entry_...",
    "dateEpochMillis": 1780450000000,
    "weight": 185,
    "reps": 5,
    "phase": "Bulk",
    "trainingDayId": "push_day",
    "trainingDayName": "Push Day",
    "routineDay": "Push Day",
    "notes": "bench press 185 for 5"
  }
}
```

For an Android emulator talking to a server on the host computer, use `http://10.0.2.2:<port>` as the endpoint. For a real phone, use the computer's LAN IP address.

## API Mock Receiver

To test the optional API connector locally:

```powershell
node .\tools\api-mock-server.js
```

Use this endpoint in Android emulator developer mode:

```text
http://10.0.2.2:8790
```

Events are appended to:

```text
output\api-events.jsonl
```

Set `LIFTOGRAPH_API_KEY` before launching the mock if you want to require a bearer key.

## Next Targets

- Replace the JSON preference store with Room once workout history needs filtering, export, or migrations.
- Add CSV/JSON export and import.
- Add optional leaderboard sync as a separate opt-in network feature.
- Add desktop support from the shared data/event model.
- Add local AI parsing after the base logger is stable.

Longer-term scientific tracker direction: [docs/scientific-training-roadmap.md](docs/scientific-training-roadmap.md)
