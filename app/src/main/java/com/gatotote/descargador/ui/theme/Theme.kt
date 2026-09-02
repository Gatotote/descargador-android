package com.gatotote.descargador.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Azul = Color(0xFF3B82F6)
private val AzulHondo = Color(0xFF2563EB)
private val AzulClaro = Color(0xFF93C5FD)

private val EsquemaOscuro = darkColorScheme(
    primary = Azul,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A2740),
    onPrimaryContainer = AzulClaro,
    secondary = AzulClaro,
    background = Color(0xFF0F131A),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF171B24),
    onSurface = Color(0xFFE8EDF5),
    surfaceVariant = Color(0xFF1E2433),
    onSurfaceVariant = Color(0xFF9AA6BC),
    outline = Color(0xFF2A3142),
    error = Color(0xFFEF4444),
)

private val EsquemaClaro = lightColorScheme(
    primary = AzulHondo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFE),
    onPrimaryContainer = Color(0xFF0B2A5B),
    secondary = AzulHondo,
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF11151C),
    surface = Color.White,
    onSurface = Color(0xFF11151C),
    surfaceVariant = Color(0xFFEEF2F8),
    onSurfaceVariant = Color(0xFF5B6B82),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
)

@Composable
fun DescargadorTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    colorDinamico: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val esquema = when {
        colorDinamico && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (oscuro) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        oscuro -> EsquemaOscuro
        else -> EsquemaClaro
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = esquema.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !oscuro
        }
    }
    MaterialTheme(colorScheme = esquema, content = content)
}
