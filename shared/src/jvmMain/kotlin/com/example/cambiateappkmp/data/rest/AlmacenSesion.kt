package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.domain.model.Usuario

/** Sesión de Firebase. Nunca se guarda la contraseña. */
internal data class SesionFirebase(
    val uid: String,
    val email: String,
    val idToken: String,
    val refreshToken: String,
    /** Cuándo vence el idToken, en milisegundos desde 1970 (ya con un margen de seguridad). */
    val expiraEnMs: Long,
) {
    fun aUsuario() = Usuario(id = uid, email = email)
}

/** Dónde se guarda la sesión para que, al reabrir la app, siga iniciada. */
internal interface AlmacenSesion {
    fun leer(): SesionFirebase?
    fun guardar(sesion: SesionFirebase)
    fun borrar()
}

/** Guarda la sesión en memoria: se pierde al cerrar la app. Sirve para los tests. */
internal class AlmacenSesionEnMemoria : AlmacenSesion {
    private var sesion: SesionFirebase? = null

    override fun leer(): SesionFirebase? = sesion

    override fun guardar(sesion: SesionFirebase) {
        this.sesion = sesion
    }

    override fun borrar() {
        sesion = null
    }
}
