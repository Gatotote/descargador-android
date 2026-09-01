package com.gatotote.descargador

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class DescargadorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            motorListo = true
            Log.i(TAG, "yt-dlp inicializado: " + YoutubeDL.getInstance().version(this))
        } catch (e: YoutubeDLException) {
            errorInit = e.message
            Log.e(TAG, "No se pudo inicializar yt-dlp", e)
        }
    }

    companion object {
        private const val TAG = "DescargadorApp"

        /** true si YoutubeDL/FFmpeg se inicializaron correctamente. */
        var motorListo: Boolean = false
            private set

        /** Mensaje de error si la inicialización falló. */
        var errorInit: String? = null
            private set
    }
}
