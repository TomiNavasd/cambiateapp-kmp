package com.example.cambiateappkmp.domain.model

import kotlin.time.Instant

/** Un outfit guardado: una ocasión y las prendas que lo forman (por id). Mismo modelo que la app nativa. */
data class Outfit(
    val id: String,
    val ocasion: Ocasion,
    val prendasIds: List<String>,
    val fecha: Instant,
    val favorito: Boolean,
) {
    val esNuevo: Boolean get() = id.isEmpty()
}
