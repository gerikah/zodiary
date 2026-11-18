package com.zodiary.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object ZodiaryColors {
    val Canvas = Color(0xFFF7F3FB)
    val Purple = Color(0xFF5B45A8)
    val DeepPurple = Color(0xFF433D7D)
    val Lavender = Color(0xFFE1D1FE)
    val FormLavender = Color(0xFFF0E9FD)
    val Gold = Color(0xFFE3B866)
    val Ink = Color(0xFF2C254C)
    val Muted = Color(0xFF6E6879)
    val White = Color(0xFFFFFFFF)
    val Border = Color(0xFFD9D1E5)
    val SoftRose = Color(0xFFF5E7E4)
    val SoftGreen = Color(0xFFE5EFE7)
}

private val ZodiaryScheme = lightColorScheme(
    primary = ZodiaryColors.Purple,
    onPrimary = ZodiaryColors.White,
    secondary = ZodiaryColors.Gold,
    onSecondary = ZodiaryColors.Ink,
    background = ZodiaryColors.Canvas,
    onBackground = ZodiaryColors.Ink,
    surface = ZodiaryColors.White,
    onSurface = ZodiaryColors.Ink,
    surfaceVariant = ZodiaryColors.FormLavender,
    onSurfaceVariant = ZodiaryColors.Ink,
    outline = ZodiaryColors.Border,
    error = Color(0xFF9C3E4B),
)

@Composable
fun ZodiaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZodiaryScheme,
        typography = Typography(
            bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(fontFamily = FontFamily.SansSerif),
            bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            bodySmall = androidx.compose.material3.Typography().bodySmall.copy(fontFamily = FontFamily.SansSerif),
            titleLarge = androidx.compose.material3.Typography().titleLarge.copy(fontFamily = FontFamily.SansSerif),
            titleMedium = androidx.compose.material3.Typography().titleMedium.copy(fontFamily = FontFamily.SansSerif),
            labelLarge = androidx.compose.material3.Typography().labelLarge.copy(fontFamily = FontFamily.SansSerif),
        ),
        content = content,
    )
}
