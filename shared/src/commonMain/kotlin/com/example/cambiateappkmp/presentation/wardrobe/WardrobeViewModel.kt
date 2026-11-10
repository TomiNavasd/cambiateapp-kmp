package com.example.cambiateappkmp.presentation.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.usecase.ObservePrendasUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class WardrobeViewModel(
    private val observePrendas: ObservePrendasUseCase,
) : ViewModel() {

    private val filtro = MutableStateFlow<Categoria?>(null)
    private val busqueda = MutableStateFlow("")
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<WardrobeUiState> = reintentos
        .flatMapLatest {
            combine(observePrendas(), filtro, busqueda) { prendas, categoria, texto ->
                armarEstado(prendas, categoria, texto)
            }
                .onStart { emit(WardrobeUiState.Cargando) }
                .catch { emit(WardrobeUiState.Error(mensajeDe(it))) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WardrobeUiState.Cargando,
        )

    fun onEvent(event: WardrobeUiEvent) {
        when (event) {
            is WardrobeUiEvent.FiltroSeleccionado -> filtro.value = event.categoria
            is WardrobeUiEvent.BusquedaCambiada -> busqueda.value = event.texto
            WardrobeUiEvent.Reintentar -> reintentos.update { it + 1 }
        }
    }

    private fun armarEstado(
        prendas: List<Prenda>,
        categoria: Categoria?,
        texto: String,
    ): WardrobeUiState {
        if (prendas.isEmpty()) return WardrobeUiState.Vacio
        val consulta = texto.trim()
        val visibles = prendas
            .filter { categoria == null || it.categoria == categoria }
            .filter { consulta.isEmpty() || it.nombre.contains(consulta, ignoreCase = true) }
        return WardrobeUiState.Contenido(prendas = visibles, filtro = categoria)
    }

    private fun mensajeDe(error: Throwable): String = when (error) {
        is DomainException.SinConexion -> "Sin conexión. Revisá tu internet e intentá de nuevo"
        else -> "No pudimos cargar tu armario"
    }
}