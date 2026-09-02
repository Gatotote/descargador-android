package com.gatotote.descargador.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EntradaHistorial(
    val titulo: String,
    val url: String,
    val tipo: String,        // "video" | "audio"
    val estado: String,      // "ok" | "error"
    val detalle: String,     // ruta visible o mensaje de error
    val fecha: String,
    val uri: String = "",    // content:// del archivo guardado (si estado == "ok")
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("titulo", titulo); put("url", url); put("tipo", tipo)
        put("estado", estado); put("detalle", detalle); put("fecha", fecha); put("uri", uri)
    }

    companion object {
        fun fromJson(o: JSONObject) = EntradaHistorial(
            o.optString("titulo"), o.optString("url"), o.optString("tipo", "video"),
            o.optString("estado", "ok"), o.optString("detalle"), o.optString("fecha"),
            o.optString("uri"),
        )
    }
}

/** Historial persistido como JSON en filesDir; expone un StateFlow para Compose. */
class HistorialStore(private val ctx: Context) {

    private val archivo get() = File(ctx.filesDir, "historial.json")
    private val _entradas = MutableStateFlow<List<EntradaHistorial>>(emptyList())
    val entradas: StateFlow<List<EntradaHistorial>> = _entradas

    init {
        _entradas.value = leer()
    }

    private fun leer(): List<EntradaHistorial> = runCatching {
        if (!archivo.exists()) return emptyList()
        val arr = JSONArray(archivo.readText())
        buildList { for (i in 0 until arr.length()) add(EntradaHistorial.fromJson(arr.getJSONObject(i))) }
    }.getOrDefault(emptyList())

    private fun escribir(lista: List<EntradaHistorial>) {
        val arr = JSONArray()
        lista.forEach { arr.put(it.toJson()) }
        archivo.writeText(arr.toString())
        _entradas.value = lista
    }

    suspend fun agregar(
        titulo: String, url: String, tipo: String, estado: String, detalle: String,
        uri: String = "",
    ) = withContext(Dispatchers.IO) {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        escribir(listOf(EntradaHistorial(titulo, url, tipo, estado, detalle, fecha, uri)) + _entradas.value)
    }

    suspend fun borrar(entrada: EntradaHistorial) = withContext(Dispatchers.IO) {
        escribir(_entradas.value.filterNot { it === entrada || (it.fecha == entrada.fecha && it.titulo == entrada.titulo) })
    }

    suspend fun borrarTodo() = withContext(Dispatchers.IO) { escribir(emptyList()) }
}
