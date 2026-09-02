package com.gatotote.descargador.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

data class Ajustes(
    val calidad: Calidad = Calidad.MEJOR,
    val soloAudio: Boolean = false,
    val ultimaActualizacionYtdlp: String = "",
)

class AjustesStore(private val ctx: Context) {

    private val CALIDAD = stringPreferencesKey("calidad")
    private val SOLO_AUDIO = booleanPreferencesKey("solo_audio")
    private val ULT_ACT = stringPreferencesKey("ult_act_ytdlp")

    val flujo: Flow<Ajustes> = ctx.dataStore.data.map { p ->
        Ajustes(
            calidad = runCatching { Calidad.valueOf(p[CALIDAD] ?: "MEJOR") }.getOrDefault(Calidad.MEJOR),
            soloAudio = p[SOLO_AUDIO] ?: false,
            ultimaActualizacionYtdlp = p[ULT_ACT] ?: "",
        )
    }

    suspend fun setCalidad(c: Calidad) = ctx.dataStore.edit { it[CALIDAD] = c.name }
    suspend fun setSoloAudio(v: Boolean) = ctx.dataStore.edit { it[SOLO_AUDIO] = v }
    suspend fun setUltimaActualizacion(fecha: String) = ctx.dataStore.edit { it[ULT_ACT] = fecha }
}
