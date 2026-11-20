package com.zodiary.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zodiary.app.R

private val VariableFontWeights = listOf(
    FontWeight.W100,
    FontWeight.W200,
    FontWeight.W300,
    FontWeight.W400,
    FontWeight.W500,
    FontWeight.W600,
    FontWeight.W700,
    FontWeight.W800,
    FontWeight.W900,
)

private fun variableFontFamily(resourceId: Int): FontFamily = FontFamily(
    *VariableFontWeights.map { weight ->
        Font(
            resId = resourceId,
            weight = weight,
            variationSettings = FontVariation.Settings(weight, FontStyle.Normal),
        )
    }.toTypedArray(),
)

val FrauncesFontFamily = variableFontFamily(R.font.fraunces_variable)
val DmSansFontFamily = variableFontFamily(R.font.dm_sans_variable)

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

object ZodiaryShapes {
    val Hero = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
    val Card = RoundedCornerShape(22.dp)
    val CardLarge = RoundedCornerShape(28.dp)
    val Pill = CircleShape
    val Input = RoundedCornerShape(13.dp)
}

object ZodiarySpacing {
    val ScreenInset = 24.dp
    val CardInset = 18.dp
    val SectionGap = 20.dp
    val HeroMinHeight = 200.dp
    val ActionHeight = 48.dp
    val NavItemSize = 52.dp
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
    val defaults = Typography()
    MaterialTheme(
        colorScheme = ZodiaryScheme,
        typography = Typography(
            displayLarge = defaults.displayLarge.copy(fontFamily = FrauncesFontFamily),
            displayMedium = defaults.displayMedium.copy(fontFamily = FrauncesFontFamily),
            displaySmall = defaults.displaySmall.copy(fontFamily = FrauncesFontFamily),
            headlineLarge = defaults.headlineLarge.copy(fontFamily = FrauncesFontFamily),
            headlineMedium = defaults.headlineMedium.copy(fontFamily = FrauncesFontFamily),
            headlineSmall = defaults.headlineSmall.copy(fontFamily = FrauncesFontFamily),
            titleLarge = defaults.titleLarge.copy(fontFamily = FrauncesFontFamily),
            titleMedium = defaults.titleMedium.copy(fontFamily = FrauncesFontFamily),
            titleSmall = defaults.titleSmall.copy(fontFamily = FrauncesFontFamily),
            bodyLarge = defaults.bodyLarge.copy(fontFamily = DmSansFontFamily),
            bodyMedium = defaults.bodyMedium.copy(fontFamily = DmSansFontFamily),
            bodySmall = defaults.bodySmall.copy(fontFamily = DmSansFontFamily),
            labelLarge = defaults.labelLarge.copy(fontFamily = DmSansFontFamily),
            labelMedium = defaults.labelMedium.copy(fontFamily = DmSansFontFamily),
            labelSmall = defaults.labelSmall.copy(fontFamily = DmSansFontFamily),
        ),
        content = content,
    )
}
