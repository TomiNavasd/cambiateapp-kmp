package com.example.cambiateappkmp.data.config

import com.example.cambiateappkmp.domain.config.ApiKeys

/**
 * Lee un valor de configuración: primero como propiedad de sistema (-DNOMBRE=valor) y, si no está,
 * como variable de entorno. Si no está en ninguna, devuelve cadena vacía.
 */
internal fun leerConfig(nombre: String): String =
    System.getProperty(nombre)?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv(nombre)?.trim()?.takeIf { it.isNotEmpty() }
        ?: ""

/** Datos del proyecto de Firebase que usa la versión de escritorio para hablar con la API REST. */
data class ConfigFirebaseEscritorio(
    val apiKey: String,
    val projectId: String,
) {
    val completa: Boolean get() = apiKey.isNotBlank() && projectId.isNotBlank()

    // Para que la clave nunca aparezca en un log por accidente.
    override fun toString() = "ConfigFirebaseEscritorio(apiKey=***, projectId=$projectId)"

    companion object {
        fun desdeEntorno() = ConfigFirebaseEscritorio(
            apiKey = leerConfig("FIREBASE_API_KEY"),
            projectId = leerConfig("FIREBASE_PROJECT_ID"),
        )
    }
}

class DesktopApiKeys : ApiKeys {
    override val anthropic: String = leerConfig("ANTHROPIC_API_KEY")

    // Sin uso: el mapa es OpenStreetMap y no necesita clave.
    override val maps: String = ""
}
