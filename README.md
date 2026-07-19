# xGym

App Android nativa (Kotlin + Jetpack Compose + Room) para controlar tiempos de ejercicio y descanso en el gimnasio.

## Flujo
1. **Inicio** → Iniciar entrenamiento o ver Historial.
2. **Categoría**: Pecho/Hombro/Tríceps, Espalda/Bíceps, Pierna, Cardio.
3. **Subcategoría**: solo si la categoría tiene varias (Pierna y Cardio saltan directo).
4. **Ejercicio**: lista sembrada + botón para agregar nuevos.
5. **Entrenamiento**: cronómetro de ejercicio → "Terminar serie" (pide reps) → cronómetro de descanso → "Iniciar serie N". También: cambiar ejercicio, cambiar subcategoría, finalizar sesión.
6. **Resumen / Historial**: series con duración, reps y descanso por ejercicio; sesiones por fecha.

Los datos se guardan localmente en SQLite (Room), sin internet ni cuenta.

## Compilar
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # tests
```
O abrir la carpeta en Android Studio y ejecutar.
