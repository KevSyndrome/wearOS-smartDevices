# FitPulse — Wear OS + Android Phone

Multi-device fitness demo for the Smart Devices Development final project.
The **watch** is the immediate workout control surface; the **phone** is the authoritative hub
that stores history and owns the configuration.

## Features

| # | Feature | API | Direction |
|---|---------|-----|-----------|
| 1 | Start / Stop workout with acknowledgement | `MessageClient` | Watch → Phone → Watch |
| 2 | Daily step goal synchronization | `DataClient` | Phone → Watch |

### Communication contract

```text
# Feature 1 - MessageClient
/fitpulse/workout/start   Watch -> Phone   WorkoutCommand JSON
/fitpulse/workout/stop    Watch -> Phone   WorkoutCommand JSON
/fitpulse/workout/ack     Phone -> Watch   WorkoutAck JSON

# Feature 2 - DataClient
/fitpulse/config/daily_goal
  daily_step_goal      Int
  goal_updated_at      Long (epoch millis)
```

Both modules share `applicationId = "com.example.fitpulse"` and duplicate the same
`WearableConstants.kt` / `DataModels.kt` files. The phone writes the goal; the watch only reads it.
The phone computes the official workout duration and calories, persists the session, and returns
the summary in the acknowledgement.

## Project layout

```text
mobile/   Jetpack Compose Material 3 hub  (minSdk 26, compileSdk 36)
wear/     Wear Compose Material 3 client (minSdk 30, compileSdk 36.1)
docs/     final report, screenshots and agent handoff
```

## Build

```bash
./gradlew :mobile:assembleDebug :wear:assembleDebug
```

APKs:

```text
mobile/build/outputs/apk/debug/mobile-debug.apk
wear/build/outputs/apk/debug/wear-debug.apk
```

## Run (Android Studio)

1. Open the project and let Gradle sync.
2. Start the Phone emulator (API 36) and the Wear OS emulator (API 36.1).
3. In **Device Manager**, open the Wear emulator's three-dot menu → **Pair Wearable** and pair it
   with the phone. Wait for the link icon. The phone image must have the Wear OS companion app for
   the Data Layer to start (Android Studio handles this during pairing).
4. Run `mobile` on the phone and `wear` on the watch.

### Demo flow

1. On the watch, tap **START WORKOUT** → the phone switches to `ACTIVE` and starts logging.
2. Wait a few seconds, then tap **STOP WORKOUT** → the phone computes duration/calories and saves the
   session in history; the watch shows `Saved mm:ss · N kcal`.
3. On the phone, move the **DAILY STEP GOAL** slider and tap **Sync goal to watch** → the watch goal
   line updates through `onDataChanged`.
4. Reopen the watch app → it reloads the replicated goal.

## Implementation notes

- Coroutines + `Task.await()` (`kotlinx-coroutines-play-services`) replace nested listeners.
- `lifecycleScope` cancels pending communication when the activity is destroyed.
- `onMessageReceived` differentiates `START` and `STOP` by path; `onDataChanged` filters by data-item
  path. Feature 2 uses a different transport, so features cannot collide.
- Wearable 20.x delivers `DataEventBuffer`; each `DataItem` exposes bytes, so the code uses
  `DataMap.fromByteArray(item.data)` and releases the buffer in `finally`.
- Phone UI uses `enableEdgeToEdge()` + `safeDrawingPadding()` for API 36.
- The watch UI is a single round-screen layout with a 14 dp top spacer for the system time.

## Documentation

- Final report: `docs/Final Project_FitPulse_KevinNoh_KennyPeralta.docx`
- UI captures: `docs/screenshots/`
- Work log / continuation notes: `docs/HANDOFF.md`

## Authors

Kevin Noh · Kenny Peralta (TI91)
