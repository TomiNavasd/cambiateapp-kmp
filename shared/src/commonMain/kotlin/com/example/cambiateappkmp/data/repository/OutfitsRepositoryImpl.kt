package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.data.mapper.CAMPOS_EDITABLES_OUTFIT
import com.example.cambiateappkmp.data.mapper.aDomain
import com.example.cambiateappkmp.data.mapper.aDomainException
import com.example.cambiateappkmp.data.mapper.aDto
import com.example.cambiateappkmp.data.mapper.ordenadosPorFecha
import com.example.cambiateappkmp.data.remote.OutfitDto
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code
import dev.gitlive.firebase.firestore.QuerySnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

private const val COLECCION_OUTFITS = "outfits"
private const val TIMEOUT_ESCRITURA_MS = 10_000L

/** Outfits en Firestore con GitLive (Android). La colección y los campos son los mismos que usa la app nativa. */
class OutfitsRepositoryImpl(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : OutfitsRepository {

    private val coleccion get() = firestore.collection(COLECCION_OUTFITS)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarOutfits(): Flow<List<Outfit>> =
        auth.authStateChanged
            .flatMapLatest { usuario ->
                if (usuario == null) {
                    flowOf(emptyList())
                } else {
                    coleccion.where { "userId" equalTo usuario.uid }.snapshots.map { it.aOutfits() }
                }
            }
            .catch { error -> throw error.aDomainExceptionDeOutfits() }

    override suspend fun listarOutfits(): Result<List<Outfit>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainException.SesionNoIniciada)
        return try {
            Result.success(coleccion.where { "userId" equalTo uid }.get().aOutfits())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.aDomainExceptionDeOutfits())
        }
    }

    override suspend fun guardarOutfit(outfit: Outfit): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(DomainException.SesionNoIniciada)
        val dto = outfit.aDto(uid)
        return escribir {
            if (outfit.esNuevo) {
                coleccion.add(dto)
            } else {
                // Como el mergeFields de la nativa: solo ocasión, prendas y favorito. El userId y la fecha quedan como están.
                coleccion.document(outfit.id).set(dto, *CAMPOS_EDITABLES_OUTFIT.toTypedArray())
            }
        }
    }

    override suspend fun eliminarOutfit(id: String): Result<Unit> {
        if (auth.currentUser == null) return Result.failure(DomainException.SesionNoIniciada)
        return escribir { coleccion.document(id).delete() }
    }

    override suspend fun cambiarFavorito(id: String, favorito: Boolean): Result<Unit> {
        if (auth.currentUser == null) return Result.failure(DomainException.SesionNoIniciada)
        return escribir { coleccion.document(id).update("favorito" to favorito) }
    }

    /** Si Firestore no confirma en 10 segundos (sin red), se informa como falta de conexión. */
    private suspend fun escribir(operacion: suspend () -> Unit): Result<Unit> = try {
        val terminado = withTimeoutOrNull(TIMEOUT_ESCRITURA_MS) { operacion() }
        if (terminado == null) Result.failure(DomainException.SinConexion) else Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.aDomainExceptionDeOutfits())
    }

    private fun QuerySnapshot.aOutfits(): List<Outfit> =
        documents.mapNotNull { doc -> doc.data<OutfitDto>().aDomain(doc.id) }.ordenadosPorFecha()
}

/** Igual que el resto de los errores, pero los de permisos de Firestore (reglas) se distinguen, como en la nativa. */
private fun Throwable.aDomainExceptionDeOutfits(): DomainException =
    if (this is FirebaseFirestoreException && code == FirestoreExceptionCode.PERMISSION_DENIED) DomainException.SinPermisos else aDomainException()
