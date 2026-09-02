package com.gatotote.descargador.ui.descargar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gatotote.descargador.DescargadorApp
import com.gatotote.descargador.data.Calidad
import com.gatotote.descargador.data.MediaStoreSaver
import com.gatotote.descargador.data.OpcionesDescarga
import com.gatotote.descargador.data.YtdlpEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EstadoDescarga(
    val url: String = "",
    val calidad: Calidad = Calidad.MEJOR,
    val soloAudio: Boolean = false,
    val trabajando: Boolean = false,
    val progreso: Float = 0f,
    val linea: String = "",
    val titulo: String = "",
    val mensaje: String? = null,
)

class DescargarViewModel(app: Application) : AndroidViewModel(app) {

    private val appc = app as DescargadorApp
    private val _estado = MutableStateFlow(EstadoDescarga())
    val estado: StateFlow<EstadoDescarga> = _estado

    private var idProceso: String? = null
    private var trabajo: Job? = null

    init {
        viewModelScope.launch {
            appc.ajustes.flujo.collect { a ->
                if (!_estado.value.trabajando && _estado.value.titulo.isBlank()) {
                    _estado.update { it.copy(calidad = a.calidad, soloAudio = a.soloAudio) }
                }
            }
        }
        viewModelScope.launch {
            appc.urlCompartida.collect { url ->
                if (!url.isNullOrBlank()) {
                    _estado.update { it.copy(url = url) }
                    appc.urlCompartida.value = null
                }
            }
        }
    }

    fun setUrl(v: String) = _estado.update { it.copy(url = v) }
    fun setCalidad(c: Calidad) = _estado.update { it.copy(calidad = c) }
    fun setSoloAudio(v: Boolean) = _estado.update { it.copy(soloAudio = v) }
    fun limpiarMensaje() = _estado.update { it.copy(mensaje = null) }

    fun descargar() {
        val e = _estado.value
        val url = e.url.trim()
        if (url.isEmpty() || e.trabajando) return
        if (!DescargadorApp.motorListo) {
            _estado.update { it.copy(mensaje = "El motor no se inició: ${DescargadorApp.errorInit}") }
            return
        }
        val id = UUID.randomUUID().toString()
        idProceso = id
        _estado.update {
            it.copy(trabajando = true, progreso = 0f, linea = "Preparando…", titulo = "", mensaje = null)
        }
        trabajo = viewModelScope.launch {
            val ctx = getApplication<Application>()
            val opciones = OpcionesDescarga(e.calidad, e.soloAudio)
            try {
                val titulo = runCatching { YtdlpEngine.obtenerTitulo(url) }.getOrDefault("Vídeo")
                _estado.update { it.copy(titulo = titulo) }

                val descargado = YtdlpEngine.descargar(ctx, url, opciones, id) { prog, _, linea ->
                    _estado.update { it.copy(progreso = prog.coerceIn(0f, 100f) / 100f, linea = linea) }
                }
                val guardado = MediaStoreSaver.guardar(ctx, descargado.archivo, descargado.esAudio)
                appc.historial.agregar(
                    titulo = descargado.titulo.ifBlank { titulo },
                    url = url,
                    tipo = if (descargado.esAudio) "audio" else "video",
                    estado = "ok",
                    detalle = guardado.rutaVisible,
                )
                _estado.update {
                    it.copy(trabajando = false, progreso = 1f, linea = "",
                        mensaje = "Guardado en ${guardado.rutaVisible}")
                }
            } catch (t: Throwable) {
                val msg = t.message?.lineSequence()?.lastOrNull { it.isNotBlank() }?.trim() ?: "Falló la descarga"
                appc.historial.agregar(_estado.value.titulo.ifBlank { url }, url,
                    if (opciones.soloAudio) "audio" else "video", "error", msg)
                _estado.update { it.copy(trabajando = false, linea = "", mensaje = "Error: $msg") }
            } finally {
                YtdlpEngine.limpiarTemporal(ctx, id)
                idProceso = null
            }
        }
    }

    fun cancelar() {
        idProceso?.let { YtdlpEngine.cancelar(it) }
        trabajo?.cancel()
        _estado.update { it.copy(trabajando = false, linea = "", mensaje = "Cancelado") }
    }
}
