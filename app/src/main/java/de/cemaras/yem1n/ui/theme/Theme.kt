package de.cemaras.yem1n.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NavyFarben = darkColorScheme(
    primary = Gelb,
    onPrimary = TiefNavy,
    secondary = Himmel,
    onSecondary = TiefNavy,
    tertiary = Mint,
    onTertiary = TiefNavy,
    background = Navy,
    onBackground = Color.White,
    surface = Navy,
    onSurface = Color.White,
    surfaceVariant = Glanz,
    onSurfaceVariant = Color.White,
    error = Koralle,
    onError = TiefNavy,
)

/** Die App ist immer dunkelblau mit gelben Knöpfen, auch wenn das Handy hell eingestellt ist. */
@Composable
fun YEM1NTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NavyFarben,
        typography = Typography,
        content = content,
    )
}
