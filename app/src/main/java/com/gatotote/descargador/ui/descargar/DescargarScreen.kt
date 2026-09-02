package com.gatotote.descargador.ui.descargar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatotote.descargador.data.Calidad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescargarScreen(
    modifier: Modifier = Modifier,
    vm: DescargarViewModel = viewModel(),
) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val form = e.form
    val t = e.trabajo
    val portapapeles = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = form.url,
            onValueChange = vm::setUrl,
            label = { Text("Enlace del vídeo") },
            singleLine = true,
            enabled = !t.trabajando,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { portapapeles.getText()?.text?.let(vm::setUrl) },
                enabled = !t.trabajando,
            ) { Text("Pegar") }
            OutlinedButton(onClick = { vm.setUrl("") }, enabled = !t.trabajando) { Text("Limpiar") }
        }

        Text("Calidad", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            Calidad.entries.forEach { c ->
                FilterChip(
                    selected = form.calidad == c && !form.soloAudio,
                    onClick = { vm.setCalidad(c) },
                    label = { Text(c.etiqueta.substringBefore(" ")) },
                    enabled = !t.trabajando && !form.soloAudio,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = form.soloAudio, onCheckedChange = vm::setSoloAudio, enabled = !t.trabajando)
            Text("  Solo audio (MP3)")
        }

        if (t.trabajando) {
            Button(onClick = vm::cancelar, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        } else {
            Button(
                onClick = vm::descargar,
                enabled = form.url.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Descargar") }
        }

        if (t.trabajando || t.titulo.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (t.titulo.isNotBlank()) {
                        Text(t.titulo, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall)
                    }
                    if (t.trabajando) {
                        if (t.progreso > 0f) {
                            LinearProgressIndicator(
                                progress = { t.progreso }, modifier = Modifier.fillMaxWidth())
                        } else {
                            CircularProgressIndicator()
                        }
                        Text(t.linea.ifBlank { "Trabajando…" },
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        t.mensaje?.let { msg ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(msg, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
