package com.d13.xgym.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.d13.xgym.data.Category
import com.d13.xgym.data.DayPlan
import com.d13.xgym.data.Preferences
import com.d13.xgym.ui.theme.Outline
import com.d13.xgym.viewmodel.WorkoutViewModel
import java.time.LocalDate

private val dayNames = listOf(
    "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
)

private fun dayLabel(day: DayPlan, categories: List<Category>): String = when {
    day.rest -> "Descanso"
    day.categoryIds.isEmpty() -> "— Sin asignar"
    else -> day.categoryIds
        .mapNotNull { id -> categories.firstOrNull { it.id == id }?.name }
        .joinToString(", ")
        .ifEmpty { "— Sin asignar" }
}

@Composable
fun WeeklyPlanScreen(nav: NavController, prefs: Preferences, vm: WorkoutViewModel) {
    var plan by remember { mutableStateOf(prefs.weeklyPlan) }
    var dayToEdit by remember { mutableStateOf<Int?>(null) }
    var showClearWeekDialog by remember { mutableStateOf(false) }
    val categories by vm.catalogDao.categories().collectAsStateWithLifecycle(emptyList())

    val todayIndex = LocalDate.now().dayOfWeek.value - 1 // 0=Lunes … 6=Domingo

    fun setDay(day: Int, value: DayPlan) {
        plan = plan.toMutableList().also { it[day] = value }
        prefs.weeklyPlan = plan
    }

    val trainingCount = plan.count { it.categoryIds.isNotEmpty() }
    val restCount = plan.count { it.rest }

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
                val dayPlan = plan[day]
                val isToday = day == todayIndex
                val barColor = when {
                    dayPlan.categoryIds.isNotEmpty() -> MaterialTheme.colorScheme.primary
                    dayPlan.rest -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> Outline
                }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(56.dp),
                    colors = CardDefaults.cardColors(),
                    border = if (isToday)
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else null
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Barra de acento
                        Box(
                            Modifier
                                .width(6.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                                .background(barColor)
                        )
                        TextButton(
                            onClick = { dayToEdit = day },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    if (isToday) "${dayNames[day]} (hoy)" else dayNames[day],
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    dayLabel(dayPlan, categories),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (dayPlan.categoryIds.isEmpty() && !dayPlan.rest)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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

    // Diálogo de edición de un día (multi-selección)
    dayToEdit?.let { day ->
        DayEditDialog(
            dayName = dayNames[day],
            categories = categories,
            initial = plan[day],
            onDismiss = { dayToEdit = null },
            onSave = { updated ->
                setDay(day, updated)
                dayToEdit = null
            }
        )
    }

    if (showClearWeekDialog) {
        AlertDialog(
            onDismissRequest = { showClearWeekDialog = false },
            title = { Text("¿Limpiar la semana?") },
            text = { Text("Se quitará la asignación de todos los días.") },
            confirmButton = {
                TextButton(onClick = {
                    plan = List(7) { DayPlan() }
                    prefs.weeklyPlan = plan
                    showClearWeekDialog = false
                }) { Text("Limpiar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearWeekDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DayEditDialog(
    dayName: String,
    categories: List<Category>,
    initial: DayPlan,
    onDismiss: () -> Unit,
    onSave: (DayPlan) -> Unit
) {
    var editRest by remember { mutableStateOf(initial.rest) }
    var editSelected by remember { mutableStateOf(initial.categoryIds.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dayName) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CheckRow(
                    label = "Descanso",
                    checked = editRest,
                    onToggle = {
                        editRest = !editRest
                        if (editRest) editSelected = emptySet()
                    }
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                categories.forEach { cat ->
                    CheckRow(
                        label = cat.name,
                        checked = cat.id in editSelected,
                        onToggle = {
                            editSelected = if (cat.id in editSelected)
                                editSelected - cat.id
                            else editSelected + cat.id
                            if (editSelected.isNotEmpty()) editRest = false
                        }
                    )
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { editRest = false; editSelected = emptySet() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vaciar día", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(DayPlan(rest = editRest, categoryIds = editSelected.toList()))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
