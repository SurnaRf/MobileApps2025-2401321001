package bg.uni_plovdiv.fmi.everquill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import bg.uni_plovdiv.fmi.everquill.ui.navigation.MainScreen
import bg.uni_plovdiv.fmi.everquill.ui.screens.SplashScreen
import bg.uni_plovdiv.fmi.everquill.ui.theme.EverquillTheme
import bg.uni_plovdiv.fmi.everquill.util.AppThemeMode
import bg.uni_plovdiv.fmi.everquill.util.ThemePreferenceManager

class MainActivity : ComponentActivity() {
    private lateinit var themeManager: ThemePreferenceManager
    private var lastThemeMode: AppThemeMode = AppThemeMode.SYSTEM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeManager = ThemePreferenceManager(this)
        lastThemeMode = themeManager.getThemeMode()
        enableEdgeToEdge()
        setContent {
            val currentTheme = themeManager.themeMode.collectAsState(initial = lastThemeMode).value
            val useDarkTheme = when (currentTheme) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            var showSplash by remember { mutableStateOf(true) }

            EverquillTheme(darkTheme = useDarkTheme) {
                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    MainScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val current = themeManager.getThemeMode()
        if (current != lastThemeMode) {
            lastThemeMode = current
            recreate()
        }
    }
}
