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
val BookColor = Color(0xffa88764)
val BestColor = Color(0xFF18A304)
val BlunderColor = Color(0xfff9412d)
val GoodColor = Color(0xff95b776)
val InaccuracyColor = Color(0xfff8c630)
val MistakeColor = Color(0xffffa459)
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