package com.example.cambiateappkmp.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cambiateappkmp.domain.notification.Notificador
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import com.example.cambiateappkmp.domain.usecase.MENSAJE_RECORDATORIO_GENERAL
import com.example.cambiateappkmp.domain.usecase.construirMensajeRecordatorio
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Tarea diaria del recordatorio. Usa la última ubicación guardada (en segundo plano no se puede
 * pedir el GPS), consulta el pronóstico de mañana y muestra la notificación.
 */
class RecordatorioClimaWorker(
    contexto: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(contexto, parametros), KoinComponent {

    private val climaRepository: ClimaRepository by inject()
    private val preferencias: PreferenciasRepository by inject()
    private val notificador: Notificador by inject()

    override suspend fun doWork(): Result {
        val mensaje = try {
            withTimeoutOrNull(TIEMPO_MAXIMO_CLIMA_MS) { armarMensaje() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: MENSAJE_RECORDATORIO_GENERAL

        // Pase lo que pase con el clima, el aviso sale: nunca queda en silencio.
        notificador.mostrarAhora(mensaje.first, mensaje.second)
        return Result.success()
    }

    /** Devuelve null si todavía no hay ubicación guardada o si no se pudo obtener el pronóstico. */
    private suspend fun armarMensaje(): Pair<String, String>? {
        val ubicacion = preferencias.ultimaUbicacion() ?: return null
        val pronostico = climaRepository.obtenerPronosticoManiana(ubicacion.lat, ubicacion.lng).getOrNull()
            ?: return null
        return construirMensajeRecordatorio(pronostico)
    }

    private companion object {
        const val TIEMPO_MAXIMO_CLIMA_MS = 20_000L
    }
}
