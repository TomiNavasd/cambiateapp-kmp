package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.data.remote.CloudinaryConfig
import com.example.cambiateappkmp.data.remote.LectorDeImagen
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
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class CloudinaryImageStorageRepositoryTest {

    private class LectorFalso(private val falla: Boolean = false) : LectorDeImagen {
        override suspend fun leerBytes(uri: String): ByteArray {
            if (falla) error("no se pudo leer")
            return byteArrayOf(1, 2, 3)
        }
    }

    private fun clienteCon(
        status: HttpStatusCode,
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
    fun devuelve_la_url_cuando_cloudinary_responde_bien() = runTest {
        val cliente = clienteCon(
            HttpStatusCode.OK,
            """{"secure_url":"https://res.cloudinary.com/demo/foto.jpg","public_id":"abc","bytes":123}""",
        )
        val repo = CloudinaryImageStorageRepository(cliente, LectorFalso())

        val resultado = repo.subirImagen("content://foto")

        assertTrue(resultado.isSuccess)
        assertEquals("https://res.cloudinary.com/demo/foto.jpg", resultado.getOrNull())
    }

    @Test
    fun falla_cuando_cloudinary_responde_con_error() = runTest {
        val cliente = clienteCon(HttpStatusCode.BadRequest, """{"error":{"message":"Upload preset not found"}}""")
        val repo = CloudinaryImageStorageRepository(cliente, LectorFalso())

        val resultado = repo.subirImagen("content://foto")

        assertTrue(resultado.isFailure)
    }

    @Test
    fun falla_cuando_no_se_puede_leer_la_imagen() = runTest {
        val cliente = clienteCon(HttpStatusCode.OK, """{"secure_url":"https://x/y.jpg"}""")
        val repo = CloudinaryImageStorageRepository(cliente, LectorFalso(falla = true))

        val resultado = repo.subirImagen("content://foto")

        assertTrue(resultado.isFailure)
    }

    @Test
    fun hace_un_post_a_la_url_de_cloudinary_con_el_cloud_name() = runTest {
        var pedido: HttpRequestData? = null
        val cliente = clienteCon(HttpStatusCode.OK, """{"secure_url":"https://x/y.jpg"}""") { pedido = it }
        val repo = CloudinaryImageStorageRepository(cliente, LectorFalso())

        repo.subirImagen("content://foto")

        assertEquals(HttpMethod.Post, pedido?.method)
        assertEquals(
            "https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/image/upload",
            pedido?.url.toString(),
        )
    }
}
