package com.example.cambiateappkmp.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.usecase.ObserveAuthStateUseCase
import com.example.cambiateappkmp.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class SessionViewModel(
    observarSesion: ObserveAuthStateUseCase,
    private val cerrarSesionUseCase: SignOutUseCase,
) : ViewModel() {

    val estado: StateFlow<SessionUiState> = observarSesion()
        .map { usuario ->
            if (usuario == null) SessionUiState.SinSesion else SessionUiState.ConSesion(usuario.email)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionUiState.Cargando,
        )

    fun cerrarSesion() {
        viewModelScope.launch { cerrarSesionUseCase() }
    }
}