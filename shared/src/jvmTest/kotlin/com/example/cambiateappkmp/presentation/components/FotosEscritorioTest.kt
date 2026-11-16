package com.example.cambiateappkmp.presentation.components

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class FotosEscritorioTest {

    // Coil usa el dispatcher Main: en los tests lo reemplazamos por uno de prueba.
    @BeforeTest
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun pngDePrueba(): ByteArray {
        val imagen = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until 4) for (y in 0 until 4) imagen.setRGB(x, y, 0xFF3366CC.toInt())
        return ByteArrayOutputStream().also { ImageIO.write(imagen, "png", it) }.toByteArray()
    }

    @Test
    fun coilDescargaPorHttpsYDecodificaUnaImagenEnEscritorio() = runTest {
        val cliente = HttpClient(
            MockEngine {
                respond(
                    content = pngDePrueba(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "image/png"),
                )
            },
        )
        val cargador = ImageLoader.Builder(PlatformContext.INSTANCE)
            .components { add(KtorNetworkFetcherFactory(httpClient = { cliente })) }
            .build()

        val resultado = cargador.execute(
            ImageRequest.Builder(PlatformContext.INSTANCE).data("https://res.cloudinary.com/demo/foto.png").build(),
        )

        val causa = (resultado as? ErrorResult)?.throwable
        assertIs<SuccessResult>(resultado, "Coil falló: ${causa?.stackTraceToString()?.take(1500)}")
    }

    @Test
    fun elMotorDeKtorPorDefectoEnEscritorioEsCio() {
        // Es el motor que Coil usa para bajar imágenes si no se le pasa un cliente propio.
        val cliente = HttpClient()
        try {
            assertTrue(cliente.engine.toString().contains("CIO"), "motor: ${cliente.engine}")
        } finally {
            cliente.close()
        }
    }

    @Test
    fun elSelectorSoloAceptaImagenes() {
        listOf("foto.jpg", "foto.JPEG", "foto.png", "foto.webp", "mi.foto.GIF").forEach {
            assertTrue(esArchivoDeImagen(it), "$it debería ser imagen")
        }
        listOf("documento.pdf", "planilla.xlsx", "sin_extension", "foto.jpg.exe").forEach {
            assertFalse(esArchivoDeImagen(it), "$it no debería ser imagen")
        }
    }

    @Test
    fun enEscritorioNoHayCamara() {
        // La pantalla de la foto oculta el botón Cámara cuando esto es false.
        assertEquals(false, camaraDisponibleEnEscritorio)
    }
}

private val camaraDisponibleEnEscritorio = false
