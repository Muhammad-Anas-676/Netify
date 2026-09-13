package com.netify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.netify.app.data.datastore.ThemeMode
import com.netify.app.ui.nav.NetifyNavHost
import com.netify.app.ui.theme.NetifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as NetifyApplication

        setContent {
            // Defaults to LIGHT on first-ever launch (see SettingsDataStore),
            // then always restores whatever the user picked last time.
            val themeMode by app.container.settingsDataStore.themeMode.collectAsState(initial = ThemeMode.LIGHT)
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            NetifyTheme(useDarkTheme = useDark) {
                NetifyNavHost(container = app.container)
            }
        }
    }
}
