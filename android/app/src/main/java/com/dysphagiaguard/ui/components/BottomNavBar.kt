package com.dysphagiaguard.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dysphagiaguard.ui.theme.*

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Monitor : BottomNavItem("live_monitor", "Monitor", Icons.Default.MonitorHeart)
    object Alert : BottomNavItem("alert_history", "Alerts", Icons.Default.Warning)
    object Report : BottomNavItem("daily_report", "Report", Icons.Default.Assessment)
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(BottomNavItem.Monitor, BottomNavItem.Alert, BottomNavItem.Report)

    NavigationBar(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(32.dp)),
        containerColor = GlassmorphismBackground,
        contentColor = TextPrimary,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PremiumTeal,
                    selectedTextColor = PremiumTeal,
                    indicatorColor = GlassmorphismBorder,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
