package com.example.cambiateappkmp.data.rest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Json de la API REST de Firebase: ignora lo que no usamos y envía también los valores por defecto. */
internal val jsonFirebase = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
internal data class CredencialesRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true,
)

/** Respuesta de signInWithPassword y de signUp. */
@Serializable
internal data class AuthRespuesta(
    val idToken: String = "",
    val refreshToken: String = "",
    val expiresIn: String = "3600",
    val localId: String = "",
    val email: String = "",
)

/** Respuesta del endpoint de renovación del token (los nombres vienen en snake_case). */
@Serializable
internal data class RenovarRespuesta(
    @SerialName("id_token") val idToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: String = "3600",
    @SerialName("user_id") val userId: String = "",
)

@Serializable
internal data class ErrorFirebaseRespuesta(val error: ErrorFirebase? = null)

@Serializable
internal data class ErrorFirebase(
    val code: Int = 0,
    val message: String = "",
)
