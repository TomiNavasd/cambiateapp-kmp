package com.example.cambiateappkmp.domain.repository

import com.example.cambiateappkmp.domain.model.ConfigNotificaciones
import com.example.cambiateappkmp.domain.model.TemaApp
import com.example.cambiateappkmp.domain.model.Ubicacion
import kotlinx.coroutines.flow.Flow

interface PreferenciasRepository {
    val tema: Flow<TemaApp>
    suspend fun guardarTema(tema: TemaApp)

    val notificaciones: Flow<ConfigNotificaciones>
    suspend fun guardarNotificaciones(config: ConfigNotificaciones)

    /** La última ubicación conocida, para que el recordatorio en segundo plano sepa de dónde pedir el clima. */
    suspend fun guardarUltimaUbicacion(ubicacion: Ubicacion)
    suspend fun ultimaUbicacion(): Ubicacion?
}
