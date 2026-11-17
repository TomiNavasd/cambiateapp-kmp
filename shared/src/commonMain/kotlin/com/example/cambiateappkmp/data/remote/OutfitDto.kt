package com.example.cambiateappkmp.data.remote

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/** Documento de la colección "outfits". Mismos campos que la app nativa (el id es el del documento). */
@Serializable
data class OutfitDto(
    val userId: String = "",
    val ocasion: String = "",
    val prendasIds: List<String> = emptyList(),
    val fecha: Timestamp? = null,
    val favorito: Boolean = false,
)
