package com.example.cambiateappkmp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEvent(evento: LoginUiEvent) {
        when (evento) {
            is LoginUiEvent.EmailCambiado -> _state.update {
                it.copy(email = evento.valor, errorEmail = null, errorGeneral = null)
            }

            is LoginUiEvent.ContrasenaCambiada -> _state.update {
                it.copy(contrasena = evento.valor, errorContrasena = null, errorGeneral = null)
            }

            LoginUiEvent.Ingresar -> ingresar()
        }
    }

    private fun ingresar() {
        val actual = _state.value
        if (actual.cargando) return

        _state.update {
            it.copy(cargando = true, errorEmail = null, errorContrasena = null, errorGeneral = null)
        }

        viewModelScope.launch {
            loginUseCase(actual.email, actual.contrasena).fold(
                onSuccess = { _state.value = LoginUiState() },
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