package com.example.cambiateappkmp.presentation.mapa

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.Image

/** Cuántos tiles se guardan en memoria. Cada uno ocupa unos 256 KB ya decodificado. */
private const val MAXIMO_EN_MEMORIA = 200

/** Cuánto se espera antes de volver a intentar un tile que falló. */
private const val ESPERA_REINTENTO_MS = 30_000L

/** Tiles descargados de OpenTopoMap. Pide un User-Agent que identifique a la app. */
internal fun crearClienteTiles() = HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
    }
    install(UserAgent) {
        agent = "CambiateApp/1.0 (trabajo practico UNAJ; app de escritorio)"
    }
}

/**
 * Baja los tiles del mapa, los decodifica con Skia y los guarda en una caché en memoria.
 * Descarga como máximo [maximoSimultaneos] a la vez, como piden las políticas de uso de los servidores de mapas.
 * [tiles] es un estado de Compose: al llegar un tile nuevo, el mapa se redibuja solo.
 */
internal class CargadorDeTiles(
    private val cliente: HttpClient,
    private val alcance: CoroutineScope,
    maximoSimultaneos: Int = 2,
    private val urlDe: (ClaveTile) -> String = ::urlDeTileOpenTopo,
    private val ahora: () -> Long = { System.currentTimeMillis() },
) {
    val tiles = mutableStateMapOf<ClaveTile, ImageBitmap>()

    private val limite = Semaphore(maximoSimultaneos)
    private val ordenDeLlegada = ArrayDeque<ClaveTile>()
    private val enCurso = mutableSetOf<ClaveTile>()
    private val fallidos = mutableMapOf<ClaveTile, Long>()

    /** Pide un tile si todavía no lo tenemos, no se está descargando y no falló hace poco. */
    fun solicitar(clave: ClaveTile) {
        synchronized(this) {
            if (clave in tiles || clave in enCurso) return
            val fallo = fallidos[clave]
            if (fallo != null && ahora() - fallo < ESPERA_REINTENTO_MS) return
            enCurso += clave
        }

        alcance.launch {
            val imagen = try {
                limite.withPermit { descargar(clave) }
            } catch (e: CancellationException) {
                synchronized(this@CargadorDeTiles) { enCurso -= clave }
                throw e
            } catch (e: Exception) {
                null
            }

            synchronized(this@CargadorDeTiles) {
                enCurso -= clave
                if (imagen == null) {
                    fallidos[clave] = ahora()
                } else {
                    fallidos -= clave
                    tiles[clave] = imagen
                    ordenDeLlegada.addLast(clave)
                    while (ordenDeLlegada.size > MAXIMO_EN_MEMORIA) {
                        tiles.remove(ordenDeLlegada.removeFirst())
                    }
                }
            }
        }
    }

    private suspend fun descargar(clave: ClaveTile): ImageBitmap? {
        val respuesta = cliente.get(urlDe(clave))
        if (!respuesta.status.isSuccess()) return null
        return Image.makeFromEncoded(respuesta.body<ByteArray>()).toComposeImageBitmap()
    }
}
