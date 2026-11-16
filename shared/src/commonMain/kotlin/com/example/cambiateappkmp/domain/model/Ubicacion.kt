package com.example.cambiateappkmp.domain.model

/**
 * Un punto del mapa. [aproximada] es true cuando no sale del GPS (por ejemplo, en escritorio,
 * donde se calcula por la dirección IP o se usa una ubicación por defecto).
 */
data class Ubicacion(
    val lat: Double,
    val lng: Double,
    val aproximada: Boolean = false,
)
