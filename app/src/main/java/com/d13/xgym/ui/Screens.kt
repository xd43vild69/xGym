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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
fun HomeScreen(nav: NavController) {
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
            Button(onClick = { nav.navigate("categories") }, Modifier.fillMaxWidth()) {
                Text("Iniciar entrenamiento", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { nav.navigate("history") }, Modifier.fillMaxWidth()) {
                Text("Historial")
            }
        }
        OutlinedButton(onClick = { nav.navigate("settings") }, Modifier.fillMaxWidth()) {
            Text("⚙ Ajustes")
        }
    }
}

@Composable
fun CategoryScreen(nav: NavController, vm: WorkoutViewModel) {
    val categories by vm.catalogDao.categories().collectAsStateWithLifecycle(emptyList())
    ListScaffold("Elige categoría") {
        items(categories) { cat ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                TextButton(
                    onClick = { nav.navigate("subcategories/${cat.id}") },
                    Modifier.fillMaxWidth()
                ) { Text(cat.name, style = MaterialTheme.typography.titleMedium) }
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
    ListScaffold("Elige subcategoría") {
        items(subs) { sub ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
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
    val dbExercises by vm.catalogDao.exercises(subcategoryId).collectAsStateWithLifecycle(emptyList())
    var exercises by remember(dbExercises) { mutableStateOf(dbExercises) }
    
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var exerciseToDelete by remember { mutableStateOf<com.d13.xgym.data.Exercise?>(null) }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Text("Elige ejercicio", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        
        LazyColumn {
            itemsIndexed(exercises, key = { _, ex -> ex.id }) { index, ex ->
                val isDragged = index == draggedItemIndex
                val translationY = if (isDragged) dragOffset else 0f
                
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            exerciseToDelete = ex
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
                    },
                    modifier = Modifier.zIndex(if (isDragged) 1f else 0f)
                ) {
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                        .graphicsLayer { this.translationY = translationY }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggedItemIndex = index },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                    
                                    val currentIdx = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                                    val itemHeight = 72.dp.toPx() // Approximation height
                                    
                                    var newIdx = currentIdx
                                    if (dragOffset > itemHeight && currentIdx < exercises.size - 1) {
                                        newIdx = currentIdx + 1
                                        dragOffset -= itemHeight
                                    } else if (dragOffset < -itemHeight && currentIdx > 0) {
                                        newIdx = currentIdx - 1
                                        dragOffset += itemHeight
                                    }
                                    
                                    if (newIdx != currentIdx) {
                                        exercises = exercises.toMutableList().apply {
                                            val item = removeAt(currentIdx)
                                            add(newIdx, item)
                                        }
                                        draggedItemIndex = newIdx
                                    }
                                },
                                onDragEnd = {
                                    draggedItemIndex = null
                                    dragOffset = 0f
                                    vm.reorderExercises(exercises)
                                },
                                onDragCancel = {
                                    draggedItemIndex = null
                                    dragOffset = 0f
                                }
                            )
                        }
                ) {
                    TextButton(
                        onClick = {
                            if (draggedItemIndex == null) {
                                vm.selectExercise(categoryId, subcategoryId, ex.id, ex.name)
                                nav.navigate("workout")
                            }
                        },
                        Modifier.fillMaxWidth()
                    ) { Text(ex.name, style = MaterialTheme.typography.titleMedium) }
                }
                } // End SwipeToDismissBox
            }
            item {
                OutlinedButton(onClick = { showAdd = true }, Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("+ Agregar ejercicio")
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
                    val ex = exerciseToDelete!!
                    exercises = exercises.filter { it.id != ex.id }
                    vm.deleteExercise(ex)
                    exerciseToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) { Text("Cancelar") }
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
