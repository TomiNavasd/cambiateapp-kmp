package com.example.cambiateappkmp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onEvent(evento: RegisterUiEvent) {
        when (evento) {
            is RegisterUiEvent.EmailCambiado -> _state.update {
                it.copy(email = evento.valor, errorEmail = null, errorGeneral = null)
            }

            is RegisterUiEvent.ContrasenaCambiada -> _state.update {
                it.copy(contrasena = evento.valor, errorContrasena = null, errorGeneral = null)
            }

            RegisterUiEvent.Registrar -> registrar()
        }
    }

    private fun registrar() {
        val actual = _state.value
        if (actual.cargando) return

        _state.update {
            it.copy(cargando = true, errorEmail = null, errorContrasena = null, errorGeneral = null)
        }

        viewModelScope.launch {
            registerUseCase(actual.email, actual.contrasena).fold(
                onSuccess = { _state.value = RegisterUiState() },
                onFailure = { error ->
                    val e = error.aErrorDeFormulario()
                    _state.update {
                        it.copy(
                            cargando = false,
                            errorEmail = e.email,
                            errorContrasena = e.contrasena,
                            errorGeneral = e.general,
                        )
                    }
                },
            )
        }
    }
}