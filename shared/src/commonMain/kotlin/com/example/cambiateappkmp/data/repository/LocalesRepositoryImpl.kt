package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.data.mapper.aLocales
import com.example.cambiateappkmp.data.mapper.consultaOverpass
import com.example.cambiateappkmp.data.remote.OverpassRespuesta
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.LocalRopa
import com.example.cambiateappkmp.domain.repository.LocalesRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlin.coroutines.cancellation.CancellationException

private const val URL_OVERPASS = "https://overpass-api.de/api/interpreter"

/** Busca tiendas de ropa en OpenStreetMap con Overpass (sin clave). */
class LocalesRepositoryImpl(
    private val cliente: HttpClient,
) : LocalesRepository {

    override suspend fun buscarCercanos(lat: Double, lng: Double, radioMetros: Int): Result<List<LocalRopa>> {
        val respuesta = try {
            cliente.submitForm(
                url = URL_OVERPASS,
                formParameters = parameters { append("data", consultaOverpass(lat, lng, radioMetros)) },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Si ni siquiera llegó una respuesta, lo tratamos como falta de conexión.
            return Result.failure(DomainException.SinConexion)
        }

        if (!respuesta.status.isSuccess()) {
            return Result.failure(
                DomainException.Desconocido(IllegalStateException("Overpass respondió ${respuesta.status}")),
            )
        }

        return try {
            Result.success(respuesta.body<OverpassRespuesta>().aLocales(lat, lng))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(DomainException.Desconocido(e))
        }
    }
}
