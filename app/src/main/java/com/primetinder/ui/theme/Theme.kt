package com.primetinder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PrimeBackground = Color(0xFF101820)
val PrimeBackgroundTop = Color(0xFF16222B)
val PrimeSurface = Color(0xFF1B2730)
val PrimeTrack = Color(0xFF2D3A42)
val PrimeAccent = Color(0xFF00C2A8)
val PrimePositive = Color(0xFF62F2C2)
val PrimeNegative = Color(0xFFFF7A90)
val PrimeTextPrimary = Color(0xFFF7F8FA)
val PrimeTextMuted = Color(0xFF7F93A1)
val PrimeCard = Color(0xFFFFFFFF)

// Bottom stop of the card gradient: a hair off-white so the card reads as a
// physical surface rather than a flat rectangle.
val PrimeCardBottom = Color(0xFFF2F0EB)
val PrimeCardEdge = Color(0xFFE4E1DA)
val PrimeInk = Color(0xFF101820)

// Darkened accents for text/stamps drawn on the white card.
val PrimePositiveInk = Color(0xFF00806E)
val PrimeNegativeInk = Color(0xFFC63A52)

/**
 * Tabular figures keep the score from shifting sideways as digits change.
 * Also used by the card number so a dealt card never re-centres itself.
 */
val PrimeScoreStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    fontSize = 30.sp,
    letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum",
)

val PrimeCardNumberStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    fontSize = 88.sp,
    letterSpacing = (-3).sp,
    fontFeatureSettings = "tnum",
)

private val PrimeColors = darkColorScheme(
    primary = PrimeAccent,
    onPrimary = PrimeBackground,
    secondary = PrimePositive,
    onSecondary = PrimeBackground,
    background = PrimeBackground,
    onBackground = PrimeTextPrimary,
    surface = PrimeSurface,
    onSurface = PrimeTextPrimary,
    surfaceVariant = PrimeTrack,
    onSurfaceVariant = PrimeTextMuted,
    error = PrimeNegative,
    onError = PrimeBackground,
    outline = PrimeTrack,
)

private val PrimeTypography = Typography(
    displayLarge = PrimeCardNumberStyle,
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        fontFeatureSettings = "tnum",
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        fontFeatureSettings = "tnum",
    ),
)

@Composable
fun PrimeTinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PrimeColors,
        typography = PrimeTypography,
        content = content,
    )
}
