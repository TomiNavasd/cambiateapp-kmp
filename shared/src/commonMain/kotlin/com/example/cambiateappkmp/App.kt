package com.example.cambiateappkmp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.cambiateappkmp.presentation.auth.LoginScreen
import com.example.cambiateappkmp.presentation.auth.LoginUiEvent
import com.example.cambiateappkmp.presentation.auth.LoginUiState
import com.example.cambiateappkmp.presentation.auth.RegisterScreen
import com.example.cambiateappkmp.presentation.auth.RegisterUiEvent
import com.example.cambiateappkmp.presentation.auth.RegisterUiState
import com.example.cambiateappkmp.ui.theme.CambiateAppTheme

@Composable
fun App() {
    var enRegistro by remember { mutableStateOf(false) }
    var login by remember { mutableStateOf(LoginUiState()) }
    var registro by remember { mutableStateOf(RegisterUiState()) }

    CambiateAppTheme(darkTheme = isSystemInDarkTheme()) {
        if (enRegistro) {
            RegisterScreen(
                state = registro,
                onEvent = { evento ->
                    registro = when (evento) {
                        is RegisterUiEvent.EmailCambiado -> registro.copy(
                            email = evento.valor, errorEmail = null, errorGeneral = null,
                        )
                        is RegisterUiEvent.ContrasenaCambiada -> registro.copy(
                            contrasena = evento.valor, errorContrasena = null, errorGeneral = null,
                        )
                        RegisterUiEvent.Registrar -> registro.copy(
                            errorEmail = if (registro.email.isBlank()) "Ingresá tu email" else null,
                            errorContrasena = if (registro.contrasena.length < 6) "Mínimo 6 caracteres" else null,
                            errorGeneral = if (registro.email == "error@test.com") "Ese email ya está registrado" else null,
                            cargando = registro.email.isNotBlank() &&
                                    registro.contrasena.length >= 6 &&
                                    registro.email != "error@test.com",
                        )
                    }
                },
                onIrALogin = { enRegistro = false },
            )
        } else {
            LoginScreen(
                state = login,
                onEvent = { evento ->
                    login = when (evento) {
                        is LoginUiEvent.EmailCambiado -> login.copy(
                            email = evento.valor, errorEmail = null, errorGeneral = null,
                        )
                        is LoginUiEvent.ContrasenaCambiada -> login.copy(
                            contrasena = evento.valor, errorContrasena = null, errorGeneral = null,
                        )
                        LoginUiEvent.Ingresar -> login.copy(
                            errorEmail = if (login.email.isBlank()) "Ingresá tu email" else null,
                            errorContrasena = if (login.contrasena.length < 6) "Mínimo 6 caracteres" else null,
                            errorGeneral = if (login.email == "error@test.com") "Email o contraseña incorrectos" else null,
                            cargando = login.email.isNotBlank() &&
                                    login.contrasena.length >= 6 &&
                                    login.email != "error@test.com",
                        )
                    }
                },
                onIrARegistro = { enRegistro = true },
            )
        }
    }
}