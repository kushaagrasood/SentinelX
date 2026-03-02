package com.sentinelx.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val NeonGreen  = Color(0xFF00FFA3)
val NeonOrange = Color(0xFFFF8A00)
val NeonRed    = Color(0xFFFF3B3B)
val NeonPurple = Color(0xFF9D4EDD)

val BgPrimary   = Color(0xFF0D0D0D)
val BgSecondary = Color(0xFF151515)
val BgCard      = Color(0xFF1F1F1F)
val BgCardAlt   = Color(0xFF252525)

val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
val TextMuted     = Color(0xFF666666)

private val DarkColorScheme = darkColorScheme(
    primary        = NeonGreen,
    secondary      = NeonOrange,
    tertiary       = NeonPurple,
    background     = BgPrimary,
    surface        = BgCard,
    onPrimary      = Color.Black,
    onSecondary    = Color.Black,
    onBackground   = TextPrimary,
    onSurface      = TextPrimary,
    error          = NeonRed,
)

@Composable
fun SentinelXTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}

fun riskColor(level: String): Color = when (level) {
    "HIGH"   -> NeonRed
    "MEDIUM" -> NeonOrange
    else     -> NeonGreen
}

fun riskColorByScore(score: Int): Color = when {
    score >= 61 -> NeonRed
    score >= 31 -> NeonOrange
    else        -> NeonGreen
}