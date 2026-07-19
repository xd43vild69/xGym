package com.d13.xgym.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.d13.xgym.data.Preferences
import com.d13.xgym.viewmodel.WorkoutViewModel
import androidx.compose.runtime.remember
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val vm: WorkoutViewModel = viewModel()
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }

    val uiState by vm.ui.collectAsState()
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(Modifier.fillMaxSize()) {
        if (uiState.sessionStartTs != null && currentRoute != "workout") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .statusBarsPadding()
                    .clickable { nav.navigate("workout") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entrenamiento activo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatHMS(uiState.sessionElapsedMs),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        NavHost(navController = nav, startDestination = "home", modifier = Modifier.weight(1f)) {
            composable("home") { HomeScreen(nav, vm) }
        composable("categories") { CategoryScreen(nav, vm) }
        composable(
            "subcategories/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) {
            SubcategoryScreen(nav, vm, it.arguments!!.getLong("categoryId"))
        }
        composable(
            "exercises/{categoryId}/{subcategoryId}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType },
                navArgument("subcategoryId") { type = NavType.LongType }
            )
        ) {
            ExerciseScreen(
                nav, vm,
                it.arguments!!.getLong("categoryId"),
                it.arguments!!.getLong("subcategoryId")
            )
        }
        composable("workout") { WorkoutScreen(nav, vm) }
        composable(
            "summary/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) {
            SummaryScreen(nav, it.arguments!!.getLong("sessionId"))
        }
        composable("history") { HistoryScreen(nav, vm) }
        composable("settings") { SettingsScreen(nav, prefs, vm) }
    }
}}
