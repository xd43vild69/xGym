package com.d13.xgym.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

@Composable
fun SummaryScreen(nav: NavController, sessionId: Long) {
    val context = LocalContext.current
    var sets by remember { mutableStateOf<List<SetWithExercise>>(emptyList()) }
    LaunchedEffect(sessionId) {
        sets = AppDatabase.get(context).workoutDao().setsForSession(sessionId)
    }

    val byExercise = sets.groupBy { it.exerciseName }
    ListScaffold("Resumen de la sesión") {
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
fun HistoryScreen(nav: NavController) {
    val context = LocalContext.current
    val sessions by AppDatabase.get(context).workoutDao()
        .sessionsWithCategory().collectAsStateWithLifecycle(emptyList())

    ListScaffold("Historial") {
        items(sessions) { s ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("${s.session.date} — ${s.categoryName}",
                        style = MaterialTheme.typography.titleMedium)
                    val duration = s.session.endTs?.minus(s.session.startTs)
                    Text(
                        "${s.setCount} series" +
                            (duration?.let { " · duración ${formatMs(it)}" } ?: " · en curso"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.material3.TextButton(
                        onClick = { nav.navigate("summary/${s.session.id}") }
                    ) { Text("Ver detalle") }
                }
            }
        }
    }
}
