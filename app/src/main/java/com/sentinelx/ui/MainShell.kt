package com.sentinelx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sentinelx.ui.theme.*

data class NavTab(val route: String, val emoji: String, val label: String)

@Composable
fun MainShell(
    viewModel: ScanViewModel,
    navController: NavController,
    startTab: String
) {
    val tabs = listOf(
        NavTab(Routes.DASHBOARD, "🏠", "Dashboard"),
        NavTab(Routes.REPORTS,   "📊", "Reports"),
        NavTab(Routes.SETTINGS,  "⚙️", "Settings")
    )
    var selected by remember { mutableStateOf(startTab) }

    Scaffold(
        containerColor = BgPrimary,
        bottomBar = {
            NavigationBar(
                containerColor = BgSecondary,
                tonalElevation = 0.dp,
                modifier = Modifier.height(72.dp)
            ) {
                tabs.forEach { tab ->
                    val active = selected == tab.route
                    NavigationBarItem(
                        selected = active,
                        onClick = { selected = tab.route },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(tab.emoji, fontSize = 20.sp)
                            }
                        },
                        label = {
                            Text(
                                tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) NeonGreen else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selected) {
                Routes.DASHBOARD -> DashboardScreen(viewModel, navController)
                Routes.REPORTS   -> ReportsScreen(viewModel)
                Routes.SETTINGS  -> SettingsScreen(viewModel)
            }
        }
    }
}