package io.github.sj42tech.route42.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Route42LightColors = lightColorScheme(
    primary = Color(0xFF2B75F0),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF24B8F7),
    onSecondary = Color(0xFF00293A),
    tertiary = Color(0xFF5A4CC9),
    background = Color(0xFFF7F4FC),
    onBackground = Color(0xFF171B27),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF171B27),
    surfaceVariant = Color(0xFFE8ECF7),
    onSurfaceVariant = Color(0xFF454B5C),
    outline = Color(0xFF70788C),
)

private val Route42DarkColors = darkColorScheme(
    primary = Color(0xFF73B4FF),
    onPrimary = Color(0xFF002A61),
    secondary = Color(0xFF5AD7FF),
    onSecondary = Color(0xFF003545),
    tertiary = Color(0xFFC5B7FF),
    background = Color(0xFF071521),
    onBackground = Color(0xFFE6F1FF),
    surface = Color(0xFF0B1D2C),
    onSurface = Color(0xFFE6F1FF),
    surfaceVariant = Color(0xFF13283B),
    onSurfaceVariant = Color(0xFFBCC8D9),
    outline = Color(0xFF8A96A7),
)

@Composable
fun Route42Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) Route42DarkColors else Route42LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
