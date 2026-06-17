package net.steamcardexchange.dealfinder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Steam-inspired palette
private val SteamBlue = Color(0xFF66C0F4)
private val SteamDarkBlue = Color(0xFF1B2838)
private val SteamSlate = Color(0xFF2A475E)
private val SteamPanel = Color(0xFF22303D)

private val DarkColors = darkColorScheme(
    primary = SteamBlue,
    onPrimary = SteamDarkBlue,
    secondary = Color(0xFF417A9B),
    background = SteamDarkBlue,
    onBackground = Color(0xFFE6EEF5),
    surface = SteamPanel,
    onSurface = Color(0xFFE6EEF5),
    surfaceVariant = SteamSlate,
    onSurfaceVariant = Color(0xFFB9C7D4)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6FA8),
    secondary = Color(0xFF417A9B),
    background = Color(0xFFF3F6F9),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun SteamCardDealFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
