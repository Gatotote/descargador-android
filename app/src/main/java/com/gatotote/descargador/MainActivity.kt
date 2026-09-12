package com.gatotote.descargador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gatotote.descargador.ui.ajustes.AjustesScreen
import com.gatotote.descargador.ui.descargar.DescargarScreen
import com.gatotote.descargador.ui.historial.HistorialScreen
import com.gatotote.descargador.ui.theme.DescargadorTheme
import com.gatotote.descargador.ui.theme.FondoEscena
import com.gatotote.descargador.ui.theme.NeonAzul
import com.gatotote.descargador.ui.theme.NeonCian

private enum class Pantalla(val ruta: String, val titulo: String, val icono: ImageVector) {
    Descargar("descargar", "Descargar", Icons.Filled.Download),
    Historial("historial", "Historial", Icons.AutoMirrored.Filled.List),
    Ajustes("ajustes", "Ajustes", Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {

    private val pedirNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumirEnlaceCompartido(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { DescargadorTheme { AppRaiz() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumirEnlaceCompartido(intent)
    }

    private fun consumirEnlaceCompartido(intent: Intent?) {
        val texto = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }?.trim()?.takeIf { it.startsWith("http") }
        if (texto != null) {
            (application as DescargadorApp).urlCompartida.value = texto
        }
    }
}

@Composable
private fun AppRaiz() {
    val nav = rememberNavController()
    val entradaActual by nav.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination
    val coloresNav = NavigationBarItemDefaults.colors(
        selectedIconColor = NeonCian,
        selectedTextColor = NeonCian,
        indicatorColor = NeonAzul.copy(alpha = 0.18f),
        unselectedIconColor = Color(0xFF8BA0C2),
        unselectedTextColor = Color(0xFF8BA0C2),
    )

    FondoEscena {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color(0xE6101A2C)) {
                    Pantalla.entries.forEach { p ->
                        val seleccionado = destinoActual?.hierarchy?.any { it.route == p.ruta } == true
                        NavigationBarItem(
                            selected = seleccionado,
                            onClick = {
                                nav.navigate(p.ruta) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = coloresNav,
                            icon = { Icon(p.icono, contentDescription = p.titulo) },
                            label = { Text(p.titulo) },
                        )
                    }
                }
            },
        ) { inner ->
            NavHost(
                navController = nav,
                startDestination = Pantalla.Descargar.ruta,
                modifier = Modifier.padding(inner),
            ) {
                composable(Pantalla.Descargar.ruta) { DescargarScreen() }
                composable(Pantalla.Historial.ruta) { HistorialScreen() }
                composable(Pantalla.Ajustes.ruta) { AjustesScreen() }
            }
        }
    }
}
