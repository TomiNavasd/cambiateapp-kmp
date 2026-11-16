package com.example.cambiateappkmp.presentation.mapa

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class CargadorDeTilesTest {

    private fun png(): ByteArray {
        val imagen = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
        return ByteArrayOutputStream().also { ImageIO.write(imagen, "png", it) }.toByteArray()
    }

    private suspend fun esperar(condicion: () -> Boolean) = withTimeout(10_000) {
        while (!condicion()) delay(10)
    }

    private fun conCargador(
        cliente: HttpClient,
        ahora: () -> Long = { System.currentTimeMillis() },
        bloque: suspend (CargadorDeTiles) -> Unit,
    ) = runBlocking {
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            bloque(CargadorDeTiles(cliente, alcance, ahora = ahora))
        } finally {
            alcance.cancel()
        }
    }

    @Test
    fun descargaDecodificaYGuardaElTile() {
        val cliente = HttpClient(MockEngine { respond(png(), headers = headersOf(HttpHeaders.ContentType, "image/png")) })
        val clave = ClaveTile(5, 10, 12)

        conCargador(cliente) { cargador ->
            cargador.solicitar(clave)
            esperar { clave in cargador.tiles }
            assertEquals(4, cargador.tiles[clave]!!.width)
        }
    }

    @Test
    fun unTileQueYaEstaEnCacheNoSeVuelveAPedir() {
        val pedidos = AtomicInteger()
        val cliente = HttpClient(MockEngine { pedidos.incrementAndGet(); respond(png()) })
        val clave = ClaveTile(5, 1, 1)

        conCargador(cliente) { cargador ->
            cargador.solicitar(clave)
            esperar { clave in cargador.tiles }
            cargador.solicitar(clave)
            cargador.solicitar(clave)
            delay(100)
        }

        assertEquals(1, pedidos.get())
    }

    @Test
    fun noDescargaMasDeDosALaVez() {
        val simultaneos = AtomicInteger()
        val maximo = AtomicInteger()
        val cliente = HttpClient(
            MockEngine {
                val ahora = simultaneos.incrementAndGet()
                maximo.updateAndGet { maxOf(it, ahora) }
                delay(80)
                simultaneos.decrementAndGet()
                respond(png())
            },
        )

        conCargador(cliente) { cargador ->
            val claves = (0 until 8).map { ClaveTile(6, it, 0) }
            claves.forEach(cargador::solicitar)
            esperar { claves.all { it in cargador.tiles } }
        }

        assertTrue(maximo.get() in 1..2, "hubo ${maximo.get()} descargas al mismo tiempo")
    }

    @Test
    fun siFallaNoReintentaHastaPasadaLaEspera() {
        val pedidos = AtomicInteger()
        var reloj = 1_000L
        val cliente = HttpClient(MockEngine { pedidos.incrementAndGet(); respond("", HttpStatusCode.NotFound) })
        val clave = ClaveTile(5, 2, 2)

        conCargador(cliente, ahora = { reloj }) { cargador ->
            cargador.solicitar(clave)
            esperar { pedidos.get() == 1 }
            delay(100)

            cargador.solicitar(clave) // enseguida: no debería pedirlo de nuevo
            delay(100)
            assertEquals(1, pedidos.get())

            reloj += 10 * 60 * 1000L // pasó un buen rato
            cargador.solicitar(clave)
            esperar { pedidos.get() == 2 }
        }

        assertTrue(pedidos.get() == 2)
    }

    @Test
    fun unaImagenCorruptaNoRompeNada() {
        val cliente = HttpClient(MockEngine { respond(byteArrayOf(1, 2, 3, 4)) })
        val clave = ClaveTile(5, 3, 3)

        conCargador(cliente) { cargador ->
            cargador.solicitar(clave)
            delay(300)
            assertTrue(clave !in cargador.tiles)
        }
    }

    @Test
    fun laCacheEnMemoriaTieneUnTopeDe200Tiles() {
        val cliente = HttpClient(MockEngine { respond(png()) })

        conCargador(cliente) { cargador ->
            val claves = (0 until 230).map { ClaveTile(8, it, 0) }
            claves.forEach(cargador::solicitar)
            esperar { claves.last() in cargador.tiles && claves.all { it in cargador.tiles || cargador.tiles.size >= 200 } }
            delay(300)
            assertTrue(cargador.tiles.size <= 200, "había ${cargador.tiles.size} tiles")
        }
    }
}
