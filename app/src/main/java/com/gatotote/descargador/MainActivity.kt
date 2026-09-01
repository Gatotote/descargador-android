package com.gatotote.descargador

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SPIKE: sirve solo para comprobar si yt-dlp puede sacar los formatos de un
 * vídeo de YouTube en Android (el problema del reto JavaScript). La UI real
 * llegará después.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val urlInicial = intent?.getStringExtra("url")
            ?: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        setContent {
            val ctx = LocalContext.current
            val scheme = runCatching { dynamicDarkColorScheme(ctx) }
                .getOrElse { dynamicLightColorScheme(ctx) }
            MaterialTheme(colorScheme = scheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    SpikeScreen(urlInicial, Modifier.padding(inner))
                }
            }
        }
    }
}

@Composable
private fun SpikeScreen(urlInicial: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(urlInicial) }
    var salida by remember { mutableStateOf("") }
    var trabajando by remember { mutableStateOf(false) }

    fun probar(clienteAndroid: Boolean) {
        if (trabajando) return
        trabajando = true
        salida = "Consultando…"
        scope.launch {
            val texto = withContext(Dispatchers.IO) {
                try {
                    val version = YoutubeDL.getInstance().version(ctx) ?: "?"
                    val req = YoutubeDLRequest(url).apply {
                        addOption("--no-playlist")
                        if (clienteAndroid) addOption("--extractor-args", "youtube:player_client=android")
                    }
                    val info = YoutubeDL.getInstance().getInfo(req)
                    val fmts = info.formats.orEmpty()
                        .filter { (it.vcodec != null && it.vcodec != "none") || (it.acodec != null && it.acodec != "none") }
                    buildString {
                        appendLine("yt-dlp $version")
                        appendLine("OK  —  ${info.title}")
                        appendLine("Formatos reproducibles: ${fmts.size}")
                        fmts.take(25).forEach {
                            appendLine("  ${it.formatId}  ${it.ext}  ${it.height}p  v=${it.vcodec} a=${it.acodec}")
                        }
                    }
                } catch (e: Throwable) {
                    "ERROR\n${e.message}"
                }
            }
            Log.i("SPIKE", "cliente_android=$clienteAndroid\n$texto")
            salida = texto
            trabajando = false
        }
    }

    // SPIKE: al abrir, actualiza yt-dlp y luego lanza las dos pruebas (capturar por logcat).
    LaunchedEffect(Unit) {
        if (DescargadorApp.motorListo) {
            val upd = withContext(Dispatchers.IO) {
                try {
                    val st = YoutubeDL.getInstance()
                        .updateYoutubeDL(ctx, YoutubeDL.UpdateChannel.STABLE)
                    "update=$st  version=" + YoutubeDL.getInstance().version(ctx)
                } catch (e: Throwable) {
                    "update ERROR: ${e.message}"
                }
            }
            Log.i("SPIKE", upd)
            probar(false)
            while (trabajando) kotlinx.coroutines.delay(300)
            probar(true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Spike yt-dlp", style = MaterialTheme.typography.titleLarge)
        Text(
            if (DescargadorApp.motorListo) "Motor: inicializado"
            else "Motor: FALLÓ  (${DescargadorApp.errorInit})",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL") },
            modifier = Modifier.fillMaxSize(),
        )
        Button(onClick = { probar(false) }, enabled = !trabajando) { Text("Probar getInfo") }
        Button(onClick = { probar(true) }, enabled = !trabajando) { Text("Probar con cliente android") }
        Text(
            text = salida,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
