package com.lattice.notes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val Ink = Color(0xFF1D2928)
val Moss = Color(0xFF496A61)
val Terracotta = Color(0xFFC96F48)
val Paper = Color(0xFFF8F6F0)
val WarmSurface = Color(0xFFFFFDF8)
val Hairline = Color(0xFFE5E0D6)

private val LightColors = lightColorScheme(
    primary = Moss,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E7E1),
    onPrimaryContainer = Ink,
    secondary = Terracotta,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = WarmSurface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFECE4),
    onSurfaceVariant = Color(0xFF596461),
    outline = Color(0xFF7A8581),
    outlineVariant = Hairline,
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FCFC0),
    onPrimary = Color(0xFF08372F),
    primaryContainer = Color(0xFF2E5148),
    onPrimaryContainer = Color(0xFFD9E7E1),
    secondary = Color(0xFFF3B392),
    onSecondary = Color(0xFF55200B),
    background = Color(0xFF111817),
    onBackground = Color(0xFFE3E9E6),
    surface = Color(0xFF171F1E),
    onSurface = Color(0xFFE3E9E6),
    surfaceVariant = Color(0xFF28312F),
    onSurfaceVariant = Color(0xFFBBC6C2),
    outline = Color(0xFF85918D),
    outlineVariant = Color(0xFF37413E)
)

private val AppTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp)
)

@Composable
fun LatticeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
