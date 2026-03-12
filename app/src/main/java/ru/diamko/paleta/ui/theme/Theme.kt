package ru.diamko.paleta.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandViolet,
    onPrimary = LightSurface,
    secondary = BrandBlue,
    onSecondary = LightSurface,
    tertiary = BrandCoral,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = Color(0xFFEBEFF8),
    outline = LightOutline,
    error = Color(0xFFCE2B4D),
    onError = LightSurface,
)

private val DarkColors = darkColorScheme(
    primary = BrandBlue,
    onPrimary = DarkSurface,
    secondary = BrandViolet,
    onSecondary = DarkOnSurface,
    tertiary = BrandCoral,
    onTertiary = DarkSurface,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = Color(0xFF1A2040),
    outline = DarkOutline,
    error = Color(0xFFFF7E97),
    onError = DarkSurface,
)

@Composable
fun PaletaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
