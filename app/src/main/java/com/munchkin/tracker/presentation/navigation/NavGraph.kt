package com.munchkin.tracker.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.munchkin.tracker.presentation.components.AppBottomBar
import com.munchkin.tracker.presentation.game.GameScreen
import com.munchkin.tracker.presentation.history.HistoryScreen
import com.munchkin.tracker.presentation.players.PlayerDetailScreen
import com.munchkin.tracker.presentation.players.PlayerManagementScreen
import com.munchkin.tracker.presentation.settings.SettingsScreen
import com.munchkin.tracker.presentation.statistics.StatisticsScreen
import com.munchkin.tracker.ui.theme.Background

object Routes {
    const val GAME = "game"
    const val HISTORY = "history"
    const val STATISTICS = "statistics"
    const val PLAYER_MANAGEMENT = "player_management"
    const val PLAYER_DETAIL = "player_detail/{playerId}"
    const val SETTINGS = "settings"

    fun playerDetail(id: Long) = "player_detail/$id"
}

private val screenOrder = listOf(
    Routes.GAME,
    Routes.STATISTICS,
    Routes.PLAYER_MANAGEMENT,
    Routes.SETTINGS
)

@Composable
fun MunchkinNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Routes.GAME, Routes.STATISTICS, Routes.PLAYER_MANAGEMENT, Routes.SETTINGS
    )

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) AppBottomBar(navController)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.GAME,
                enterTransition = {
                    val targetRoute = targetState.destination.route ?: ""
                    val initialRoute = initialState.destination.route ?: ""
                    val targetIndex = screenOrder.indexOf(targetRoute)
                    val initialIndex = screenOrder.indexOf(initialRoute)

                    if (targetIndex > initialIndex) {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                    }
                },
                exitTransition = {
                    val targetRoute = targetState.destination.route ?: ""
                    val initialRoute = initialState.destination.route ?: ""
                    val targetIndex = screenOrder.indexOf(targetRoute)
                    val initialIndex = screenOrder.indexOf(initialRoute)

                    if (targetIndex > initialIndex) {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                    } else {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                    }
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                }
            ) {
                composable(Routes.GAME) { GameScreen(navController = navController) }
                composable(Routes.HISTORY) { HistoryScreen(navController = navController) }
                composable(Routes.STATISTICS) { StatisticsScreen(navController = navController) }
                composable(Routes.PLAYER_MANAGEMENT) { PlayerManagementScreen(navController = navController) }
                composable(
                    route = Routes.PLAYER_DETAIL,
                    arguments = listOf(navArgument("playerId") { type = NavType.LongType })
                ) { backStack ->
                    val playerId = backStack.arguments?.getLong("playerId") ?: return@composable
                    PlayerDetailScreen(playerId = playerId, navController = navController)
                }
                composable(Routes.SETTINGS) { SettingsScreen(navController = navController) }
            }
        }
    }
}