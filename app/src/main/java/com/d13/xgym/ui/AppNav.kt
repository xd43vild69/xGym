package com.d13.xgym.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.d13.xgym.viewmodel.WorkoutViewModel

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val vm: WorkoutViewModel = viewModel()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
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
        composable("history") { HistoryScreen(nav) }
    }
}
