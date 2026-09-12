package com.gatotote.descargador.ui.ajustes

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.gatotote.descargador.ui.theme.BotonNeon
import com.gatotote.descargador.ui.theme.CabeceraPantalla
import com.gatotote.descargador.ui.theme.FilaCalidad
import com.gatotote.descargador.ui.theme.NeonCian
import com.gatotote.descargador.ui.theme.SuperficieVidrio
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

@Composable
fun AjustesScreen(
    modifier: Modifier = Modifier,
    vm: AjustesViewModel = viewModel(),
) {
    val a by vm.ajustes.collectAsState()
    val ctx = LocalContext.current
    var estadoUpd by remember { mutableStateOf("") }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CabeceraPantalla(
            titulo = "Ajustes",
            subtitulo = "Calidad por defecto y el motor yt-dlp.",
        )

        SuperficieVidrio(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("POR DEFECTO", style = MaterialTheme.typography.labelSmall, color = NeonCian)
                FilaCalidad(
                    calidad = a.calidad,
                    soloAudio = a.soloAudio,
                    enabled = true,
                    onCalidad = vm::setCalidad,
                    onSoloAudio = vm::setSoloAudio,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = a.soloAudio,
                        onCheckedChange = vm::setSoloAudio,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCian,
                            checkedTrackColor = NeonCian.copy(alpha = 0.35f),
                        ),
                    )
                    Text("  Solo audio (MP3) por defecto", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        SuperficieVidrio(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MOTOR YT-DLP", style = MaterialTheme.typography.labelSmall, color = NeonCian)
                Text(
                    "Versión: ${YtdlpEngine.version(ctx) ?: "?"}" +
                        if (a.ultimaActualizacionYtdlp.isNotBlank()) "\nÚltima actualización: ${a.ultimaActualizacionYtdlp}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BotonNeon("Actualizar yt-dlp", onClick = { vm.actualizarYtdlp { estadoUpd = it } }, modifier = Modifier.fillMaxWidth())
                if (estadoUpd.isNotBlank()) {
                    Text(estadoUpd, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        SuperficieVidrio(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ARCHIVOS", style = MaterialTheme.typography.labelSmall, color = NeonCian)
                Text(
                    "Los vídeos se guardan en Movies/Descargador y el audio en Music/Descargador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
