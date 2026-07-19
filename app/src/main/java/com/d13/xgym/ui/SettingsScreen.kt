package com.d13.xgym.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.d13.xgym.data.Preferences
import com.d13.xgym.viewmodel.WorkoutViewModel

@Composable
fun SettingsScreen(nav: NavController, prefs: Preferences, vm: WorkoutViewModel) {
    var restText by remember { mutableStateOf(prefs.restDurationSeconds.toString()) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showClearTodayDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        LazyColumn {
            item {
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Duración de descanso:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = restText,
                                onValueChange = {
                                    val filtered = it.filter { c -> c.isDigit() }
                                    restText = filtered
                                    val sec = filtered.toIntOrNull() ?: 90
                                    if (sec > 0) prefs.restDurationSeconds = sec
                                },
                                label = { Text("s") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                modifier = Modifier.width(80.dp),
                                singleLine = true
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "El celular vibrará cuando se cumplan los segundos de descanso.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text("Entrenamiento", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { nav.navigate("weeklyPlan") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Plan semanal") }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text("Historial", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
            }

            item {
                OutlinedButton(
                    onClick = { showClearTodayDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Borrar historial de hoy") }
            }

            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Limpiar todo el historial", color = MaterialTheme.colorScheme.error) }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Button(onClick = { nav.popBackStack() }, Modifier.fillMaxWidth()) {
                    Text("Volver")
                }
            }
        }
    }

    if (showClearTodayDialog) {
        AlertDialog(
            onDismissRequest = { showClearTodayDialog = false },
            title = { Text("¿Borrar historial de hoy?") },
            text = { Text("Se eliminarán todas las sesiones de hoy. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearTodaysSessions()
                    showClearTodayDialog = false
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { showClearTodayDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("¿Limpiar TODO?") },
            text = { Text("Se eliminarán TODAS las sesiones. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAllSessions()
                    showClearAllDialog = false
                }) { Text("Eliminar todo") }
            },
            dismissButton = { TextButton(onClick = { showClearAllDialog = false }) { Text("Cancelar") } }
        )
    }
}
