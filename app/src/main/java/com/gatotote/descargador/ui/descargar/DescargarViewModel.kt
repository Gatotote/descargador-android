package com.gatotote.descargador.ui.descargar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gatotote.descargador.DescargadorApp
import com.gatotote.descargador.data.Calidad
import com.gatotote.descargador.data.DownloadService
import com.gatotote.descargador.data.EstadoTrabajo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del formulario (lo que el usuario elige). */
data class FormularioDescarga(
    val url: String = "",
    val calidad: Calidad = Calidad.MEJOR,
    val soloAudio: Boolean = false,
)

data class EstadoPantalla(
    val form: FormularioDescarga = FormularioDescarga(),
    val trabajo: EstadoTrabajo = EstadoTrabajo(),
)

class DescargarViewModel(app: Application) : AndroidViewModel(app) {

    private val appc = app as DescargadorApp
    private val _form = MutableStateFlow(FormularioDescarga())

    val estado = combine(_form, DownloadService.estado) { f, t -> EstadoPantalla(f, t) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoPantalla())

    init {
        viewModelScope.launch {
            appc.ajustes.flujo.collect { a ->
                if (!DownloadService.estado.value.trabajando && _form.value.url.isBlank()) {
                    _form.update { it.copy(calidad = a.calidad, soloAudio = a.soloAudio) }
                }
            }
        }
        viewModelScope.launch {
            appc.urlCompartida.collect { url ->
                if (!url.isNullOrBlank()) {
                    _form.update { it.copy(url = url) }
                    appc.urlCompartida.value = null
                }
            }
        }
    }

    fun setUrl(v: String) = _form.update { it.copy(url = v) }
    fun setCalidad(c: Calidad) = _form.update { it.copy(calidad = c) }
    fun setSoloAudio(v: Boolean) = _form.update { it.copy(soloAudio = v) }

    fun limpiarMensaje() {
        if (!DownloadService.estado.value.trabajando) {
            DownloadService.estado.value = EstadoTrabajo()
        }
    }

    fun descargar() {
        val f = _form.value
        if (f.url.isBlank() || DownloadService.estado.value.trabajando) return
        if (DescargadorApp.inicializado.value && !DescargadorApp.motorListo) {
            DownloadService.estado.value =
                EstadoTrabajo(mensaje = "El motor no se inició: ${DescargadorApp.errorInit}")
            return
        }
        DownloadService.iniciar(getApplication(), f.url.trim(), f.calidad, f.soloAudio)
    }

    fun cancelar() = DownloadService.cancelar(getApplication())
}
