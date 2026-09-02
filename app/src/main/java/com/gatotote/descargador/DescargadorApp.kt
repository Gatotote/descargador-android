package com.gatotote.descargador

import android.app.Application
import android.util.Log
import com.gatotote.descargador.data.AjustesStore
import com.gatotote.descargador.data.HistorialStore
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.flow.MutableStateFlow
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class DescargadorApp : Application() {

    val historial by lazy { HistorialStore(this) }
    val ajustes by lazy { AjustesStore(this) }

    /** URL recibida por "Compartir" hacia esta app; la pantalla Descargar la consume. */
    val urlCompartida = MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        instancia = this
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            motorListo = true
        } catch (e: YoutubeDLException) {
            errorInit = e.message
            Log.e("DescargadorApp", "No se pudo inicializar yt-dlp", e)
        }
    }

    companion object {
        lateinit var instancia: DescargadorApp
            private set
        var motorListo: Boolean = false
            private set
        var errorInit: String? = null
            private set
    }
}
