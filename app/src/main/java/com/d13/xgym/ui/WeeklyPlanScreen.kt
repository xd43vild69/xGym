package com.d13.xgym.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.d13.xgym.data.Category
import com.d13.xgym.data.Preferences
import com.d13.xgym.ui.theme.OnSurfaceVariant
import com.d13.xgym.ui.theme.Outline
import com.d13.xgym.viewmodel.WorkoutViewModel
import java.time.LocalDate

private val dayNames = listOf(
    "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
)

private val categoryPalette = listOf(
    Color(0xFF00E5FF), // cian
    Color(0xFFFFB300), // ámbar
    Color(0xFF4CAF50), // verde
    Color(0xFFAB47BC)  // púrpura
)

private fun slotColor(code: Int): Color = when (code) {
    Preferences.EMPTY -> Outline
    Preferences.REST -> OnSurfaceVariant
    else -> categoryPalette[(code - 1).mod(categoryPalette.size)]
}

private fun slotLabel(code: Int, categories: List<Category>): String = when (code) {
    Preferences.EMPTY -> "— Sin asignar"
    Preferences.REST -> "Descanso"
    else -> categories.firstOrNull { it.id == code.toLong() }?.name ?: "— Sin asignar"
}

@Composable
fun WeeklyPlanScreen(nav: NavController, prefs: Preferences, vm: WorkoutViewModel) {
    var plan by remember { mutableStateOf(prefs.weeklyPlan) }
    var dayToEdit by remember { mutableStateOf<Int?>(null) }
    var showClearWeekDialog by remember { mutableStateOf(false) }
    val categories by vm.catalogDao.categories().collectAsStateWithLifecycle(emptyList())

    val todayIndex = LocalDate.now().dayOfWeek.value - 1 // 0=Lunes … 6=Domingo

    fun setDay(day: Int, code: Int) {
        plan = plan.toMutableList().also { it[day] = code }
        prefs.weeklyPlan = plan
    }

    val trainingCount = plan.count { it > 0 }
    val restCount = plan.count { it == Preferences.REST }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Text("Plan semanal", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "$trainingCount entrenamientos · $restCount descansos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(7) { day ->
                val code = plan[day]
                val isToday = day == todayIndex
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .height(72.dp),
                    colors = CardDefaults.cardColors(),
                    border = if (isToday)
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else null
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Barra de color-coding
                        Box(
                            Modifier
                                .width(6.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                                .background(slotColor(code))
                        )
                        TextButton(
                            onClick = { dayToEdit = day },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isToday) "${dayNames[day]} (hoy)" else dayNames[day],
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    slotLabel(code, categories),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (code == Preferences.EMPTY)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showClearWeekDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("Limpiar semana", color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { nav.popBackStack() }, Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }

    // Diálogo de edición de un día
    dayToEdit?.let { day ->
        AlertDialog(
            onDismissRequest = { dayToEdit = null },
            title = { Text(dayNames[day]) },
            text = {
                Column {
                    DayOption("Descanso") { setDay(day, Preferences.REST); dayToEdit = null }
                    categories.forEach { cat ->
                        DayOption(cat.name) { setDay(day, cat.id.toInt()); dayToEdit = null }
                    }
                    DayOption("Vaciar día", tint = MaterialTheme.colorScheme.onSurfaceVariant) {
                        setDay(day, Preferences.EMPTY); dayToEdit = null
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { dayToEdit = null }) { Text("Cancelar") } }
        )
    }

    if (showClearWeekDialog) {
        AlertDialog(
            onDismissRequest = { showClearWeekDialog = false },
            title = { Text("¿Limpiar la semana?") },
            text = { Text("Se quitará la asignación de todos los días.") },
            confirmButton = {
                TextButton(onClick = {
                    plan = List(7) { Preferences.EMPTY }
                    prefs.weeklyPlan = plan
                    showClearWeekDialog = false
                }) { Text("Limpiar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearWeekDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DayOption(label: String, tint: Color? = null, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
