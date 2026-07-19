package com.d13.xgym.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.d13.xgym.ui.theme.PhaseExercising
import com.d13.xgym.ui.theme.PhaseResting
import com.d13.xgym.viewmodel.Phase
import com.d13.xgym.viewmodel.WorkoutViewModel

@Composable
fun WorkoutScreen(nav: NavController, vm: WorkoutViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val categories by vm.catalogDao.categories().collectAsStateWithLifecycle(emptyList())
    var repsText by remember { mutableStateOf("") }

    val currentCategory = categories.firstOrNull { it.id == ui.categoryId }
    val isCardio = currentCategory?.name == "Cardio"

    // En Cardio, auto-setear reps = 0 cuando hay un pending set (sin diálogo)
    LaunchedEffect(ui.pendingSetId, isCardio) {
        if (isCardio && ui.pendingSetId != null) {
            vm.setReps(0)
        }
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Tiempo total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(formatHMS(ui.sessionElapsedMs), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(24.dp))
            Text(
                ui.exerciseName.ifEmpty { "Selecciona un ejercicio para comenzar" },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "Serie ${ui.setNumber}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (ui.phase) {
                    Phase.IDLE -> "Listo"
                    Phase.EXERCISING -> "EJERCICIO"
                    Phase.RESTING -> "DESCANSO"
                },
                style = MaterialTheme.typography.titleLarge,
                color = when (ui.phase) {
                    Phase.EXERCISING -> PhaseExercising
                    Phase.RESTING -> PhaseResting
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
            Text(formatMs(ui.elapsedMs), style = MaterialTheme.typography.displayLarge)
        }

        Column(Modifier.fillMaxWidth()) {
            when (ui.phase) {
                Phase.IDLE -> Button(
                    onClick = { vm.startSet() },
                    Modifier.fillMaxWidth().height(72.dp),
                    enabled = ui.exerciseId != null
                ) { Text("Iniciar serie", style = MaterialTheme.typography.titleLarge) }

                Phase.EXERCISING -> Button(
                    onClick = { vm.endSet() },
                    Modifier.fillMaxWidth().height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PhaseExercising,
                        contentColor = Color.White
                    )
                ) { Text("Terminar serie", style = MaterialTheme.typography.titleLarge) }

                Phase.RESTING -> Button(
                    onClick = { vm.startNextSet() },
                    Modifier.fillMaxWidth().height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PhaseResting,
                        contentColor = Color.White
                    )
                ) { Text("Iniciar serie ${ui.setNumber}", style = MaterialTheme.typography.titleLarge) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    nav.navigate("subcategories/${ui.categoryId}") {
                        popUpTo("workout") { inclusive = true }
                    }
                },
                Modifier.fillMaxWidth()
            ) { Text("Subcategoría", textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    vm.finishSession { sessionId ->
                        nav.navigate("summary/$sessionId") {
                            popUpTo("home")
                        }
                    }
                },
                Modifier.fillMaxWidth()
            ) { Text("Finalizar sesión") }
        }
    }

    if (ui.pendingSetId != null && !isCardio) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Repeticiones de la serie ${ui.setNumber - 1}") },
            text = {
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setReps(repsText.toIntOrNull() ?: 0)
                    repsText = ""
                }) { Text("Guardar") }
            }
        )
    }

    if (ui.showRestAlarm) {
        AlertDialog(
            onDismissRequest = { vm.dismissRestAlarm() },
            title = { Text("¡Hora de descanso!", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Se cumplió el tiempo de descanso configurado.") },
            confirmButton = {
                TextButton(onClick = { vm.dismissRestAlarm() }) { Text("OK") }
            }
        )
    }
}
