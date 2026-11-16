package com.example.cambiateappkmp.data.notification

import com.example.cambiateappkmp.domain.notification.Notificador
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import com.example.cambiateappkmp.domain.usecase.MENSAJE_RECORDATORIO_GENERAL
import com.example.cambiateappkmp.domain.usecase.construirMensajeRecordatorio
import com.example.cambiateappkmp.presentation.avisos.AvisosEnApp
import java.awt.Color
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TIEMPO_MAXIMO_CLIMA_MS = 20_000L

/** Evita que, si el reloj despierta un instante antes de la hora, el aviso salga dos veces. */
private const val PAUSA_DESPUES_DE_AVISAR_MS = 1_000L

/** Milisegundos hasta la próxima vez que el reloj marque hora:minuto:00 (hoy si todavía no pasó, si no, mañana). */
internal fun milisHastaProximaEjecucion(ahora: ZonedDateTime, hora: Int, minuto: Int): Long {
    var proximo = ahora.toLocalDate().atTime(hora, minuto).atZone(ahora.zone)
    if (!proximo.isAfter(ahora)) proximo = proximo.plusDays(1)
    return Duration.between(ahora, proximo).toMillis()
}

/** Muestra una notificación del sistema. Devuelve false si no se pudo. */
internal interface BandejaDelSistema {
    fun mostrar(titulo: String, mensaje: String): Boolean
}

/** Notificaciones con la bandeja del sistema de Java (AWT). */
internal class BandejaAwt : BandejaDelSistema {
    private var icono: TrayIcon? = null

    @Synchronized
    override fun mostrar(titulo: String, mensaje: String): Boolean =
        try {
            if (!SystemTray.isSupported()) {
                false
            } else {
                val bandeja = SystemTray.getSystemTray()
                val actual = icono ?: crearIcono().also {
                    bandeja.add(it)
                    icono = it
                    // Al cerrar la app, el ícono se saca de la bandeja.
                    Runtime.getRuntime().addShutdownHook(
                        Thread {
                            try {
                                bandeja.remove(it)
                            } catch (e: Throwable) {
                                // Al cerrar no importa si falla.
                            }
                        },
                    )
                }
                actual.displayMessage(titulo, mensaje, TrayIcon.MessageType.INFO)
                true
            }
        } catch (e: Exception) {
            // Sin bandeja (HeadlessException, AWTException, etc.): el aviso sale dentro de la app.
            false
        }

    private fun crearIcono(): TrayIcon {
        val imagen = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val dibujo = imagen.createGraphics()
        dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        dibujo.color = Color(0x00897B)
        dibujo.fillOval(1, 1, 14, 14)
        dibujo.dispose()
        return TrayIcon(imagen, "CambiateApp").apply { isImageAutoSize = true }
    }
}

/**
 * Notificador de escritorio. No hay un servicio del sistema que despierte la app (como WorkManager en Android):
 * el recordatorio diario lo programa la propia app y solo sale mientras esté abierta.
 */
internal class NotificadorEscritorio(
    private val clima: ClimaRepository,
    private val ubicacion: UbicacionProvider,
    private val avisos: AvisosEnApp,
    private val bandeja: BandejaDelSistema = BandejaAwt(),
    private val alcance: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val ahora: () -> ZonedDateTime = { ZonedDateTime.now() },
) : Notificador {

    private var tarea: Job? = null

    @Synchronized
    override fun programarDiaria(hora: Int, minuto: Int) {
        // Reemplaza la programación anterior: así cambiar la hora mueve el aviso.
        tarea?.cancel()
        tarea = alcance.launch {
            while (isActive) {
                delay(milisHastaProximaEjecucion(ahora(), hora, minuto))
                ejecutarRecordatorio()
                delay(PAUSA_DESPUES_DE_AVISAR_MS)
            }
        }
    }

    @Synchronized
    override fun cancelarDiaria() {
        tarea?.cancel()
        tarea = null
    }

    override fun mostrarAhora(titulo: String, mensaje: String) {
        // Si el sistema no puede mostrar la notificación, el aviso sale dentro de la app.
        if (!bandeja.mostrar(titulo, mensaje)) avisos.mostrar(titulo, mensaje)
    }

    private suspend fun ejecutarRecordatorio() {
        val mensaje = try {
            withTimeoutOrNull(TIEMPO_MAXIMO_CLIMA_MS) { armarMensaje() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: MENSAJE_RECORDATORIO_GENERAL

        // Pase lo que pase con el clima, el aviso sale: nunca queda en silencio.
        mostrarAhora(mensaje.first, mensaje.second)
    }

    /** Devuelve null si no se pudo obtener el pronóstico de mañana. */
    private suspend fun armarMensaje(): Pair<String, String>? {
        val lugar = ubicacion.obtenerUbicacionActual().getOrNull() ?: return null
        val pronostico = clima.obtenerPronosticoManiana(lugar.lat, lugar.lng).getOrNull() ?: return null
        return construirMensajeRecordatorio(pronostico)
    }
}
