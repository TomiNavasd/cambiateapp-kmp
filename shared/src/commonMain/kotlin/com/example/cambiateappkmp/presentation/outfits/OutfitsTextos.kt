package com.example.cambiateappkmp.presentation.outfits

import com.example.cambiateappkmp.domain.error.DomainException

/** Textos de la pantalla de outfits, los mismos que la app nativa. */
internal object OutfitsTextos {
    const val SIN_SUGERENCIAS =
        "No hay prendas suficientes para esa ocasión: necesitás al menos una superior, una inferior y un calzado marcados para ella."
    const val YA_GUARDADO = "Ese outfit ya está guardado"
    const val GUARDADO = "Outfit guardado"
    const val SIN_PERMISOS = "No tenés permisos para esta acción."
    const val SIN_CONEXION = "Sin conexión. Revisá tu internet e intentá de nuevo"
    const val SESION_VENCIDA = "Tu sesión venció. Iniciá sesión de nuevo"
    const val INESPERADO = "Ocurrió un error inesperado. Intentá de nuevo"
}

internal fun Throwable.aMensajeDeOutfits(): String = when (this) {
    is DomainException.SinConexion -> OutfitsTextos.SIN_CONEXION
    is DomainException.SesionNoIniciada -> OutfitsTextos.SESION_VENCIDA
    is DomainException.SinPermisos -> OutfitsTextos.SIN_PERMISOS
    is DomainException.OutfitYaGuardado -> OutfitsTextos.YA_GUARDADO
    else -> OutfitsTextos.INESPERADO
}
