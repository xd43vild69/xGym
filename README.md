# xGym

App Android nativa (Kotlin + Jetpack Compose + Room) para controlar tiempos de ejercicio y descanso en el gimnasio.

## Flujo
1. **Inicio** → botón **Entrenar**. En la parte inferior hay una **barra de navegación** (Home · Historial · Ajustes) visible en todas las pantallas.
2. **Categoría**: 8 categorías independientes — Pecho, Hombro, Tríceps, Espalda, Bíceps, Pierna, Cardio, Core. Si hay un **plan** para el día de hoy, esas categorías salen primero (ordenadas alfabéticamente y en azul) y el resto se atenúa en gris, pero siguen siendo seleccionables.
3. **Ejercicio**: lista sembrada por categoría + botón para agregar nuevos. Deslizar para renombrar/eliminar; mantener presionado para reordenar.
4. **Entrenamiento** (pantalla principal): barra superior con **Tiempo total** y **Ejercicio**; cronómetro grande de la fase actual; tarjeta con el número de **serie completadas**; botón principal por fase:
   - **INICIAR** → empieza el ejercicio (fase EJERCICIO).
   - **TERMINAR** → cierra la serie (aquí avanza el contador de serie), pide reps y empieza el descanso. En **Cardio** no se piden reps.
   - **SIGUIENTE SERIE** → arranca la próxima serie tras el descanso.
   - Botones **Categoría** / **Subcategoría** para moverse, y **Finalizar sesión**.
5. **Resumen / Historial**: series con duración, reps y descanso por ejercicio; sesiones por fecha.

## Plan semanal
En **Ajustes → Plan semanal** se configura qué se entrena cada día (L–D). Cada día admite **varias categorías** o **Descanso** (exclusivo); se puede vaciar un día o limpiar la semana. El plan persiste localmente y guía el orden/resaltado en la pantalla "Elige categoría".

## Ajustes
- **Duración de descanso** (segundos): al cumplirse, el celular vibra.
- **Plan semanal**.
- **Borrar historial de hoy** / **Limpiar todo el historial**.

Los datos se guardan localmente en SQLite (Room) y las preferencias en SharedPreferences, sin internet ni cuenta.

## Compilar
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/
./gradlew installDebug           # instala en el dispositivo/emulador conectado
./gradlew testDebugUnitTest      # tests
```
O abrir la carpeta en Android Studio y ejecutar.
