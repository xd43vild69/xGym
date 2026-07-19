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
import com.d13.xgym.viewmodel.Phase
import com.d13.xgym.viewmodel.WorkoutViewModel

@Composable
fun WorkoutScreen(nav: NavController, vm: WorkoutViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var repsText by remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.resumeSessionTimer()
                Lifecycle.Event.ON_PAUSE -> vm.pauseSessionTimer()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatHMS(ui.sessionElapsedPausedMs), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text(ui.exerciseName, style = MaterialTheme.typography.headlineMedium)
            Text("Serie ${ui.setNumber}", style = MaterialTheme.typography.titleMedium)
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
                    Phase.EXERCISING -> Color(0xFF4CAF50)
                    Phase.RESTING -> Color(0xFF2196F3)
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
            Text(formatMs(ui.elapsedMs), style = MaterialTheme.typography.displayLarge)
        }

        Column(Modifier.fillMaxWidth()) {
            when (ui.phase) {
                Phase.IDLE -> Button(
                    onClick = { vm.startSet() },
                    Modifier.fillMaxWidth().height(72.dp)
                ) { Text("Iniciar serie", style = MaterialTheme.typography.titleLarge) }

                Phase.EXERCISING -> Button(
                    onClick = { vm.endSet() },
                    Modifier.fillMaxWidth().height(72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Terminar serie", style = MaterialTheme.typography.titleLarge) }

                Phase.RESTING -> Button(
                    onClick = { vm.startNextSet() },
                    Modifier.fillMaxWidth().height(72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) { Text("Iniciar serie ${ui.setNumber}", style = MaterialTheme.typography.titleLarge) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        nav.navigate("exercises/${ui.categoryId}/${ui.subcategoryId}") {
                            popUpTo("workout") { inclusive = true }
                        }
                    },
                    Modifier.weight(1f)
                ) { Text("Cambiar ejercicio") }
                OutlinedButton(
                    onClick = {
                        nav.navigate("subcategories/${ui.categoryId}") {
                            popUpTo("workout") { inclusive = true }
                        }
                    },
                    Modifier.weight(1f)
                ) { Text("Subcategoría") }
            }
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

    if (ui.pendingSetId != null) {
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
