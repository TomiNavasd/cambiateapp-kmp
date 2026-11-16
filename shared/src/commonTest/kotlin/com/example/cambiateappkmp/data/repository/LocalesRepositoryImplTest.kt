package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.domain.error.DomainException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class LocalesRepositoryImplTest {

    // Origen de la búsqueda: el Obelisco.
    private val lat = -34.6037
    private val lng = -58.3816

    private val respuestaOverpass = """
        {"elements": [
          {"type": "way", "id": 2, "center": {"lat": -34.6200, "lon": -58.4000}, "tags": {"name": "Lejana"}},
          {"type": "node", "id": 1, "lat": -34.6040, "lon": -58.3830,
           "tags": {"name": "Cercana", "addr:street": "Corrientes", "addr:housenumber": "1234", "addr:city": "Buenos Aires"}},
          {"type": "node", "id": 3, "lat": -34.6030, "lon": -58.3820, "tags": {}},
          {"type": "node", "id": 4, "tags": {"name": "Sin coordenadas"}}
        ]}
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
    fun devuelveLosLocalesOrdenadosDelMasCercanoAlMasLejano() = runTest {
        val repo = LocalesRepositoryImpl(clienteCon(cuerpo = respuestaOverpass))

        val locales = repo.buscarCercanos(lat, lng, 1500).getOrThrow()

        // El que no tiene nombre queda como "Tienda de ropa" y el que no trae coordenadas se descarta.
        assertEquals(listOf("Tienda de ropa", "Cercana", "Lejana"), locales.map { it.nombre })
    }

    @Test
    fun armaLaDireccionYUsaElCentroDeLosEdificios() = runTest {
        val repo = LocalesRepositoryImpl(clienteCon(cuerpo = respuestaOverpass))

        val locales = repo.buscarCercanos(lat, lng, 1500).getOrThrow().associateBy { it.id }

        assertEquals("Corrientes 1234, Buenos Aires", locales["node/1"]?.direccion)
        assertEquals("", locales["node/3"]?.direccion)
        assertEquals(-34.6200, locales["way/2"]?.lat)
        assertEquals(-58.4000, locales["way/2"]?.lng)
    }

    @Test
    fun sinResultadosDevuelveUnaListaVacia() = runTest {
        val repo = LocalesRepositoryImpl(clienteCon(cuerpo = """{"elements": []}"""))

        assertEquals(emptyList(), repo.buscarCercanos(lat, lng, 1500).getOrThrow())
    }

    @Test
    fun hacePostAOverpassConLaConsultaDeTiendasDeRopaYElRadio() = runTest {
        var pedido: HttpRequestData? = null
        val repo = LocalesRepositoryImpl(clienteCon(cuerpo = respuestaOverpass) { pedido = it })

        repo.buscarCercanos(lat, lng, 1500)

        assertEquals(HttpMethod.Post, pedido?.method)
        assertEquals("overpass-api.de", pedido?.url?.host)
        val consulta = (pedido?.body as? FormDataContent)?.formData?.get("data").orEmpty()
        assertContains(consulta, "\"shop\"=\"clothes\"")
        assertContains(consulta, "around:1500")
    }

    @Test
    fun fallaConDesconocidoSiOverpassRespondeConError() = runTest {
        val repo = LocalesRepositoryImpl(clienteCon(HttpStatusCode.TooManyRequests, "{}"))

        assertIs<DomainException.Desconocido>(repo.buscarCercanos(lat, lng, 1500).exceptionOrNull())
    }

    @Test
    fun fallaConDesconocidoSiElJsonEsInvalido() = runTest {
        val repo = LocalesRepositoryImpl(clienteCon(cuerpo = "<html>no es json</html>"))

        assertIs<DomainException.Desconocido>(repo.buscarCercanos(lat, lng, 1500).exceptionOrNull())
    }

    @Test
    fun fallaConSinConexionSiNoLlegaNingunaRespuesta() = runTest {
        val repo = LocalesRepositoryImpl(HttpClient(MockEngine { throw IllegalStateException("sin red") }))

        assertSame(DomainException.SinConexion, repo.buscarCercanos(lat, lng, 1500).exceptionOrNull())
    }
}
