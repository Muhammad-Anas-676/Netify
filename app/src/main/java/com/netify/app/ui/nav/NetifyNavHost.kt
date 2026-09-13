package com.netify.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.netify.app.di.AppContainer
import com.netify.app.ui.history.HistoryScreen
import com.netify.app.ui.settings.SettingsScreen
import com.netify.app.ui.speed.SpeedScreen
import com.netify.app.ui.vault.QrScanScreen
import com.netify.app.ui.vault.VaultScreen
import com.netify.app.ui.wifi.WifiScreen

private sealed class Dest(val route: String, val label: String) {
    data object Speed : Dest("speed", "Speed")
    data object Wifi : Dest("wifi", "WiFi")
    data object History : Dest("history", "History")
    data object Vault : Dest("vault", "Vault")
    data object Settings : Dest("settings", "Settings")
    data object QrScan : Dest("qr_scan", "Scan QR")
}

private val bottomItems = listOf(Dest.Speed, Dest.Wifi, Dest.History, Dest.Vault, Dest.Settings)

@Composable
fun NetifyNavHost(container: AppContainer) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                bottomItems.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconFor(dest), contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Speed.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Dest.Speed.route) { SpeedScreen(container) }
            composable(Dest.Wifi.route) { WifiScreen(container) }
            composable(Dest.History.route) { HistoryScreen(container) }
            composable(Dest.Vault.route) {
                VaultScreen(container, onScanQr = { navController.navigate(Dest.QrScan.route) })
            }
            composable(Dest.Settings.route) { SettingsScreen(container) }
            composable(Dest.QrScan.route) {
                QrScanScreen(container, onDone = { navController.popBackStack() })
            }
        }
    }
}

private fun iconFor(dest: Dest) = when (dest) {
    Dest.Speed -> Icons.Filled.Speed
    Dest.Wifi -> Icons.Filled.Wifi
    Dest.History -> Icons.Filled.History
    Dest.Vault -> Icons.Filled.Lock
    Dest.Settings -> Icons.Filled.Settings
    Dest.QrScan -> Icons.Filled.Speed
}
