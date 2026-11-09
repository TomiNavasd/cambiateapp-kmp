package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import kotlinx.coroutines.flow.Flow

class ObservePrendasUseCase(
    private val repository: PrendaRepository,
) {
    operator fun invoke(): Flow<List<Prenda>> = repository.observarPrendas()
}