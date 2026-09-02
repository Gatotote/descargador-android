package com.gatotote.descargador.ui.ajustes

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatotote.descargador.DescargadorApp
import com.gatotote.descargador.data.Ajustes
import com.gatotote.descargador.data.Calidad
import com.gatotote.descargador.data.YtdlpEngine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AjustesViewModel(app: Application) : AndroidViewModel(app) {
    private val appc = app as DescargadorApp
    private val store = appc.ajustes
    val ajustes = store.flujo.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, Ajustes())

    fun setCalidad(c: Calidad) = viewModelScope.launch { store.setCalidad(c) }
    fun setSoloAudio(v: Boolean) = viewModelScope.launch { store.setSoloAudio(v) }

    fun actualizarYtdlp(onResultado: (String) -> Unit) = viewModelScope.launch {
        onResultado("Actualizando…")
        val r = YtdlpEngine.actualizar(getApplication())
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        store.setUltimaActualizacion(fecha)
        onResultado(r)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    modifier: Modifier = Modifier,
    vm: AjustesViewModel = viewModel(),
) {
    val a by vm.ajustes.collectAsState()
    val ctx = LocalContext.current
    var estadoUpd by remember { mutableStateOf("") }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Calidad por defecto", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            Calidad.entries.forEach { c ->
                FilterChip(
                    selected = a.calidad == c,
                    onClick = { vm.setCalidad(c) },
                    label = { Text(c.etiqueta.substringBefore(" ")) },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = a.soloAudio, onCheckedChange = vm::setSoloAudio)
            Text("  Solo audio (MP3) por defecto")
        }

        Text("Motor yt-dlp", style = MaterialTheme.typography.titleSmall)
        Text(
            "Versión: ${YtdlpEngine.version(ctx) ?: "?"}" +
                if (a.ultimaActualizacionYtdlp.isNotBlank()) "\nÚltima actualización: ${a.ultimaActualizacionYtdlp}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { vm.actualizarYtdlp { estadoUpd = it } }) { Text("Actualizar yt-dlp") }
        if (estadoUpd.isNotBlank()) {
            Text(estadoUpd, style = MaterialTheme.typography.bodySmall)
        }

        Text(
            "Los vídeos se guardan en Movies/Descargador y el audio en Music/Descargador.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
