package com.example.cambiateappkmp.presentation.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.model.ConfigNotificaciones
import com.example.cambiateappkmp.domain.model.PronosticoManiana
import com.example.cambiateappkmp.domain.notification.Notificador
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import com.example.cambiateappkmp.domain.usecase.MENSAJE_RECORDATORIO_GENERAL
import com.example.cambiateappkmp.domain.usecase.construirMensajeRecordatorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificacionesViewModel(
    private val preferencias: PreferenciasRepository,
    private val notificador: Notificador,
    private val climaRepository: ClimaRepository,
    private val ubicacionProvider: UbicacionProvider,
) : ViewModel() {

    private val aviso = MutableStateFlow<String?>(null)

    val estado: StateFlow<NotificacionesUiState> =
        combine(preferencias.notificaciones, aviso) { config, aviso ->
            NotificacionesUiState(
                activadas = config.activadas,
                hora = config.hora,
                minuto = config.minuto,
                aviso = aviso,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificacionesUiState())

    /** Se llama cuando el permiso de notificaciones ya está concedido. */
    fun activar() = guardar(activadas = true)

    fun desactivar() = guardar(activadas = false)

    fun elegirHora(hora: Int, minuto: Int) = guardar(hora = hora, minuto = minuto)

    fun permisoNotificacionesRechazado() {
        aviso.value = "Sin permiso no podemos avisarte. Activalo en los ajustes del sistema."
    }

    fun ubicacionRechazada() {
        aviso.value = "Sin tu ubicación no podemos saber el clima de tu zona. Activala en los ajustes del sistema."
    }

    fun avisoMostrado() {
        aviso.value = null
    }

    /** Pide la ubicación actual y la deja guardada para el aviso en segundo plano. */
    fun actualizarUbicacion() {
        viewModelScope.launch {
            ubicacionProvider.obtenerUbicacionActual().fold(
                onSuccess = { ubicacion -> preferencias.guardarUltimaUbicacion(ubicacion) },
                onFailure = {
                    aviso.value = "No pudimos obtener tu ubicación. Revisá que el GPS esté activado."
                },
            )
        }
    }

    /** Muestra ahora una notificación con el pronóstico real (o de ejemplo, en modo fake). */
    fun enviarPrueba() {
        viewModelScope.launch {
            val pronostico = obtenerPronostico()
            if (pronostico != null) {
                val (titulo, mensaje) = construirMensajeRecordatorio(pronostico)
                notificador.mostrarAhora(titulo, mensaje)
                aviso.value = "Listo, mirá la barra de notificaciones"
            } else {
                val (titulo, mensaje) = MENSAJE_RECORDATORIO_GENERAL
                notificador.mostrarAhora(titulo, mensaje)
                aviso.value = "No pudimos obtener el clima: te mandamos el aviso general"
            }
        }
    }

    private suspend fun obtenerPronostico(): PronosticoManiana? {
        val ubicacion = preferencias.ultimaUbicacion()
            ?: ubicacionProvider.obtenerUbicacionActual().getOrNull()?.also { preferencias.guardarUltimaUbicacion(it) }
            ?: return null
        return climaRepository.obtenerPronosticoManiana(ubicacion.lat, ubicacion.lng).getOrNull()
    }

    private fun guardar(activadas: Boolean? = null, hora: Int? = null, minuto: Int? = null) {
        viewModelScope.launch {
            val actual = preferencias.notificaciones.first()
            val nueva = ConfigNotificaciones(
                activadas = activadas ?: actual.activadas,
                hora = hora ?: actual.hora,
                minuto = minuto ?: actual.minuto,
            )
            preferencias.guardarNotificaciones(nueva)

            if (nueva.activadas) {
                notificador.programarDiaria(nueva.hora, nueva.minuto)
                aviso.value = "Recordatorio activado: te avisamos todos los días a las ${formatearHora(nueva.hora, nueva.minuto)}"
            } else {
                notificador.cancelarDiaria()
                aviso.value = "Recordatorio desactivado"
            }
        }
    }
}
