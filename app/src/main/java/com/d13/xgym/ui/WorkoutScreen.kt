package com.d13.xgym.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    val phaseColor = when (ui.phase) {
        Phase.EXERCISING -> PhaseExercising
        Phase.RESTING -> PhaseResting
        else -> MaterialTheme.colorScheme.onBackground
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Barra superior: secundaria (Tiempo total | Ejercicio) ---
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TIEMPO TOTAL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatHMS(ui.sessionElapsedMs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            VerticalDivider(
                Modifier.height(40.dp).padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "EJERCICIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    ui.exerciseName.ifEmpty { "—" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- Bloque central: prioritario (fase + cronómetro gigante + serie) ---
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                when (ui.phase) {
                    Phase.IDLE -> "LISTO"
                    Phase.EXERCISING -> "EJERCICIO"
                    Phase.RESTING -> "DESCANSO"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = phaseColor
            )
            Text(formatMs(ui.elapsedMs), style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(28.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier.padding(horizontal = 40.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "SERIE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${ui.setNumber}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Zona de acciones ---
        Column(Modifier.fillMaxWidth()) {
            when (ui.phase) {
                Phase.IDLE -> Button(
                    onClick = { vm.startSet() },
                    Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(50),
                    enabled = ui.exerciseId != null
                ) { Text("INICIAR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

                Phase.EXERCISING -> Button(
                    onClick = { vm.endSet() },
                    Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PhaseExercising,
                        contentColor = Color.White
                    )
                ) { Text("TERMINAR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

                Phase.RESTING -> Button(
                    onClick = { vm.startNextSet() },
                    Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PhaseResting,
                        contentColor = Color.White
                    )
                ) { Text("SIGUIENTE SERIE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        nav.navigate("categories") {
                            popUpTo("workout") { inclusive = true }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Categoría") }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = {
                        nav.navigate("subcategories/${ui.categoryId}") {
                            popUpTo("workout") { inclusive = true }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Subcategoría") }
            }
            Spacer(Modifier.height(8.dp))
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
            title = {
                Column {
                    Text("Repeticiones", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Serie ${ui.setNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
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
