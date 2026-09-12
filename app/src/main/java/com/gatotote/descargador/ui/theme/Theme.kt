package com.gatotote.descargador.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val NeonAzul = Color(0xFF4DA3FF)
val NeonCian = Color(0xFF22D3EE)
val NeonInk = Color(0xFF03101C)
val VidrioBorde = Color(0x334DA3FF)

val DegradadoMarca = Brush.linearGradient(listOf(NeonCian, NeonAzul, Color(0xFF6366F1)))
val DegradadoFondo = Brush.verticalGradient(
    listOf(Color(0xFF0A1222), Color(0xFF070B14), Color(0xFF05080F)),
)

private val Oscuro = darkColorScheme(
    primary = NeonAzul,
    onPrimary = NeonInk,
    primaryContainer = Color(0xFF123056),
    onPrimaryContainer = Color(0xFFB8D9FF),
    secondary = NeonCian,
    onSecondary = Color(0xFF003440),
    secondaryContainer = Color(0xFF0B3A48),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = Color(0xFF818CF8),
    onTertiary = Color(0xFF1A1340),
    background = Color(0xFF070B14),
    onBackground = Color(0xFFE8F0FF),
    surface = Color(0xFF0E1624),
    onSurface = Color(0xFFE8F0FF),
    surfaceVariant = Color(0xFF152033),
    onSurfaceVariant = Color(0xFF8BA0C2),
    surfaceContainerLowest = Color(0xFF05080F),
    surfaceContainerLow = Color(0xFF0B1220),
    surfaceContainer = Color(0xFF101A2C),
    surfaceContainerHigh = Color(0xFF162238),
    surfaceContainerHighest = Color(0xFF1C2B44),
    outline = Color(0xFF2A3F63),
    outlineVariant = Color(0xFF1A2A44),
    error = Color(0xFFFB7185),
    onError = Color(0xFF3B0711),
    errorContainer = Color(0xFF4C1520),
    onErrorContainer = Color(0xFFFFD5DB),
)

private val Tipo: Typography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(letterSpacing = 0.4.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(letterSpacing = 0.3.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(letterSpacing = 0.2.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(letterSpacing = 0.2.sp, fontWeight = FontWeight.Medium),
        labelLarge = labelLarge.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(letterSpacing = 0.6.sp),
        labelSmall = labelSmall.copy(letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun DescargadorTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = Oscuro, typography = Tipo, content = content)
}
