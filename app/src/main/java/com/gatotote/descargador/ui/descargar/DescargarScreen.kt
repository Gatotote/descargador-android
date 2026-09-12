package com.gatotote.descargador.ui.descargar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatotote.descargador.ui.theme.BotonNeon
import com.gatotote.descargador.ui.theme.CabeceraPantalla
import com.gatotote.descargador.ui.theme.FilaCalidad
import com.gatotote.descargador.ui.theme.NeonCian
import com.gatotote.descargador.ui.theme.SuperficieVidrio

@Composable
fun DescargarScreen(
    modifier: Modifier = Modifier,
    vm: DescargarViewModel = viewModel(),
) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val form = e.form
    val t = e.trabajo
    val portapapeles = LocalClipboardManager.current
    val campo = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonCian,
        cursorColor = NeonCian,
        focusedLabelColor = NeonCian,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CabeceraPantalla(
            titulo = "Trae el enlace",
            subtitulo = "Vídeo o solo audio. Lo deja en MP4 o MP3.",
        )

        SuperficieVidrio(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.url,
                    onValueChange = vm::setUrl,
                    label = { Text("Enlace del vídeo") },
                    leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                    singleLine = true,
                    enabled = !t.trabajando,
                    colors = campo,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { portapapeles.getText()?.text?.let(vm::setUrl) },
                        enabled = !t.trabajando,
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Pegar")
                    }
                    TextButton(onClick = { vm.setUrl("") }, enabled = !t.trabajando) { Text("Limpiar") }
                }
            }
        }

        SuperficieVidrio(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CALIDAD", style = MaterialTheme.typography.labelSmall, color = NeonCian)
                FilaCalidad(
                    calidad = form.calidad,
                    soloAudio = form.soloAudio,
                    enabled = !t.trabajando,
                    onCalidad = vm::setCalidad,
                    onSoloAudio = vm::setSoloAudio,
                )
            }
        }

        if (t.trabajando) {
            BotonNeon("Cancelar", onClick = vm::cancelar, modifier = Modifier.fillMaxWidth())
        } else {
            BotonNeon(
                texto = if (form.soloAudio) "Descargar MP3" else "Descargar",
                onClick = vm::descargar,
                enabled = form.url.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (t.trabajando || t.titulo.isNotBlank()) {
            SuperficieVidrio(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("EN CURSO", style = MaterialTheme.typography.labelSmall, color = NeonCian)
                    if (t.titulo.isNotBlank()) {
                        Text(
                            t.titulo,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    if (t.trabajando) {
                        if (t.progreso > 0f) {
                            LinearProgressIndicator(
                                progress = { t.progreso },
                                modifier = Modifier.fillMaxWidth(),
                                color = NeonCian,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = NeonCian,
                                strokeWidth = 3.dp,
                            )
                        }
                        Text(
                            t.linea.ifBlank { "Trabajando…" },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        t.mensaje?.let { msg ->
            SuperficieVidrio(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ESTADO", style = MaterialTheme.typography.labelSmall, color = NeonCian)
                    Text(msg, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
