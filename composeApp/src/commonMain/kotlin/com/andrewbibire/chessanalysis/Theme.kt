package com.andrewbibire.chessanalysis

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF1a1a1a)
val DarkSurface = Color(0xFF2b2b2b)
val DarkSurfaceVariant = Color(0xFF3a3a3a)
val LightSurface = Color(0xFF4a4a4a)
val PrimaryBlue = Color(0xFF4a90e2)
val PrimaryBlueDark = Color(0xFF357abd)
val TextPrimary = Color(0xFFfafafa)
val TextSecondary = Color(0xFFb0b0b0)
val EvalGreen = Color(0xFF81b64c)
val EvalRed = Color(0xFFe76f51)
val EvalYellow = Color(0xFFf4a261)
val BoardLight = Color(0xFFeeeed2)
val BoardDark = Color(0xFF769656)

val ChessAnalysisDarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPrimary,
    secondary = PrimaryBlueDark,
    onSecondary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary
)