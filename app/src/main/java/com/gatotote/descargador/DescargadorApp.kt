package com.gatotote.descargador

import android.app.Application
import android.util.Log
import com.gatotote.descargador.data.AjustesStore
import com.gatotote.descargador.data.DownloadService
import com.gatotote.descargador.data.HistorialStore
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DescargadorApp : Application() {

    val historial by lazy { HistorialStore(this) }
    val ajustes by lazy { AjustesStore(this) }

    /** URL recibida por "Compartir" hacia esta app; la pantalla Descargar la consume. */
    val urlCompartida = MutableStateFlow<String?>(null)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instancia = this
        DownloadService.crearCanal(this)
        // La 1ª vez, init() extrae Python + FFmpeg (segundos): fuera del hilo principal.
        scope.launch {
            try {
                YoutubeDL.getInstance().init(this@DescargadorApp)
                FFmpeg.getInstance().init(this@DescargadorApp)
                motorListo = true
            } catch (e: Exception) {
                errorInit = e.message
                Log.e("DescargadorApp", "No se pudo inicializar yt-dlp", e)
            } finally {
                inicializado.value = true
            }
        }
    }

    companion object {
        lateinit var instancia: DescargadorApp
            private set

        /** true cuando el init terminó (con éxito o error). */
        val inicializado = MutableStateFlow(false)
        var motorListo: Boolean = false
            private set
        var errorInit: String? = null
            private set

        suspend fun esperarMotor(): Boolean {
            inicializado.first { it }
            return motorListo
        }
    }
}
