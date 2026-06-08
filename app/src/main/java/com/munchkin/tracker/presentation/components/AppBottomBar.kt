package com.munchkin.tracker.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.munchkin.tracker.presentation.navigation.Routes
import com.munchkin.tracker.ui.theme.*

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.GAME, "Игра", Icons.Default.SportsEsports),
    BottomNavItem(Routes.STATISTICS, "Статистика", Icons.Default.BarChart),
    BottomNavItem(Routes.PLAYER_MANAGEMENT, "Игроки", Icons.Default.People),
    BottomNavItem(Routes.SETTINGS, "Настройки", Icons.Default.Settings),
)

@Composable
fun AppBottomBar(navController: NavController) {
    val currentRoute = navController.currentDestination?.route

    NavigationBar(
        containerColor = Surface,
        contentColor = Primary,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (item.route != currentRoute) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.GAME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    unselectedIconColor = OnSurfaceVariant,
                    unselectedTextColor = OnSurfaceVariant,
                    indicatorColor = Primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}