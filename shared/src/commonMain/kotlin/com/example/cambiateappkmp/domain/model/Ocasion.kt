package com.example.cambiateappkmp.domain.model

enum class Ocasion { TRABAJO, FACULTAD, SALIDA, CASAMIENTO, DEPORTE, CASUAL }

/** Valor que se guarda en Firestore (campo "ocasion"): el nombre en minúscula, igual que la app nativa. */
val Ocasion.valorFirestore: String get() = name.lowercase()

/** Lee el valor de Firestore sin distinguir mayúsculas. Null si no es una ocasión conocida. */
fun ocasionDeValorFirestore(valor: String): Ocasion? =
    Ocasion.entries.firstOrNull { it.name.equals(valor, ignoreCase = true) }
