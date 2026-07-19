# xGym

Native Android app (Kotlin + Jetpack Compose + Room) to track exercise and rest times at the gym.

## Flow
1. **Home** → **Train** button. There is a **navigation bar** at the bottom (Home · History · Settings) visible on all screens.
2. **Category**: 8 independent categories — Chest, Shoulder, Triceps, Back, Biceps, Leg, Cardio, Core. If there is a **plan** for today, those categories appear first (sorted alphabetically and in blue), and the rest are grayed out but remain selectable.
3. **Exercise**: category-seeded list + button to add new ones. Swipe to rename/delete; long press to reorder.
4. **Workout** (main screen): top bar with **Total time** and **Exercise**; large stopwatch for the current phase; card showing the number of **completed sets**; main button per phase:
   - **START** → starts the exercise (EXERCISE phase).
   - **FINISH** → finishes the set (increments the set counter), prompts for reps, and starts the rest. Reps are not prompted in **Cardio**.
   - **NEXT SET** → starts the next set after the rest.
   - **Category** / **Subcategory** buttons to navigate, and **Finish session**.
5. **Summary / History**: sets with duration, reps, and rest per exercise; sessions by date.

## Weekly Plan
In **Settings → Weekly Plan**, you can configure what is trained each day (Mon–Sun). Each day accepts **multiple categories** or **Rest** (exclusive); you can clear a single day or clear the entire week. The plan persists locally and guides the ordering/highlighting on the "Choose category" screen.

## Settings
- **Rest duration** (seconds): once completed, the phone vibrates.
- **Weekly plan**.
- **Clear today's history** / **Clear all history**.

Data is saved locally in SQLite (Room) and preferences in SharedPreferences, with no internet or account required.

## Build
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug          # APK in app/build/outputs/apk/debug/
./gradlew installDebug           # installs on the connected device/emulator
./gradlew testDebugUnitTest      # tests
```
Or open the folder in Android Studio and run.
