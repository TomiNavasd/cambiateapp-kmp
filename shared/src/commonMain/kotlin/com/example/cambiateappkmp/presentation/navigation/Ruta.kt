package com.example.cambiateappkmp.presentation.navigation

import com.example.cambiateappkmp.domain.model.ClimaActual

sealed interface Ruta {
    data object Login : Ruta
    data object Registro : Ruta

    data object Armario : Ruta
    data class DetallePrenda(val id: String) : Ruta

    data class AltaEdicion(val id: String?) : Ruta

    /** Pestaña Outfits. El clima es opcional: llega cuando se entra desde "Sugerir" del Mapa. */
    data class Outfits(val clima: ClimaActual? = null) : Ruta

    data object Mapa : Ruta
    data object Ajustes : Ruta
    data object Notificaciones : Ruta
}

/** Las tres pantallas raíz que tienen pestaña en la barra inferior: Armario, Outfits y Mapa. */
val Ruta.esPestana: Boolean
    get() = this is Ruta.Armario || this is Ruta.Outfits || this is Ruta.Mapa

/** La barra inferior se muestra solo en las pestañas, no en Login, Registro, Detalle, Alta/Edición, Ajustes ni Notificaciones. */
val Ruta.muestraBarraInferior: Boolean get() = esPestana

/** Dos rutas son la misma pestaña si son del mismo tipo, aunque Outfits traiga otro clima. */
fun Ruta.mismaPestanaQue(otra: Ruta): Boolean = when (this) {
    is Ruta.Armario -> otra is Ruta.Armario
    is Ruta.Outfits -> otra is Ruta.Outfits
    is Ruta.Mapa -> otra is Ruta.Mapa
    else -> false
}
