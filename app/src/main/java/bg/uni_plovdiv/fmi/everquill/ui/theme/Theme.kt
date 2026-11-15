package bg.uni_plovdiv.fmi.everquill.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LightForestGreen,
    onPrimary = DeepForestGreen,
    secondary = MutedGold,
    onSecondary = DeepForestGreen,
    tertiary = LightBurgundy,
    onTertiary = WarmParchment,
    background = DeepForestGreen,
    onBackground = WarmParchment,
    surface = Color(0xFF1F3830),
    onSurface = WarmParchment,
    surfaceVariant = Color(0xFF2A4D40),
    onSurfaceVariant = LightGold
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = WarmParchment,
    secondary = MutedGold,
    onSecondary = DeepForestGreen,
    tertiary = DarkBurgundy,
    onTertiary = WarmParchment,
    background = WarmParchment,
    onBackground = DeepForestGreen,
    surface = CreamSurface,
    onSurface = DeepForestGreen,
    surfaceVariant = Color(0xFFE4D8C3),
    onSurfaceVariant = ForestGreen
)

@Composable
fun EverquillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default to preserve the botanical fantasy color palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
