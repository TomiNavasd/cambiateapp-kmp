package com.example.cambiateappkmp.presentation.mapa

import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.LocalRopa
import com.example.cambiateappkmp.domain.model.PronosticoManiana
import com.example.cambiateappkmp.domain.model.Ubicacion

sealed interface MapaUiState {
    data class SinPermiso(val rechazado: Boolean = false) : MapaUiState
    data object Cargando : MapaUiState
    data class Error(val mensaje: String) : MapaUiState

    /** Con la ubicación ya conocida: el clima y los locales llegan por separado y no se bloquean entre sí. */
    data class Contenido(
        val ubicacion: Ubicacion,
        val clima: ClimaActual? = null,
        val pronostico: PronosticoManiana? = null,
        val cargandoClima: Boolean = true,
        val errorClima: String? = null,
        val locales: List<LocalRopa> = emptyList(),
        val cargandoLocales: Boolean = true,
        val errorLocales: String? = null,
    ) : MapaUiState
}
