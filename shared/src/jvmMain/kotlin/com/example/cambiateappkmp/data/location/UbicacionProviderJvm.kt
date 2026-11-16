package com.example.cambiateappkmp.data.location

import com.example.cambiateappkmp.domain.model.Ubicacion
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable

private const val URL_UBICACION_POR_IP = "https://ipwho.is/"

/** Si no se pudo saber dónde está el usuario, se usa Buenos Aires (el Obelisco). */
internal val UBICACION_POR_DEFECTO = Ubicacion(lat = -34.6037, lng = -58.3816, aproximada = true)

@Serializable
internal data class UbicacionPorIpRespuesta(
    val success: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

/**
 * Ubicación de escritorio: no hay GPS, así que se calcula una ubicación aproximada a partir de la dirección IP.
 * Si eso falla, se usa la última ubicación guardada y, si tampoco hay, Buenos Aires. Nunca falla.
 */
internal class UbicacionProviderJvm(
    private val cliente: HttpClient,
    private val preferencias: PreferenciasRepository,
) : UbicacionProvider {

    override suspend fun obtenerUbicacionActual(): Result<Ubicacion> {
        porIp()?.let { return Result.success(it) }
        preferencias.ultimaUbicacion()?.let { return Result.success(it.copy(aproximada = true)) }
        return Result.success(UBICACION_POR_DEFECTO)
    }

    private suspend fun porIp(): Ubicacion? =
        try {
            val respuesta = cliente.get(URL_UBICACION_POR_IP) {
                parameter("fields", "success,latitude,longitude")
            }
            if (!respuesta.status.isSuccess()) {
                null
            } else {
                val datos = respuesta.body<UbicacionPorIpRespuesta>()
                val lat = datos.latitude
                val lng = datos.longitude
                if (datos.success && lat != null && lng != null) Ubicacion(lat, lng, aproximada = true) else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
