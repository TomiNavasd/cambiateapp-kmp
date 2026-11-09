package com.example.cambiateappkmp.domain.error

enum class CampoInvalido { EMAIL, CONTRASENA, NOMBRE, FOTO }

sealed class DomainException(cause: Throwable? = null) : Exception(cause) {
    object CredencialesInvalidas : DomainException()
    object EmailYaRegistrado : DomainException()
    object ContrasenaDebil : DomainException()
    object EmailInvalido : DomainException()
    object SesionNoIniciada : DomainException()
    object SinConexion : DomainException()
    class Validacion(val campo: CampoInvalido) : DomainException()
    class Desconocido(cause: Throwable? = null) : DomainException(cause)
}
