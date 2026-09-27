package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MidnightBrandPrimary,
    onPrimary = Color(0xFF000000),
    background = MidnightBg,
    surface = MidnightBg,
    surfaceVariant = MidnightCard,
    onBackground = MidnightTextMain,
    onSurface = MidnightTextMain,
    outline = MidnightBorder
)

private val LightColorScheme = lightColorScheme(
    primary = LightBrandPrimary,
    onPrimary = Color(0xFFFFFFFF),
    background = LightBg,
    surface = LightBg,
    surfaceVariant = LightCard,
    onBackground = LightTextMain,
    onSurface = LightTextMain,
    outline = LightBorder
)

object ZenTheme {
    val colors: ZenMathColors
        @Composable
        @ReadOnlyComposable
        get() = LocalZenColors.current
}

@Composable
fun ZenMathTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val zenColors = if (darkTheme) DarkZenColors else LightZenColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalZenColors provides zenColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
