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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.d13.xgym.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.d13.xgym.data.AppDatabase
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color

fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

fun formatHMS(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%d:%02d:%02d".format(h, m, s)
}

@Composable
fun HomeScreen(nav: NavController, vm: WorkoutViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showActiveSessionPrompt by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text("xGym", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(48.dp))
            Button(onClick = { 
                if (ui.sessionId != null) {
                    showActiveSessionPrompt = true
                } else {
                    nav.navigate("categories")
                }
            }, Modifier.fillMaxWidth()) {
                Text("Iniciar entrenamiento", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { nav.navigate("history") }, Modifier.fillMaxWidth()) {
                Text("Historial")
            }
        }
    }

    if (showActiveSessionPrompt) {
        AlertDialog(
            onDismissRequest = { showActiveSessionPrompt = false },
            title = { Text("Sesión activa") },
            text = { Text("Actualmente tienes un entrenamiento en curso. ¿Deseas retomarlo o empezar uno nuevo?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resumeWorkoutService()
                    nav.navigate("workout")
                    showActiveSessionPrompt = false
                }) { Text("Retomar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.cancelCurrentSession()
                    nav.navigate("categories")
                    showActiveSessionPrompt = false
                }) { Text("Empezar nuevo") }
            }
        )
    }
}

@Composable
fun CategoryScreen(nav: NavController, vm: WorkoutViewModel, prefs: com.d13.xgym.data.Preferences) {
    val categories by vm.catalogDao.categories().collectAsStateWithLifecycle(emptyList())

    // Categorías planeadas para hoy: si el día tiene alguna, esas van primero (alfabético)
    // y el resto se atenúa (pero sigue clicable).
    val todayIndex = java.time.LocalDate.now().dayOfWeek.value - 1
    val todaySet = remember { prefs.weeklyPlan[todayIndex].categoryIds.toSet() }
    val hasPlan = todaySet.isNotEmpty()

    val orderedCategories = if (hasPlan) {
        val planned = categories.filter { it.id in todaySet }.sortedBy { it.name }
        val rest = categories.filter { it.id !in todaySet }
        planned + rest
    } else categories

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Text("Elige categoría", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        DraggableList(
            items = orderedCategories,
            key = { it.id },
            onReorder = { vm.reorderCategories(it) }
        ) { cat, dragModifier, isDragged ->
            val inPlan = cat.id in todaySet
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .zIndex(if (isDragged) 1f else 0f)
                    .then(dragModifier)
            ) {
                TextButton(
                    onClick = { nav.navigate("subcategories/${cat.id}") },
                    Modifier.fillMaxWidth()
                ) {
                    Text(
                        cat.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            !hasPlan -> MaterialTheme.colorScheme.onSurface
                            inPlan -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SubcategoryScreen(nav: NavController, vm: WorkoutViewModel, categoryId: Long) {
    val subs by vm.catalogDao.subcategories(categoryId).collectAsStateWithLifecycle(emptyList())

    // Categorías con una sola subcategoría saltan directo a ejercicios.
    if (subs.size == 1) {
        androidx.compose.runtime.LaunchedEffect(subs) {
            nav.navigate("exercises/$categoryId/${subs[0].id}") {
                popUpTo("subcategories/{categoryId}") { inclusive = true }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Text("Elige subcategoría", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        DraggableList(
            items = subs,
            key = { it.id },
            onReorder = { vm.reorderSubcategories(it) }
        ) { sub, dragModifier, isDragged ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .zIndex(if (isDragged) 1f else 0f)
                    .then(dragModifier)
            ) {
                TextButton(
                    onClick = { nav.navigate("exercises/$categoryId/${sub.id}") },
                    Modifier.fillMaxWidth()
                ) { Text(sub.name, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
fun ExerciseScreen(nav: NavController, vm: WorkoutViewModel, categoryId: Long, subcategoryId: Long) {
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val exercises by AppDatabase.get(context).catalogDao()
        .exercises(subcategoryId).collectAsStateWithLifecycle(emptyList<com.d13.xgym.data.Exercise>())

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var exerciseToDelete by remember { mutableStateOf<com.d13.xgym.data.Exercise?>(null) }
    var exerciseToRename by remember { mutableStateOf<com.d13.xgym.data.Exercise?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Text("Elige ejercicio", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        DraggableList(
            items = exercises,
            key = { it.id },
            onReorder = { vm.reorderExercises(it) },
            footer = {
                OutlinedButton(onClick = { showAdd = true }, Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("+ Agregar ejercicio")
                }
            }
        ) { ex, dragModifier, isDragged ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    when (it) {
                        SwipeToDismissBoxValue.EndToStart -> exerciseToDelete = ex
                        SwipeToDismissBoxValue.StartToEnd -> {
                            exerciseToRename = ex
                            renameText = ex.name
                        }
                        else -> {}
                    }
                    false // Snap back, wait for dialog confirmation
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = !isDragged,
                enableDismissFromEndToStart = !isDragged,
                backgroundContent = {
                    // Solo mostrar icono y fondo durante un swipe.
                    // En reposo, currentValue == targetValue == Settled (progress == 1f),
                    // por eso no se puede usar `progress` para ocultarlo.
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> Box(
                            Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp)
                                .background(Color.Red),
                            Alignment.CenterEnd
                        ) {
                            Text("🗑️", Modifier.padding(end = 24.dp), style = MaterialTheme.typography.headlineMedium)
                        }
                        SwipeToDismissBoxValue.StartToEnd -> Box(
                            Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp)
                                .background(Color(0xFF2196F3)),
                            Alignment.CenterStart
                        ) {
                            Text("✏️", Modifier.padding(start = 24.dp), style = MaterialTheme.typography.headlineMedium)
                        }
                        else -> {}
                    }
                },
                modifier = Modifier.zIndex(if (isDragged) 1f else 0f)
            ) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .then(dragModifier)
                ) {
                    TextButton(
                        onClick = {
                            vm.selectExercise(categoryId, subcategoryId, ex.id, ex.name)
                            nav.navigate("workout")
                        },
                        Modifier.fillMaxWidth()
                    ) { Text(ex.name, style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Nuevo ejercicio") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nombre") })
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            val nextOrderIndex = if (exercises.isEmpty()) 0 else (exercises.maxOf { it.orderIndex } + 1)
                            vm.catalogDao.insertExercise(
                                com.d13.xgym.data.Exercise(subcategoryId = subcategoryId, name = name, orderIndex = nextOrderIndex)
                            )
                        }
                    }
                    newName = ""
                    showAdd = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancelar") } }
        )
    }

    if (exerciseToDelete != null) {
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Eliminar ejercicio") },
            text = { Text("¿Estás seguro de que deseas eliminar '${exerciseToDelete?.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteExercise(exerciseToDelete!!)
                    exerciseToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (exerciseToRename != null) {
        AlertDialog(
            onDismissRequest = { exerciseToRename = null },
            title = { Text("Renombrar ejercicio") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ex = exerciseToRename!!
                    val name = renameText.trim()
                    if (name.isNotEmpty() && name != ex.name) {
                        vm.renameExercise(ex, name)
                    }
                    exerciseToRename = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToRename = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun ListScaffold(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        LazyColumn(content = content)
    }
}
