package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Black,
      fontSize = 52.sp,
      lineHeight = 58.sp,
      letterSpacing = (-1).sp,
      fontFeatureSettings = "tnum"
    ),
    titleLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      lineHeight = 26.sp,
      letterSpacing = (-0.5).sp
    ),
    bodyLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.2.sp,
      fontFeatureSettings = "tnum"
    ),
    labelSmall = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Black,
      fontSize = 10.sp,
      lineHeight = 14.sp,
      letterSpacing = 1.4.sp
    )
  )
