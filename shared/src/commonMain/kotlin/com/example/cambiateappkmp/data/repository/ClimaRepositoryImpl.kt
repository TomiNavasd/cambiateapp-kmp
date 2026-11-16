package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.data.mapper.aClimaActual
import com.example.cambiateappkmp.data.mapper.aPronosticoManiana
import com.example.cambiateappkmp.data.remote.OpenMeteoRespuesta
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.PronosticoManiana
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

private const val URL_OPEN_METEO = "https://api.open-meteo.com/v1/forecast"

class ClimaRepositoryImpl(
    private val cliente: HttpClient,
) : ClimaRepository {

    override suspend fun obtenerClimaActual(lat: Double, lng: Double): Result<ClimaActual> =
        pedirDatos(lat, lng).fold(
            onSuccess = { datos ->
                datos.aClimaActual()?.let { Result.success(it) } ?: Result.failure(incompleto("el clima actual"))
            },
            onFailure = { Result.failure(it) },
        )

    override suspend fun obtenerPronosticoManiana(lat: Double, lng: Double): Result<PronosticoManiana> =
        pedirDatos(lat, lng).fold(
            onSuccess = { datos ->
                datos.aPronosticoManiana()?.let { Result.success(it) } ?: Result.failure(incompleto("el pronóstico de mañana"))
            },
            onFailure = { Result.failure(it) },
        )

    private suspend fun pedirDatos(lat: Double, lng: Double): Result<OpenMeteoRespuesta> {
        val respuesta = try {
            cliente.get(URL_OPEN_METEO) {
                parameter("latitude", lat)
                parameter("longitude", lng)
                parameter("current", "temperature_2m,weather_code,precipitation")
                parameter("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code")
                parameter("timezone", "auto")
                parameter("forecast_days", 2)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Si ni siquiera llegó una respuesta, lo tratamos como falta de conexión.
            return Result.failure(DomainException.SinConexion)
        }

        if (!respuesta.status.isSuccess()) {
            return Result.failure(
                DomainException.Desconocido(IllegalStateException("El servicio del clima respondió ${respuesta.status}")),
            )
        }

        return try {
            Result.success(respuesta.body<OpenMeteoRespuesta>())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(DomainException.Desconocido(e))
        }
    }

    private fun incompleto(que: String) =
        DomainException.Desconocido(IllegalStateException("La respuesta no trae $que"))
}
