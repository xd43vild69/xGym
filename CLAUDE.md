# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

**xGym** is a native Android fitness app (Kotlin + Jetpack Compose + Room) for tracking exercise sets, rest times, and workout sessions. Data persists locally in SQLite—no internet or account required.

## Building & Testing

```bash
# Set Java runtime (required on macOS without Android Studio active)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Build
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk

# Compile Kotlin only (quick validation)
./gradlew compileDebugKotlin

# Unit tests
./gradlew testDebugUnitTest

# Or open the project in Android Studio and build/run from there
```

**Java version:** JDK 17 (set `JAVA_HOME` above if Gradle fails)

## Architecture

### Data Layer (`data/`)

**Room database schema:**
- **Category** → Subcategory → Exercise (hierarchical structure, seeded at startup)
- **Session** (workout instance): tracks `startTs`, `endTs`, `durationMs` (total elapsed time)
- **SetRecord** (individual exercise set): links to Session + Exercise; stores `exerciseStartTs`/`exerciseEndTs`, `restEndTs`, `reps`

Key insight: `Session.startTs` is set when the user presses "Iniciar serie" (startSet) for the first time, not when they navigate to the workout screen. `durationMs` captures the paused/resumed total time.

**Preferences** (SharedPreferences wrapper): stores `restDurationSeconds` (configured in settings).

### Presentation Layer

**UI Navigation** (`ui/AppNav.kt`):
```
home → categories → subcategories → exercises → workout → summary
                  ↓ (direct)              ↓ (direct)
                  └──────────────────────┘
             (Pierna, Cardio skip subcategories)
```
Additional routes: `history`, `settings`

**Screens**:
- **HomeScreen**: Start workout or view history
- **CategoryScreen / SubcategoryScreen / ExerciseScreen**: Navigate catalog; exercise list supports long-press drag-to-reorder (drag gestures in `pointerInput` must use stable `ex.id` as the key, not composable `index`)
- **WorkoutScreen**: Real-time UI for active training; displays current phase (IDLE/EXERCISING/RESTING), elapsed time for current set + total session time
- **SummaryScreen / HistoryScreen**: Review completed sessions
- **SettingsScreen**: Configure rest duration, clear data

### State Management (`viewmodel/WorkoutViewModel.kt`)

Single ViewModel manages entire workout state via `WorkoutUiState`:

```kotlin
enum class Phase { IDLE, EXERCISING, RESTING }

data class WorkoutUiState(
    val phase: Phase,
    val sessionStartTs: Long? = null,           // When first set started (null = not started yet)
    val sessionActive: Boolean = false,          // Is timer running? (paused on background)
    val sessionElapsedPausedMs: Long = 0,       // Total elapsed time (paused value)
    val phaseStartTs: Long = 0,                 // When current set/rest began
    val elapsedMs: Long = 0,                    // Elapsed time in current phase
    // ... other fields for set number, exercise, pending reps, rest alarm, etc.
)
```

**Key flows:**
1. **selectExercise()** → Creates Session in DB (temporary `startTs`), loads exercise, sets phase = IDLE
2. **startSet()** → First call: updates Session.startTs in DB; activates session timer; phase = EXERCISING
3. **endSet()** → Records SetRecord; phase = RESTING; displays rest timer
4. **startNextSet()** → Closes previous rest period; advances set number; phase = EXERCISING
5. **finishSession()** → Writes final durationMs to Session; clears state

**Background handling** (lifecycle events):
- ON_PAUSE → `pauseSessionTimer()` (sets `sessionActive = false`; elapsed time frozen)
- ON_RESUME → `resumeSessionTimer()` (resumes only if `sessionStartTs != null`, i.e., a set has started)

**Timing ticker** (`init` block): Updates every 200ms; recalculates phase elapsed time and session total time if active. Rest alarm fires once when rest duration exceeded.

## Key Patterns & Conventions

### Drag Reordering (Exercise List)
- Long-press + drag an exercise to reorder within a subcategory
- Internally: swap items in list, update `orderIndex`, persist via `vm.reorderExercises(exercises)`
- **Critical:** `pointerInput` block key must be exercise `id`, not composable `index`. The lambda captures the stable `id` at declaration; indices are invalidated on reordering. Item height calculation must account for padding (not just approximation).

### Time Tracking
- **Phase elapsed** (`elapsedMs`): Duration of current exercise or rest period; shown on-screen; resets to 0 when phase changes
- **Session elapsed** (`sessionElapsedPausedMs`): Total accumulated time; only increments when `sessionActive = true`; paused on background
- **DB timestamps** (`startTs`, `exerciseStartTs`, `exerciseEndTs`, `restEndTs`): Epoch milliseconds; absolute reference for audit/replay

### UI Lifecycle & Composition
- Main activity keeps screen on (`FLAG_KEEP_SCREEN_ON`) during workouts
- WorkoutScreen observes lifecycle events to pause/resume timer
- Exercise list is read-only from DB except for order; new exercises added via dialog
- Summary screen is stateless (loads session data on navigation with `sessionId` arg)

## Debugging Notes

- **Workout timer not starting:** Check `sessionStartTs` is initialized in `startSet()`, and `sessionActive` is true. Verify lifecycle resume logic.
- **Drag reordering broken:** Ensure `pointerInput` key is exercise id (stable), not index. Item height must match actual Card height + padding.
- **Compose recompositions:** State keys (`key = { ex.id }`) in LazyColumn prevent UI glitches when lists reorder; without them, composables are reused in wrong positions.

## Dependency Versions

- Jetpack Compose: 2024.12.01 BOM
- Androidx Lifecycle: 2.8.7
- Navigation Compose: 2.8.5
- Room: 2.6.1
- Kotlin Coroutines (test): 1.9.0
- Kotlin JVM target: 17
- Android Compile SDK: 36, Target: 36, Min: 26
