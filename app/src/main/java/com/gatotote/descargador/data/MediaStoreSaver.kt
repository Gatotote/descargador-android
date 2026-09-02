package com.gatotote.descargador.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Copia el archivo descargado a la galería/almacenamiento público del sistema. */
object MediaStoreSaver {

    private const val SUBCARPETA = "Descargador"

    data class Guardado(val uri: Uri, val rutaVisible: String)

    suspend fun guardar(ctx: Context, archivo: File, esAudio: Boolean): Guardado =
        withContext(Dispatchers.IO) {
            val nombre = archivo.name
            val mime = if (esAudio) "audio/mpeg" else "video/mp4"
            val dirBase = if (esAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
            val relPath = "$dirBase/$SUBCARPETA"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val col = if (esAudio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val valores = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = ctx.contentResolver.insert(col, valores)
                    ?: error("No se pudo crear la entrada en MediaStore.")
                ctx.contentResolver.openOutputStream(uri).use { out ->
                    archivo.inputStream().use { it.copyTo(out!!) }
                }
                valores.clear()
                valores.put(MediaStore.MediaColumns.IS_PENDING, 0)
                ctx.contentResolver.update(uri, valores, null, null)
                archivo.delete()
                Guardado(uri, "$relPath/$nombre")
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(dirBase), SUBCARPETA)
                    .apply { mkdirs() }
                val destino = File(dir, nombre)
                archivo.copyTo(destino, overwrite = true)
                archivo.delete()
                Guardado(Uri.fromFile(destino), destino.absolutePath)
            }
        }
}
