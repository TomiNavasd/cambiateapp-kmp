package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.data.preferences.AlmacenClaveValor
import com.example.cambiateappkmp.domain.model.ConfigNotificaciones
import com.example.cambiateappkmp.domain.model.TemaApp
import com.example.cambiateappkmp.domain.model.Ubicacion
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val CLAVE_TEMA = "tema"
private const val CLAVE_NOTIF_ACTIVADAS = "notificaciones_activadas"
private const val CLAVE_NOTIF_HORA = "notificaciones_hora"
private const val CLAVE_NOTIF_MINUTO = "notificaciones_minuto"
private const val CLAVE_UBICACION_LAT = "ultima_ubicacion_lat"
private const val CLAVE_UBICACION_LNG = "ultima_ubicacion_lng"

/** Valores por defecto: tema del sistema y recordatorios desactivados, a las 20:00. */
private val NOTIFICACIONES_POR_DEFECTO = ConfigNotificaciones(activadas = false, hora = 20, minuto = 0)

class PreferenciasRepositoryImpl(
    private val almacen: AlmacenClaveValor,
) : PreferenciasRepository {

    // Se leen una vez al crear el repositorio y después se mantienen al día cada vez que se guarda.
    private val _tema = MutableStateFlow(leerTema())
    private val _notificaciones = MutableStateFlow(leerNotificaciones())

    override val tema: Flow<TemaApp> = _tema.asStateFlow()

    override suspend fun guardarTema(tema: TemaApp) {
        almacen.guardar(CLAVE_TEMA, tema.name)
        _tema.value = tema
    }

    override val notificaciones: Flow<ConfigNotificaciones> = _notificaciones.asStateFlow()

    override suspend fun guardarNotificaciones(config: ConfigNotificaciones) {
        val valida = config.copy(hora = config.hora.coerceIn(0, 23), minuto = config.minuto.coerceIn(0, 59))
        almacen.guardar(CLAVE_NOTIF_ACTIVADAS, valida.activadas.toString())
        almacen.guardar(CLAVE_NOTIF_HORA, valida.hora.toString())
        almacen.guardar(CLAVE_NOTIF_MINUTO, valida.minuto.toString())
        _notificaciones.value = valida
    }

    override suspend fun guardarUltimaUbicacion(ubicacion: Ubicacion) {
        almacen.guardar(CLAVE_UBICACION_LAT, ubicacion.lat.toString())
        almacen.guardar(CLAVE_UBICACION_LNG, ubicacion.lng.toString())
    }

    override suspend fun ultimaUbicacion(): Ubicacion? {
        val lat = almacen.leer(CLAVE_UBICACION_LAT)?.toDoubleOrNull() ?: return null
        val lng = almacen.leer(CLAVE_UBICACION_LNG)?.toDoubleOrNull() ?: return null
        return Ubicacion(lat = lat, lng = lng)
    }

    private fun leerTema(): TemaApp {
        val guardado = almacen.leer(CLAVE_TEMA)
        return TemaApp.entries.firstOrNull { it.name == guardado } ?: TemaApp.SISTEMA
    }

    private fun leerNotificaciones(): ConfigNotificaciones = ConfigNotificaciones(
        activadas = almacen.leer(CLAVE_NOTIF_ACTIVADAS)?.toBooleanStrictOrNull() ?: NOTIFICACIONES_POR_DEFECTO.activadas,
        hora = almacen.leer(CLAVE_NOTIF_HORA)?.toIntOrNull()?.coerceIn(0, 23) ?: NOTIFICACIONES_POR_DEFECTO.hora,
        minuto = almacen.leer(CLAVE_NOTIF_MINUTO)?.toIntOrNull()?.coerceIn(0, 59) ?: NOTIFICACIONES_POR_DEFECTO.minuto,
    )
}
