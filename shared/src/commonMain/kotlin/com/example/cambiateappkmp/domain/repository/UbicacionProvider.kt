package com.example.cambiateappkmp.domain.repository

import com.example.cambiateappkmp.domain.model.Ubicacion

interface UbicacionProvider {
    suspend fun obtenerUbicacionActual(): Result<Ubicacion>
}
