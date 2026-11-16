package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.PronosticoManiana
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class ClimaRepositoryImplTest {

    private val respuestaCompleta = """
        {
          "current": {"temperature_2m": 22.4, "weather_code": 2, "precipitation": 0.0},
          "daily": {
            "temperature_2m_max": [25.0, 24.0],
            "temperature_2m_min": [15.0, 16.0],
            "precipitation_probability_max": [10, 70],
            "weather_code": [2, 61]
          }
        }
    """.trimIndent()

    private fun clienteCon(
        status: HttpStatusCode = HttpStatusCode.OK,
        cuerpo: String,
        alRecibir: (HttpRequestData) -> Unit = {},
    ): HttpClient = HttpClient(
        MockEngine { request ->
            alRecibir(request)
            respond(
                content = cuerpo,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Test
    fun parseaElClimaActual() = runTest {
        val repo = ClimaRepositoryImpl(clienteCon(cuerpo = respuestaCompleta))

        val resultado = repo.obtenerClimaActual(-34.6037, -58.3816)

        assertEquals(
            ClimaActual(temperaturaC = 22.4, descripcion = "Parcialmente nublado", llueve = false),
            resultado.getOrNull(),
        )
    }

    @Test
    fun parseaElPronosticoDeMananaConElSegundoDia() = runTest {
        val repo = ClimaRepositoryImpl(clienteCon(cuerpo = respuestaCompleta))

        val resultado = repo.obtenerPronosticoManiana(-34.6037, -58.3816)

        assertEquals(
            PronosticoManiana(maximaC = 24.0, minimaC = 16.0, probabilidadLluvia = 70, descripcion = "Lluvia"),
            resultado.getOrNull(),
        )
    }

    @Test
    fun laProbabilidadDeLluviaNulaCuentaComoCero() = runTest {
        val cuerpo = """
            {"daily": {"temperature_2m_max": [25.0, 24.0], "temperature_2m_min": [15.0, 16.0],
             "precipitation_probability_max": [null, null], "weather_code": [0, 0]}}
        """.trimIndent()
        val repo = ClimaRepositoryImpl(clienteCon(cuerpo = cuerpo))

        val resultado = repo.obtenerPronosticoManiana(0.0, 0.0)

        assertEquals(0, resultado.getOrNull()?.probabilidadLluvia)
    }

    @Test
    fun hayLluviaActualSiLaApiInformaPrecipitacionAunqueElCodigoNoLoDiga() = runTest {
        val cuerpo = """{"current": {"temperature_2m": 18.0, "weather_code": 3, "precipitation": 0.6}}"""
        val repo = ClimaRepositoryImpl(clienteCon(cuerpo = cuerpo))

        val resultado = repo.obtenerClimaActual(0.0, 0.0)

        assertEquals(true, resultado.getOrNull()?.llueve)
    }

    @Test
    fun fallaConDesconocidoCuandoElServicioRespondeConError() = runTest {
        val repo = ClimaRepositoryImpl(clienteCon(HttpStatusCode.InternalServerError, """{"error": true}"""))

        val clima = repo.obtenerClimaActual(0.0, 0.0)
        val pronostico = repo.obtenerPronosticoManiana(0.0, 0.0)

        assertIs<DomainException.Desconocido>(clima.exceptionOrNull())
        assertIs<DomainException.Desconocido>(pronostico.exceptionOrNull())
    }

    @Test
    fun fallaConDesconocidoCuandoElJsonEsInvalido() = runTest {
        val repo = ClimaRepositoryImpl(clienteCon(cuerpo = "esto no es un json"))

        val resultado = repo.obtenerClimaActual(0.0, 0.0)

        assertIs<DomainException.Desconocido>(resultado.exceptionOrNull())
    }

    @Test
    fun fallaCuandoLaRespuestaNoTraeLasTemperaturasDeManiana() = runTest {
        // Solo trae el día de hoy: falta el índice 1.
        val cuerpo = """{"daily": {"temperature_2m_max": [25.0], "temperature_2m_min": [15.0]}}"""
        val repo = ClimaRepositoryImpl(clienteCon(cuerpo = cuerpo))

        val resultado = repo.obtenerPronosticoManiana(0.0, 0.0)

        assertIs<DomainException.Desconocido>(resultado.exceptionOrNull())
    }

    @Test
    fun fallaCuandoLaRespuestaNoTraeElClimaActual() = runTest {
        val repo = ClimaRepositoryImpl(clienteCon(cuerpo = """{"daily": {}}"""))

        val resultado = repo.obtenerClimaActual(0.0, 0.0)

        assertIs<DomainException.Desconocido>(resultado.exceptionOrNull())
    }

    @Test
    fun fallaConSinConexionCuandoNoLlegaNingunaRespuesta() = runTest {
        val cliente = HttpClient(MockEngine { throw IllegalStateException("sin red") })
        val repo = ClimaRepositoryImpl(cliente)

        val resultado = repo.obtenerClimaActual(0.0, 0.0)

        assertSame(DomainException.SinConexion, resultado.exceptionOrNull())
    }

    @Test
    fun pideAOpenMeteoConLaUbicacionYLosCamposEsperados() = runTest {
        var pedido: HttpRequestData? = null
        val repo = ClimaRepositoryImpl(clienteCon(cuerpo = respuestaCompleta) { pedido = it })

        repo.obtenerClimaActual(-34.6037, -58.3816)

        assertEquals(HttpMethod.Get, pedido?.method)
        assertEquals("api.open-meteo.com", pedido?.url?.host)
        assertEquals("-34.6037", pedido?.url?.parameters?.get("latitude"))
        assertEquals("-58.3816", pedido?.url?.parameters?.get("longitude"))
        assertEquals("2", pedido?.url?.parameters?.get("forecast_days"))
        assertTrue(pedido?.url?.parameters?.get("current").orEmpty().contains("weather_code"))
        assertTrue(pedido?.url?.parameters?.get("daily").orEmpty().contains("precipitation_probability_max"))
    }
}
