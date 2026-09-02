package com.gatotote.descargador

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

private enum class Pantalla(val ruta: String, val titulo: String, val icono: ImageVector) {
    Descargar("descargar", "Descargar", Icons.Filled.Download),
    Historial("historial", "Historial", Icons.AutoMirrored.Filled.List),
    Ajustes("ajustes", "Ajustes", Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumirEnlaceCompartido(intent)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRaiz() {
    val nav = rememberNavController()
    val entradaActual by nav.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val titulo = Pantalla.entries
                .firstOrNull { p -> destinoActual?.hierarchy?.any { it.route == p.ruta } == true }
                ?.titulo ?: "Descargador"
            TopAppBar(title = { Text(titulo) })
        },
        bottomBar = {
            NavigationBar {
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
