package com.example.cambiateappkmp.domain.repository

import com.example.cambiateappkmp.domain.model.LocalRopa

interface LocalesRepository {
    suspend fun buscarCercanos(lat: Double, lng: Double, radioMetros: Int): Result<List<LocalRopa>>
}
