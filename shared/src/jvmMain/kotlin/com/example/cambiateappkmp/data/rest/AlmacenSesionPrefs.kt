package com.example.cambiateappkmp.data.rest

import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

private const val CLAVE_UID = "uid"
private const val CLAVE_EMAIL = "email"
private const val CLAVE_ID_TOKEN = "idToken"
private const val CLAVE_REFRESH_TOKEN = "refreshToken"
private const val CLAVE_EXPIRA = "expiraEnMs"

/**
 * Guarda la sesión con java.util.prefs (en Windows queda en el registro, dentro del usuario actual).
 * Se guardan los tokens y el uid, nunca la contraseña.
 */
internal class AlmacenSesionPrefs(
    private val nodo: Preferences = Preferences.userRoot().node("com/example/cambiateappkmp/sesion"),
) : AlmacenSesion {

    override fun leer(): SesionFirebase? {
        val uid = nodo.get(CLAVE_UID, null)?.takeIf { it.isNotBlank() } ?: return null
        val refreshToken = nodo.get(CLAVE_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        return SesionFirebase(
            uid = uid,
            email = nodo.get(CLAVE_EMAIL, ""),
            idToken = nodo.get(CLAVE_ID_TOKEN, ""),
            refreshToken = refreshToken,
            expiraEnMs = nodo.getLong(CLAVE_EXPIRA, 0L),
        )
    }

    override fun guardar(sesion: SesionFirebase) {
        nodo.put(CLAVE_UID, sesion.uid)
        nodo.put(CLAVE_EMAIL, sesion.email)
        nodo.put(CLAVE_ID_TOKEN, sesion.idToken)
        nodo.put(CLAVE_REFRESH_TOKEN, sesion.refreshToken)
        nodo.putLong(CLAVE_EXPIRA, sesion.expiraEnMs)
        vaciar()
    }

    override fun borrar() {
        listOf(CLAVE_UID, CLAVE_EMAIL, CLAVE_ID_TOKEN, CLAVE_REFRESH_TOKEN, CLAVE_EXPIRA).forEach { nodo.remove(it) }
        vaciar()
    }

    private fun vaciar() {
        try {
            nodo.flush()
        } catch (e: BackingStoreException) {
            // Si no se pudo escribir en disco, la sesión sigue valiendo mientras la app esté abierta.
        }
    }
}
