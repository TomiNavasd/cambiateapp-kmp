package com.example.cambiateappkmp.data.rest

/** Entrega el idToken que hay que mandar a Firestore, renovándolo cuando venció. */
internal interface TokenProvider {
    /** El uid del usuario con sesión iniciada, o null si no hay. */
    val uid: String?

    /**
     * Devuelve un idToken vigente. Si venció (o si [forzar] es true) pide uno nuevo con el refresh token.
     * Falla con SesionNoIniciada si no hay sesión o si Firebase ya no acepta el refresh token.
     */
    suspend fun idTokenVigente(forzar: Boolean = false): Result<String>
}
