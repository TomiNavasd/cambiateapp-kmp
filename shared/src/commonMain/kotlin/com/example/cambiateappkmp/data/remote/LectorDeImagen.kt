package com.example.cambiateappkmp.data.remote

/** Convierte la dirección (uri) de una imagen en sus bytes. Cada plataforma lo implementa a su manera. */
interface LectorDeImagen {
    suspend fun leerBytes(uri: String): ByteArray
}
