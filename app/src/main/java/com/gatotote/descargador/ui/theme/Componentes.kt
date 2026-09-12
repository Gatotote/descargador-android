package com.gatotote.descargador.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gatotote.descargador.data.Calidad

private val FormaVidrio = RoundedCornerShape(22.dp)

@Composable
fun FondoEscena(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(DegradadoFondo),
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 48.dp, y = (-36).dp)
                .size(280.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x334DA3FF), Color.Transparent)),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-56).dp, y = 48.dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x2222D3EE), Color.Transparent)),
                    CircleShape,
                ),
        )
        content()
    }
}

@Composable
fun CabeceraPantalla(
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            "DESCARGADOR",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCian,
        )
        Spacer(Modifier.height(6.dp))
        Text(titulo, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SuperficieVidrio(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .border(1.dp, VidrioBorde, FormaVidrio)
            .clip(FormaVidrio),
        shape = FormaVidrio,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = content,
    )
}

@Composable
fun BotonNeon(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val forma = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(forma)
            .background(if (enabled) DegradadoMarca else Brush.linearGradient(listOf(Color(0xFF2A3548), Color(0xFF2A3548))))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            color = if (enabled) NeonInk else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun FilaCalidad(
    calidad: Calidad,
    soloAudio: Boolean,
    enabled: Boolean,
    onCalidad: (Calidad) -> Unit,
    onSoloAudio: (Boolean) -> Unit,
) {
    val chips = FilterChipDefaults.filterChipColors(
        selectedContainerColor = NeonAzul.copy(alpha = 0.22f),
        selectedLabelColor = NeonCian,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        disabledSelectedContainerColor = NeonAzul.copy(alpha = 0.12f),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Calidad.entries.forEach { c ->
            FilterChip(
                selected = calidad == c && !soloAudio,
                onClick = {
                    onSoloAudio(false)
                    onCalidad(c)
                },
                enabled = enabled,
                label = { Text(c.corta()) },
                colors = chips,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = enabled,
                    selected = calidad == c && !soloAudio,
                    selectedBorderColor = NeonAzul.copy(alpha = 0.7f),
                    borderColor = VidrioBorde,
                ),
            )
        }
        FilterChip(
            selected = soloAudio,
            onClick = { onSoloAudio(true) },
            enabled = enabled,
            label = { Text("MP3") },
            colors = chips,
            border = FilterChipDefaults.filterChipBorder(
                enabled = enabled,
                selected = soloAudio,
                selectedBorderColor = NeonCian.copy(alpha = 0.7f),
                borderColor = VidrioBorde,
            ),
        )
    }
}

private fun Calidad.corta(): String = when (this) {
    Calidad.MEJOR -> "MAX"
    Calidad.P1080 -> "1080"
    Calidad.P720 -> "720"
    Calidad.P480 -> "480"
    Calidad.P360 -> "360"
}
