package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.config.ConfigFirebaseEscritorio
import com.example.cambiateappkmp.data.mapper.CAMPOS_EDITABLES_OUTFIT
import com.example.cambiateappkmp.data.mapper.ordenadosPorFecha
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.repository.AuthRepository
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private const val COLECCION_OUTFITS = "outfits"
private const val TIMEOUT_ESCRITURA_MS = 10_000L
private const val INTERVALO_POR_DEFECTO_MS = 5_000L

/**
 * Outfits de escritorio: habla con Firestore por su API REST, con el idToken del usuario (así se aplican las mismas
 * reglas de seguridad que en Android). Sigue el mismo patrón que [PrendaRepositoryRest]. La colección y los campos son
 * los mismos que usa la app nativa.
 */
internal class OutfitsRepositoryRest(
    private val cliente: HttpClient,
    private val config: ConfigFirebaseEscritorio,
    private val tokens: TokenProvider,
    private val auth: AuthRepository,
    private val intervaloMs: Long = INTERVALO_POR_DEFECTO_MS,
) : OutfitsRepository {

    /** Se emite después de cada escritura para refrescar la lista enseguida, sin esperar al próximo sondeo. */
    private val refrescos = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarOutfits(): Flow<List<Outfit>> =
        auth.usuarioActual.flatMapLatest { usuario ->
            if (usuario == null) flowOf(emptyList()) else outfitsDe(usuario.id)
        }

    /** Firestore REST no tiene listeners: se consulta cada pocos segundos, solo mientras haya alguien escuchando. */
    private fun outfitsDe(uid: String): Flow<List<Outfit>> = flow {
        var hayDatos = false
        emitAll(
            merge(sondeo(), refrescos)
                .transform {
                    listar(uid).fold(
                        onSuccess = { outfits ->
                            hayDatos = true
                            emit(outfits)
                        },
                        onFailure = { error ->
                            // El primer error se informa (la pantalla muestra el reintento).
                            // Si ya había datos, un fallo pasajero se ignora y se reintenta en el próximo sondeo.
                            if (!hayDatos) throw error
                        },
                    )
                },
        )
    }.distinctUntilChanged()

    private fun sondeo(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(intervaloMs)
        }
    }

    override suspend fun listarOutfits(): Result<List<Outfit>> {
        val uid = tokens.uid ?: return Result.failure(DomainException.SesionNoIniciada)
        return listar(uid)
    }

    override suspend fun guardarOutfit(outfit: Outfit): Result<Unit> {
        val uid = tokens.uid ?: return Result.failure(DomainException.SesionNoIniciada)

        val resultado = withTimeoutOrNull(TIMEOUT_ESCRITURA_MS) {
            if (outfit.esNuevo) {
                // Outfit nuevo: Firestore le pone el id y se escriben todos los campos.
                enviar(
                    metodo = HttpMethod.Post,
                    url = "${urlBase()}/$COLECCION_OUTFITS",
                    cuerpo = DocumentoFirestore(null, outfit.aCamposFirestore(uid)).aJsonCuerpo(),
                )
            } else {
                // Edición: equivale al set con mergeFields de Android. Con updateMask solo se tocan ocasión,
                // prendas y favorito, nunca el userId ni la fecha.
                enviar(
                    metodo = HttpMethod.Patch,
                    url = "${urlBase()}/$COLECCION_OUTFITS/${outfit.id}",
                    cuerpo = DocumentoFirestore(null, outfit.aCamposEditablesFirestore(uid)).aJsonCuerpo(),
                    parametros = CAMPOS_EDITABLES_OUTFIT.map { "updateMask.fieldPaths" to it },
                )
            }
        } ?: return Result.failure(DomainException.SinConexion)

        return resultadoDeEscritura(resultado)
    }

    override suspend fun eliminarOutfit(id: String): Result<Unit> {
        if (tokens.uid == null) return Result.failure(DomainException.SesionNoIniciada)

        val resultado = withTimeoutOrNull(TIMEOUT_ESCRITURA_MS) {
            enviar(HttpMethod.Delete, "${urlBase()}/$COLECCION_OUTFITS/$id")
        } ?: return Result.failure(DomainException.SinConexion)

        return resultadoDeEscritura(resultado)
    }

    override suspend fun cambiarFavorito(id: String, favorito: Boolean): Result<Unit> {
        if (tokens.uid == null) return Result.failure(DomainException.SesionNoIniciada)

        val resultado = withTimeoutOrNull(TIMEOUT_ESCRITURA_MS) {
            // Solo el campo favorito. Con "exists" el outfit tiene que existir (como el update de Android):
            // si no, el PATCH crearía un documento con un solo campo.
            enviar(
                metodo = HttpMethod.Patch,
                url = "${urlBase()}/$COLECCION_OUTFITS/$id",
                cuerpo = DocumentoFirestore(null, mapOf("favorito" to ValorFirestore.Booleano(favorito))).aJsonCuerpo(),
                parametros = listOf("updateMask.fieldPaths" to "favorito", "currentDocument.exists" to "true"),
            )
        } ?: return Result.failure(DomainException.SinConexion)

        return resultadoDeEscritura(resultado)
    }

    private fun resultadoDeEscritura(resultado: Result<HttpResponse>): Result<Unit> =
        resultado.fold(
            onSuccess = { respuesta ->
                if (respuesta.status.isSuccess()) {
                    refrescos.tryEmit(Unit)
                    Result.success(Unit)
                } else {
                    Result.failure(errorHttp(respuesta.status))
                }
            },
            onFailure = { Result.failure(it) },
        )

    /** Outfits del usuario: consulta con filtro userId == uid (lo que exigen las reglas), ordenados por fecha acá. */
    private suspend fun listar(uid: String): Result<List<Outfit>> {
        val respuesta = enviar(HttpMethod.Post, "${urlBase()}:runQuery", consultaPorUsuario(uid))
            .getOrElse { return Result.failure(it) }
        if (!respuesta.status.isSuccess()) return Result.failure(errorHttp(respuesta.status))

        return try {
            // Responde una lista; si no hay resultados, trae un único elemento sin "document".
            val outfits = leerJson(respuesta) { it.jsonArray }
                .mapNotNull { (it as? JsonObject)?.get("document") }
                .mapNotNull { DocumentoFirestore.desdeJson(it).aOutfit() } // una ocasión desconocida se descarta
                .ordenadosPorFecha()
            Result.success(outfits)
        } catch (e: DomainException) {
            Result.failure(e)
        }
    }

    private fun consultaPorUsuario(uid: String): JsonObject = buildJsonObject {
        putJsonObject("structuredQuery") {
            putJsonArray("from") { addJsonObject { put("collectionId", COLECCION_OUTFITS) } }
            putJsonObject("where") {
                putJsonObject("fieldFilter") {
                    putJsonObject("field") { put("fieldPath", "userId") }
                    put("op", "EQUAL")
                    putJsonObject("value") { put("stringValue", uid) }
                }
            }
        }
    }

    /**
     * Manda el pedido con el idToken. Si Firestore responde 401, renueva el token una vez y reintenta.
     * Devuelve la respuesta tal cual (aunque sea un error HTTP); falla solo si no hay sesión o no hay conexión.
     */
    private suspend fun enviar(
        metodo: HttpMethod,
        url: String,
        cuerpo: JsonElement? = null,
        parametros: List<Pair<String, String>> = emptyList(),
    ): Result<HttpResponse> {
        if (config.projectId.isBlank()) {
            return Result.failure(
                DomainException.Desconocido(IllegalStateException("Falta FIREBASE_PROJECT_ID en local.properties")),
            )
        }

        var renovar = false
        for (intento in 0..1) {
            val token = tokens.idTokenVigente(forzar = renovar).getOrElse { return Result.failure(it) }

            val respuesta = try {
                cliente.request(url) {
                    method = metodo
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parametros.forEach { (nombre, valor) -> parameter(nombre, valor) }
                    if (cuerpo != null) {
                        contentType(ContentType.Application.Json)
                        setBody(cuerpo)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Si ni siquiera llegó una respuesta, lo tratamos como falta de conexión.
                return Result.failure(DomainException.SinConexion)
            }

            if (respuesta.status == HttpStatusCode.Unauthorized && intento == 0) {
                renovar = true
                continue
            }
            return Result.success(respuesta)
        }
        return Result.failure(DomainException.SesionNoIniciada)
    }

    private suspend fun <T> leerJson(respuesta: HttpResponse, transformar: (JsonElement) -> T): T =
        try {
            transformar(jsonFirebase.parseToJsonElement(respuesta.bodyAsText()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DomainException.Desconocido(e)
        }

    private fun errorHttp(estado: HttpStatusCode): DomainException = when (estado.value) {
        401 -> DomainException.SesionNoIniciada
        403 -> DomainException.SinPermisos
        404 -> DomainException.Desconocido(IllegalStateException("No se encontró el documento en Firestore (404)"))
        else -> DomainException.Desconocido(IllegalStateException("Firestore respondió ${estado.value}"))
    }

    private fun urlBase() =
        "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents"
}
