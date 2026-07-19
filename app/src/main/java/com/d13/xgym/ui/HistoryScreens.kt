package com.d13.xgym.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.d13.xgym.data.AppDatabase
import com.d13.xgym.data.SetWithExercise
import com.d13.xgym.data.SessionWithCategory
import com.d13.xgym.data.Session
import com.d13.xgym.viewmodel.WorkoutViewModel

@Composable
fun SummaryScreen(nav: NavController, sessionId: Long) {
    val context = LocalContext.current
    var sets by remember { mutableStateOf<List<SetWithExercise>>(emptyList()) }
    var sessionInfo by remember { mutableStateOf<SessionWithCategory?>(null) }
    
    LaunchedEffect(sessionId) {
        val db = AppDatabase.get(context)
        sets = db.workoutDao().setsForSession(sessionId)
        sessionInfo = db.workoutDao().sessionWithCategory(sessionId)
    }

    val byExercise = sets.groupBy { it.exerciseName }
    ListScaffold("Resumen de la sesión") {
        item {
            sessionInfo?.let { info ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(info.categoryName, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Fecha: ${info.session.date}", style = MaterialTheme.typography.bodyMedium)
                        val totalTime = info.session.durationMs?.let { formatHMS(it) }
                            ?: info.session.endTs?.minus(info.session.startTs)?.let { formatHMS(it) }
                            ?: "En curso"
                        Text("Tiempo total: $totalTime", style = MaterialTheme.typography.bodyMedium)
                        Text("Series completadas: ${info.setCount}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        byExercise.forEach { (name, exSets) ->
            item {
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                    exSets.forEach { s ->
                        val workMs = s.set.exerciseEndTs - s.set.exerciseStartTs
                        val restMs = s.set.restEndTs?.minus(s.set.exerciseEndTs)
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Text(
                                "Serie ${s.set.setNumber}: ${formatMs(workMs)}" +
                                    (s.set.reps?.let { " · $it reps" } ?: "") +
                                    (restMs?.let { " · descanso ${formatMs(it)}" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Button(onClick = { nav.navigate("home") { popUpTo("home") { inclusive = true } } },
                Modifier.fillMaxWidth()) { Text("Ir al inicio") }
        }
    }
}

@Composable
fun HistoryScreen(nav: NavController, vm: WorkoutViewModel) {
    val context = LocalContext.current
    val sessions by AppDatabase.get(context).workoutDao()
        .sessionsWithCategory().collectAsStateWithLifecycle(emptyList())

    var sessionToDelete by remember { mutableStateOf<Session?>(null) }

    ListScaffold("Historial") {
        items(sessions, key = { it.session.id }) { s ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        sessionToDelete = s.session
                    }
                    false // Snap back, wait for dialog confirmation
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true,
                backgroundContent = {
                    val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(vertical = 6.dp)
                            .background(color),
                        Alignment.CenterEnd
                    ) {
                        Text("🗑️", Modifier.padding(end = 24.dp), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            ) {
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${s.session.date} — ${s.categoryName}",
                            style = MaterialTheme.typography.titleMedium)
                        val totalTime = s.session.durationMs?.let { formatHMS(it) }
                            ?: s.session.endTs?.minus(s.session.startTs)?.let { formatHMS(it) }
                            ?: "en curso"
                        Text(
                            "${s.setCount} series · $totalTime",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { nav.navigate("summary/${s.session.id}") }
                        ) { Text("Ver detalle") }
                    }
                }
            }
        }
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Eliminar sesión") },
            text = { Text("¿Estás seguro de que deseas eliminar este entrenamiento del historial?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSession(sessionToDelete!!)
                    sessionToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}
