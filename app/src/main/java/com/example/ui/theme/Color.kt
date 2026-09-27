package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Zen Obsidian — Minimalist Dark Palette
val MidnightBg = Color(0xFF000000)
val MidnightCard = Color(0xFF121214)
val MidnightCardHover = Color(0xFF1C1C1F)
val MidnightBorder = Color(0x1AFFFFFF) // 0.05 - 0.1 opacity
val MidnightTextMain = Color(0xFFFFFFFF)
val MidnightTextSecondary = Color(0xFF71717A)
val MidnightBrandPrimary = Color(0xFFE4E4E7)
val MidnightKeypadBg = Color(0xFF09090B)
val MidnightKeypadBtn = Color(0xFF18181B)
val MidnightKeypadBorder = Color(0x14FFFFFF)
val MidnightKeypadText = Color(0xFFFAFAFA)
val MidnightToggleOn = Color(0xFFE4E4E7)
val MidnightToggleOff = Color(0xFF27272A)
val MidnightToggleThumb = Color(0xFF000000)

// Paper Zen — Light Palette
val LightBg = Color(0xFFF4F4F5)
val LightCard = Color(0xFFFFFFFF)
val LightCardHover = Color(0xFFEEEEF0)
val LightBorder = Color(0x1E000000) // 0.08 opacity
val LightTextMain = Color(0xFF18181B)
val LightTextSecondary = Color(0xFF52525B)
val LightBrandPrimary = Color(0xFF000000)
val LightKeypadBg = Color(0xFFE4E4E7)
val LightKeypadBtn = Color(0xFFFAFAFA)
val LightKeypadBorder = Color(0xFFD4D4D8)
val LightKeypadText = Color(0xFF18181B)
val LightToggleOn = Color(0xFF000000)
val LightToggleOff = Color(0xFFD4D4D8)
val LightToggleThumb = Color(0xFFFFFFFF)

// Shared Feedback Colors
val ColorCorrect = Color(0xFF4ADE80)
val ColorIncorrect = Color(0xFFF87171)
val ColorTimeout = Color(0xFFFB923C)

@Immutable
data class ZenMathColors(
    val isDark: Boolean,
    val surface: Color,
    val card: Color,
    val cardHover: Color,
    val cardBorder: Color,
    val textMain: Color,
    val textSecondary: Color,
    val primary: Color,
    val onPrimary: Color,
    val keypadBg: Color,
    val keypadBtn: Color,
    val keypadBorder: Color,
    val keypadText: Color,
    val toggleOn: Color,
    val toggleOff: Color,
    val toggleThumb: Color,
    val correct: Color = ColorCorrect,
    val incorrect: Color = ColorIncorrect,
    val timeout: Color = ColorTimeout
)

val DarkZenColors = ZenMathColors(
    isDark = true,
    surface = MidnightBg,
    card = MidnightCard,
    cardHover = MidnightCardHover,
    cardBorder = MidnightBorder,
    textMain = MidnightTextMain,
    textSecondary = MidnightTextSecondary,
    primary = MidnightBrandPrimary,
    onPrimary = Color(0xFF000000),
    keypadBg = MidnightKeypadBg,
    keypadBtn = MidnightKeypadBtn,
    keypadBorder = MidnightKeypadBorder,
    keypadText = MidnightKeypadText,
    toggleOn = MidnightToggleOn,
    toggleOff = MidnightToggleOff,
    toggleThumb = MidnightToggleThumb
)

val LightZenColors = ZenMathColors(
    isDark = false,
    surface = LightBg,
    card = LightCard,
    cardHover = LightCardHover,
    cardBorder = LightBorder,
    textMain = LightTextMain,
    textSecondary = LightTextSecondary,
    primary = LightBrandPrimary,
    onPrimary = Color(0xFFFFFFFF),
    keypadBg = LightKeypadBg,
    keypadBtn = LightKeypadBtn,
    keypadBorder = LightKeypadBorder,
    keypadText = LightKeypadText,
    toggleOn = LightToggleOn,
    toggleOff = LightToggleOff,
    toggleThumb = LightToggleThumb
)

val LocalZenColors = staticCompositionLocalOf { DarkZenColors }
