package com.example.cambiateappkmp.presentation.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.model.TemaApp
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AjustesViewModel(
    private val preferencias: PreferenciasRepository,
) : ViewModel() {

    val estado: StateFlow<AjustesUiState> = preferencias.tema
        .map { tema -> AjustesUiState(tema = tema) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesUiState())

    fun elegirTema(tema: TemaApp) {
        viewModelScope.launch { preferencias.guardarTema(tema) }
    }
}
