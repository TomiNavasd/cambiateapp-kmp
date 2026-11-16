package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.domain.model.LocalRopa
import com.example.cambiateappkmp.domain.repository.LocalesRepository
import kotlinx.coroutines.delay

/** Cinco locales de ejemplo cerca del Obelisco (-34.6037, -58.3816). */
class FakeLocalesRepository : LocalesRepository {

    override suspend fun buscarCercanos(lat: Double, lng: Double, radioMetros: Int): Result<List<LocalRopa>> {
        delay(300)
        return Result.success(LOCALES)
    }

    private companion object {
        val LOCALES = listOf(
            LocalRopa("fake/1", "Boutique Corrientes", "Av. Corrientes 1234, Buenos Aires", -34.6040, -58.3830),
            LocalRopa("fake/2", "Moda Florida", "Florida 450, Buenos Aires", -34.6035, -58.3750),
            LocalRopa("fake/3", "Outlet Lavalle", "Lavalle 780, Buenos Aires", -34.6022, -58.3805),
            LocalRopa("fake/4", "Casa de Abrigos 9 de Julio", "Av. 9 de Julio 1100, Buenos Aires", -34.6055, -58.3818),
            LocalRopa("fake/5", "Jeans y Más", "Talcahuano 520, Buenos Aires", -34.6030, -58.3855),
        )
    }
}
