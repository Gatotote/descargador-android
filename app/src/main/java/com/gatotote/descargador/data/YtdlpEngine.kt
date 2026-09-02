package com.gatotote.descargador.data

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class Calidad(val etiqueta: String, val altura: Int?) {
    MEJOR("Mejor calidad", null),
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480),
    P360("360p", 360),
}

data class OpcionesDescarga(
    val calidad: Calidad = Calidad.MEJOR,
    val soloAudio: Boolean = false,
)

data class ArchivoDescargado(
    val archivo: File,
    val titulo: String,
    val esAudio: Boolean,
)

/** Envoltorio de youtubedl-android. */
object YtdlpEngine {

    fun version(ctx: Context): String? = runCatching { YoutubeDL.getInstance().version(ctx) }.getOrNull()

    suspend fun actualizar(ctx: Context): String = withContext(Dispatchers.IO) {
        runCatching {
            val estado = YoutubeDL.getInstance()
                .updateYoutubeDL(ctx, YoutubeDL.UpdateChannel.STABLE)
            "$estado (${version(ctx)})"
        }.getOrElse { "Error: ${it.message}" }
    }

    suspend fun obtenerTitulo(url: String): String = withContext(Dispatchers.IO) {
        val req = YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            if (esYouTube(url)) addOption("--extractor-args", CLIENTES_YOUTUBE)
        }
        YoutubeDL.getInstance().getInfo(req).title ?: "Vídeo"
    }

    private fun formatoVideo(altura: Int?): String {
        val h = if (altura != null) "[height<=$altura]" else ""
        // Respaldos: mp4 nativo → cualquier v+a → itag 18 (360p mp4, el más
        // fiable en YouTube sin PO token) → lo mejor combinado que haya.
        return "bv*$h[ext=mp4]+ba[ext=m4a]/bv*$h+ba/18/b$h/b"
    }

    // Clientes de YouTube a probar: algunos evitan el 403/PO-token en la descarga.
    private const val CLIENTES_YOUTUBE = "youtube:player_client=web_safari,default"

    private fun esYouTube(url: String) =
        Regex("""youtube\.com|youtu\.be""").containsMatchIn(url)

    /**
     * Descarga a una carpeta temporal privada y devuelve el archivo final.
     * Deja el vídeo siempre en .mp4 (remux, sin recodificar) y el audio en .mp3.
     */
    suspend fun descargar(
        ctx: Context,
        url: String,
        opciones: OpcionesDescarga,
        idProceso: String,
        onLinea: (progreso: Float, etaSeg: Long, linea: String) -> Unit,
    ): ArchivoDescargado = withContext(Dispatchers.IO) {
        val tmp = File(ctx.filesDir, "tmp/$idProceso").apply { mkdirs() }
        try {
            val req = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--no-mtime")
                addOption("--restrict-filenames")
                if (esYouTube(url)) addOption("--extractor-args", CLIENTES_YOUTUBE)
                addOption("-o", File(tmp, "%(title).150B.%(ext)s").absolutePath)
                if (opciones.soloAudio) {
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                    addOption("--audio-quality", "192K")
                } else {
                    addOption("-f", formatoVideo(opciones.calidad.altura))
                    addOption("--merge-output-format", "mp4")
                    addOption("--remux-video", "mp4")
                }
            }
            YoutubeDL.getInstance().execute(req, idProceso) { progreso, eta, linea ->
                Log.d("YtdlpEngine", linea)
                onLinea(progreso, eta, linea)
            }

            val final = tmp.listFiles()
                ?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
                ?.maxByOrNull { it.length() }
                ?: error("La descarga no produjo ningún archivo.")

            ArchivoDescargado(
                archivo = final,
                titulo = final.nameWithoutExtension.replace('_', ' ').trim(),
                esAudio = opciones.soloAudio,
            )
        } catch (e: Throwable) {
            tmp.deleteRecursively()
            throw e
        }
    }

    fun cancelar(idProceso: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(idProceso) }
    }

    fun limpiarTemporal(ctx: Context, idProceso: String) {
        File(ctx.filesDir, "tmp/$idProceso").deleteRecursively()
    }
}
