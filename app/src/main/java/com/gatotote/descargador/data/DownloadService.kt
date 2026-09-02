package com.gatotote.descargador.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gatotote.descargador.DescargadorApp
import com.gatotote.descargador.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EstadoTrabajo(
    val trabajando: Boolean = false,
    val progreso: Float = 0f,     // 0..1 ; 0 = indeterminado
    val linea: String = "",
    val titulo: String = "",
    val mensaje: String? = null,
)

class DownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trabajo: Job? = null
    private var idProceso: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCELAR -> {
                cancelarInterno()
                return START_NOT_STICKY
            }
            ACTION_INICIAR -> iniciarDescarga(
                url = intent.getStringExtra(EXTRA_URL).orEmpty(),
                calidad = runCatching { Calidad.valueOf(intent.getStringExtra(EXTRA_CALIDAD) ?: "MEJOR") }
                    .getOrDefault(Calidad.MEJOR),
                soloAudio = intent.getBooleanExtra(EXTRA_AUDIO, false),
            )
        }
        return START_NOT_STICKY
    }

    private fun iniciarDescarga(url: String, calidad: Calidad, soloAudio: Boolean) {
        if (url.isBlank() || estado.value.trabajando) return
        val id = UUID.randomUUID().toString()
        idProceso = id
        estado.value = EstadoTrabajo(trabajando = true, linea = "Preparando…")
        arrancarEnPrimerPlano("Preparando descarga…", 0, true)

        trabajo = scope.launch {
            val app = application as DescargadorApp
            val opciones = OpcionesDescarga(calidad, soloAudio)
            try {
                if (!DescargadorApp.esperarMotor()) {
                    error("El motor no se pudo iniciar: ${DescargadorApp.errorInit}")
                }
                estado.update { it.copy(linea = "Obteniendo información…") }
                val titulo = runCatching { YtdlpEngine.obtenerTitulo(url) }.getOrDefault("Vídeo")
                estado.update { it.copy(titulo = titulo) }
                notificar(titulo, 0, true)

                val descargado = YtdlpEngine.descargar(applicationContext, url, opciones, id) { prog, _, linea ->
                    val p = prog.coerceIn(0f, 100f)
                    estado.update { it.copy(progreso = p / 100f, linea = linea) }
                    notificar(titulo, p.toInt(), p <= 0f)
                }
                val guardado = MediaStoreSaver.guardar(applicationContext, descargado.archivo, descargado.esAudio)
                app.historial.agregar(
                    descargado.titulo.ifBlank { titulo }, url,
                    if (descargado.esAudio) "audio" else "video", "ok", guardado.rutaVisible,
                )
                estado.value = EstadoTrabajo(
                    trabajando = false, progreso = 1f,
                    mensaje = "Guardado en ${guardado.rutaVisible}",
                )
                notificarFin("Descarga completada", descargado.titulo.ifBlank { titulo })
            } catch (t: Throwable) {
                val msg = t.message?.lineSequence()?.lastOrNull { it.isNotBlank() }?.trim()
                    ?: "Falló la descarga"
                app.historial.agregar(
                    estado.value.titulo.ifBlank { url }, url,
                    if (opciones.soloAudio) "audio" else "video", "error", msg,
                )
                estado.value = EstadoTrabajo(trabajando = false, mensaje = "Error: $msg")
                notificarFin("Error en la descarga", msg)
            } finally {
                YtdlpEngine.limpiarTemporal(applicationContext, id)
                idProceso = null
                pararPrimerPlano()
            }
        }
    }

    private fun cancelarInterno() {
        idProceso?.let { YtdlpEngine.cancelar(it) }
        trabajo?.cancel()
        estado.value = EstadoTrabajo(trabajando = false, mensaje = "Cancelado")
        pararPrimerPlano()
        notificar_cancel()
    }

    // --- Notificaciones -------------------------------------------------

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun accionCancelar(): NotificationCompat.Action {
        val pi = PendingIntent.getService(
            this, 1,
            Intent(this, DownloadService::class.java).setAction(ACTION_CANCELAR),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Action(0, "Cancelar", pi)
    }

    private fun base(texto: String, progreso: Int, indeterminado: Boolean) =
        NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando")
            .setContentText(texto)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .setProgress(100, progreso, indeterminado)
            .addAction(accionCancelar())

    private fun arrancarEnPrimerPlano(texto: String, progreso: Int, indeterminado: Boolean) {
        val n = base(texto, progreso, indeterminado).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun notificar(texto: String, progreso: Int, indeterminado: Boolean) {
        nm().notify(NOTIF_ID, base(texto, progreso, indeterminado).build())
    }

    private fun notificarFin(titulo: String, texto: String) {
        val n = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        nm().notify(NOTIF_ID + 1, n)
    }

    private fun notificar_cancel() = nm().cancel(NOTIF_ID)

    private fun pararPrimerPlano() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION") stopForeground(true)
        }
        stopSelf()
    }

    private fun nm() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "descargas"
        private const val NOTIF_ID = 1001
        private const val ACTION_INICIAR = "iniciar"
        private const val ACTION_CANCELAR = "cancelar"
        private const val EXTRA_URL = "url"
        private const val EXTRA_CALIDAD = "calidad"
        private const val EXTRA_AUDIO = "audio"

        /** Estado del trabajo en curso, compartido con la UI. */
        val estado = MutableStateFlow(EstadoTrabajo())

        fun crearCanal(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(
                    CANAL, "Descargas", NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Progreso de las descargas" }
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(canal)
            }
        }

        fun iniciar(ctx: Context, url: String, calidad: Calidad, soloAudio: Boolean) {
            val i = Intent(ctx, DownloadService::class.java).apply {
                action = ACTION_INICIAR
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_CALIDAD, calidad.name)
                putExtra(EXTRA_AUDIO, soloAudio)
            }
            ContextCompat.startForegroundService(ctx, i)
        }

        fun cancelar(ctx: Context) {
            ctx.startService(
                Intent(ctx, DownloadService::class.java).setAction(ACTION_CANCELAR),
            )
        }
    }
}
