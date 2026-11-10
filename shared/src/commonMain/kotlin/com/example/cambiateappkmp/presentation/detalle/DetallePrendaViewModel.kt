package com.example.cambiateappkmp.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.usecase.ObtenerPrendaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class DetallePrendaViewModel(
    private val obtenerPrendaUseCase: ObtenerPrendaUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow<DetallePrendaUiState>(DetallePrendaUiState.Cargando)
    val estado: StateFlow<DetallePrendaUiState> = _estado.asStateFlow()

    private var idActual: String? = null
    private var trabajo: Job? = null

    fun cargar(id: String) {
        idActual = id
        trabajo?.cancel()
        _estado.value = DetallePrendaUiState.Cargando
        trabajo = viewModelScope.launch {
            _estado.value = try {
                val prenda = obtenerPrendaUseCase(id)
                if (prenda == null) {
                    DetallePrendaUiState.NoEncontrada
                } else {
                    DetallePrendaUiState.Contenido(prenda)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetallePrendaUiState.Error(mensajeDe(e))
            }
        }
    }

    fun reintentar() {
        idActual?.let { cargar(it) }
    }

    private fun mensajeDe(error: Throwable): String = when (error) {
        is DomainException.SinConexion -> "Sin conexión. Revisá tu internet e intentá de nuevo"
        else -> "No pudimos cargar la prenda"
    }
}