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

fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

@Composable
fun HomeScreen(nav: NavController) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
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
    val exercises by vm.catalogDao.exercises(subcategoryId).collectAsStateWithLifecycle(emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    ListScaffold("Elige ejercicio") {
        items(exercises) { ex ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                TextButton(
                    onClick = {
                        vm.selectExercise(categoryId, subcategoryId, ex.id, ex.name)
                        nav.navigate("workout")
                    },
                    Modifier.fillMaxWidth()
                ) { Text(ex.name, style = MaterialTheme.typography.titleMedium) }
            }
        }
        item {
            OutlinedButton(onClick = { showAdd = true }, Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("+ Agregar ejercicio")
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
                            vm.catalogDao.insertExercise(
                                com.d13.xgym.data.Exercise(subcategoryId = subcategoryId, name = name)
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
}

@Composable
fun ListScaffold(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        LazyColumn(content = content)
    }
}
