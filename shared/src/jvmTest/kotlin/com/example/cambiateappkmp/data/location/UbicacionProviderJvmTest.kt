package com.example.cambiateappkmp.data.location

import com.example.cambiateappkmp.data.preferences.AlmacenClaveValorEnMemoria
import com.example.cambiateappkmp.data.repository.PreferenciasRepositoryImpl
import com.example.cambiateappkmp.domain.model.Ubicacion
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class UbicacionProviderJvmTest {

    private val preferencias = PreferenciasRepositoryImpl(AlmacenClaveValorEnMemoria())
    private val pedidos = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun proveedor(manejador: MockRequestHandler): UbicacionProviderJvm {
        val cliente = HttpClient(MockEngine { pedidos += it; manejador(it) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return UbicacionProviderJvm(cliente, preferencias)
    }

    @Test
    fun conRespuestaExitosaUsaLaUbicacionDeLaIpYLaMarcaComoAproximada() = runTest {
        val proveedor = proveedor { respond("""{"success":true,"latitude":-34.7,"longitude":-58.27}""", headers = json) }

        val ubicacion = proveedor.obtenerUbicacionActual().getOrThrow()

        assertEquals(Ubicacion(-34.7, -58.27, aproximada = true), ubicacion)
        assertEquals("ipwho.is", pedidos.single().url.host)
        assertEquals("success,latitude,longitude", pedidos.single().url.parameters["fields"])
    }

    @Test
    fun siElServicioDiceSuccessFalseCaeALaUltimaUbicacionGuardada() = runTest {
        preferencias.guardarUltimaUbicacion(Ubicacion(-31.4, -64.18))
        val proveedor = proveedor { respond("""{"success":false}""", headers = json) }

        val ubicacion = proveedor.obtenerUbicacionActual().getOrThrow()

        assertEquals(-31.4, ubicacion.lat)
        assertEquals(-64.18, ubicacion.lng)
        assertTrue(ubicacion.aproximada)
    }

    @Test
    fun conErrorHttpYSinNadaGuardadoUsaBuenosAires() = runTest {
        val proveedor = proveedor { respond("{}", HttpStatusCode.TooManyRequests, json) }

        assertEquals(UBICACION_POR_DEFECTO, proveedor.obtenerUbicacionActual().getOrThrow())
        assertTrue(UBICACION_POR_DEFECTO.aproximada)
    }

    @Test
    fun sinConexionNuncaFallaYUsaElRespaldo() = runTest {
        val proveedor = proveedor { throw IllegalStateException("sin red") }

        val resultado = proveedor.obtenerUbicacionActual()

        assertTrue(resultado.isSuccess)
        assertEquals(UBICACION_POR_DEFECTO, resultado.getOrThrow())
    }

    @Test
    fun unaRespuestaIncompletaOQueNoEsJsonCaeAlRespaldo() = runTest {
        val sinCoordenadas = proveedor { respond("""{"success":true}""", headers = json) }
        val basura = proveedor { respond("<html>error</html>", headers = json) }

        assertEquals(UBICACION_POR_DEFECTO, sinCoordenadas.obtenerUbicacionActual().getOrThrow())
        assertEquals(UBICACION_POR_DEFECTO, basura.obtenerUbicacionActual().getOrThrow())
    }
}
