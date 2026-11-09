package com.example.cambiateappkmp.presentation.auth

import com.example.cambiateappkmp.domain.error.CampoInvalido
import com.example.cambiateappkmp.domain.error.DomainException

data class ErrorDeFormulario(
    val email: String? = null,
    val contrasena: String? = null,
    val general: String? = null,
)

fun Throwable.aErrorDeFormulario(): ErrorDeFormulario = when (this) {
    is DomainException.EmailInvalido ->
        ErrorDeFormulario(email = "Ingresá un email válido")

    is DomainException.ContrasenaDebil ->
        ErrorDeFormulario(contrasena = "La contraseña debe tener al menos 6 caracteres")

    is DomainException.Validacion -> when (this.campo) {
        CampoInvalido.EMAIL -> ErrorDeFormulario(email = "Completá el email")
        CampoInvalido.CONTRASENA -> ErrorDeFormulario(contrasena = "Ingresá tu contraseña")
        CampoInvalido.NOMBRE, CampoInvalido.FOTO ->
            ErrorDeFormulario(general = "Revisá los datos ingresados")
    }

    is DomainException.CredencialesInvalidas ->
        ErrorDeFormulario(general = "Email o contraseña incorrectos")

    is DomainException.EmailYaRegistrado ->
        ErrorDeFormulario(general = "Ese email ya está registrado")

    is DomainException.SinConexion ->
        ErrorDeFormulario(general = "No hay conexión a internet. Revisá tu red e intentá de nuevo")

    is DomainException.SesionNoIniciada ->
        ErrorDeFormulario(general = "Tu sesión no está iniciada")

    else ->
        ErrorDeFormulario(general = "Ocurrió un error inesperado. Intentá de nuevo")
}