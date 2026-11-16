package com.example.cambiateappkmp.presentation.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.error.UbicacionException
import com.example.cambiateappkmp.domain.model.Ubicacion
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import com.example.cambiateappkmp.domain.repository.LocalesRepository
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RADIO_LOCALES_METROS = 1500

class MapaViewModel(
    private val ubicacionProvider: UbicacionProvider,
    private val climaRepository: ClimaRepository,
    private val localesRepository: LocalesRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<MapaUiState>(MapaUiState.SinPermiso())
    val estado: StateFlow<MapaUiState> = _estado.asStateFlow()

    private var trabajo: Job? = null

    fun permisoConcedido() {
        // Si ya estamos cargando o mostrando el mapa, no repetimos el trabajo.
        val actual = _estado.value
        if (actual is MapaUiState.Cargando || actual is MapaUiState.Contenido) return
        cargar()
    }

    fun permisoRechazado() {
        trabajo?.cancel()
        _estado.value = MapaUiState.SinPermiso(rechazado = true)
    }

    fun reintentar() = cargar()

    fun reintentarLocales() {
        val actual = _estado.value as? MapaUiState.Contenido ?: return
        viewModelScope.launch { cargarLocales(actual.ubicacion) }
    }

    private fun cargar() {
        trabajo?.cancel()
        _estado.value = MapaUiState.Cargando
        trabajo = viewModelScope.launch {
            val ubicacion = ubicacionProvider.obtenerUbicacionActual().getOrElse { error ->
                _estado.value = MapaUiState.Error(mensajeDeUbicacion(error))
                return@launch
            }

            // Primero se muestra el mapa; el clima y los locales llegan después, en paralelo.
            _estado.value = MapaUiState.Contenido(ubicacion = ubicacion)
            launch { cargarClima(ubicacion) }
            launch { cargarLocales(ubicacion) }
        }
    }

    private suspend fun cargarClima(ubicacion: Ubicacion) {
        val (actual, manana) = coroutineScope {
            val clima = async { climaRepository.obtenerClimaActual(ubicacion.lat, ubicacion.lng) }
            val pronostico = async { climaRepository.obtenerPronosticoManiana(ubicacion.lat, ubicacion.lng) }
            clima.await() to pronostico.await()
        }

        _estado.update { estado ->
            if (estado is MapaUiState.Contenido && estado.ubicacion == ubicacion) {
                val fallaron = actual.isFailure && manana.isFailure
                estado.copy(
                    clima = actual.getOrNull(),
                    pronostico = manana.getOrNull(),
                    cargandoClima = false,
                    errorClima = if (fallaron) mensajeDeClima(actual.exceptionOrNull()) else null,
                )
            } else {
                estado
            }
        }
    }

    private suspend fun cargarLocales(ubicacion: Ubicacion) {
        _estado.update { estado ->
            if (estado is MapaUiState.Contenido) estado.copy(cargandoLocales = true, errorLocales = null) else estado
        }

        val resultado = localesRepository.buscarCercanos(ubicacion.lat, ubicacion.lng, RADIO_LOCALES_METROS)

        _estado.update { estado ->
            if (estado is MapaUiState.Contenido && estado.ubicacion == ubicacion) {
                resultado.fold(
                    onSuccess = { locales ->
                        estado.copy(locales = locales, cargandoLocales = false, errorLocales = null)
                    },
                    // Los locales son un extra: si fallan, el resto de la pantalla sigue funcionando.
                    onFailure = {
                        estado.copy(
                            locales = emptyList(),
                            cargandoLocales = false,
                            errorLocales = "No pudimos cargar los locales",
                        )
                    },
                )
            } else {
                estado
            }
        }
    }

    private fun mensajeDeUbicacion(error: Throwable): String = when (error) {
        is UbicacionException -> error.message ?: "No pudimos obtener tu ubicación"
        else -> "No pudimos obtener tu ubicación"
    }

    private fun mensajeDeClima(error: Throwable?): String = when (error) {
        is DomainException.SinConexion -> "Sin conexión. No pudimos obtener el clima"
        else -> "No pudimos obtener el clima por ahora"
    }
}
