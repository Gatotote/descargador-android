package com.gatotote.descargador.ui.historial

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatotote.descargador.DescargadorApp
import com.gatotote.descargador.data.EntradaHistorial
import com.gatotote.descargador.ui.theme.CabeceraPantalla
import com.gatotote.descargador.ui.theme.NeonAzul
import com.gatotote.descargador.ui.theme.NeonCian
import com.gatotote.descargador.ui.theme.SuperficieVidrio
import kotlinx.coroutines.launch

class HistorialViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as DescargadorApp).historial
    val entradas = store.entradas
    fun borrar(e: EntradaHistorial) = viewModelScope.launch { store.borrar(e) }
    fun borrarTodo() = viewModelScope.launch { store.borrarTodo() }
}

@Composable
fun HistorialScreen(
    modifier: Modifier = Modifier,
    vm: HistorialViewModel = viewModel(),
) {
    val lista by vm.entradas.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    fun abrir(e: EntradaHistorial) {
        if (e.uri.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(e.uri), if (e.tipo == "audio") "audio/*" else "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ctx.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(ctx, "No hay una app para abrir este archivo.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        CabeceraPantalla(
            titulo = "Historial",
            subtitulo = "Lo que ya bajaste en este celular.",
        )
        if (lista.isEmpty()) {
            Spacer(Modifier.height(28.dp))
            SuperficieVidrio(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(28.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        tint = NeonCian,
                        modifier = Modifier.size(36.dp),
                    )
                    Text("Aún no hay descargas", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Cuando bajes un vídeo o un MP3, aparece aquí.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = vm::borrarTodo) { Text("Borrar todo") }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(lista) { e ->
                SuperficieVidrio(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth()) {
                        val acento = when {
                            e.estado != "ok" -> MaterialTheme.colorScheme.error
                            e.tipo == "audio" -> NeonCian
                            else -> NeonAzul
                        }
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(96.dp)
                                .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
                                .background(acento),
                        )
                        Column(
                            Modifier.padding(horizontal = 14.dp, vertical = 12.dp).weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                e.titulo,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "${if (e.tipo == "audio") "MP3" else "Vídeo"} · ${e.fecha} · " +
                                    if (e.estado == "ok") "OK" else "Error",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (e.estado == "ok") NeonCian else MaterialTheme.colorScheme.error,
                            )
                            Text(
                                e.detalle,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row {
                                if (e.estado == "ok" && e.uri.isNotBlank()) {
                                    TextButton(onClick = { abrir(e) }) { Text("Abrir") }
                                }
                                TextButton(onClick = { vm.borrar(e) }) { Text("Borrar") }
                            }
                        }
                    }
                }
            }
        }
    }
}
