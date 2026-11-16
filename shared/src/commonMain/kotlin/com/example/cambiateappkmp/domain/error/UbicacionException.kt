package com.example.cambiateappkmp.domain.error

sealed class UbicacionException(mensaje: String) : Exception(mensaje) {
    class SinPermiso : UbicacionException("Falta el permiso de ubicación")

    class NoDisponible(
        mensaje: String = "No pudimos obtener tu ubicación. Revisá que el GPS esté activado",
    ) : UbicacionException(mensaje)
}
