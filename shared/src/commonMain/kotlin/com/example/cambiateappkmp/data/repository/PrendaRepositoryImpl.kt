package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.data.mapper.aDomain
import com.example.cambiateappkmp.data.mapper.aDomainException
import com.example.cambiateappkmp.data.mapper.aDto
import com.example.cambiateappkmp.data.remote.PrendaDto
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

private const val COLECCION_PRENDAS = "prendas"
private const val TIMEOUT_ESCRITURA_MS = 10_000L

class PrendaRepositoryImpl(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : PrendaRepository {

    private val coleccion get() = firestore.collection(COLECCION_PRENDAS)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarPrendas(): Flow<List<Prenda>> =
        auth.authStateChanged
            .flatMapLatest { usuario ->
                if (usuario == null) {
                    flowOf(emptyList())
                } else {
                    coleccion.where { "userId" equalTo usuario.uid }.snapshots
                        .map { snapshot ->
                            snapshot.documents.mapNotNull { doc ->
                                doc.data<PrendaDto>().aDomain(doc.id)
                            }
                        }
                }
            }
            .catch { error -> throw error.aDomainException() }

    override suspend fun obtenerPrenda(id: String): Prenda? {
        val uid = auth.currentUser?.uid ?: return null
        try {
            val doc = coleccion.document(id).get()
            if (!doc.exists) return null
            val dto = doc.data<PrendaDto>()
            return if (dto.userId == uid) dto.aDomain(doc.id) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.aDomainException()
        }
    }

    override suspend fun guardarPrenda(prenda: Prenda): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(DomainException.SesionNoIniciada)
        val dto = prenda.aDto(uid)
        return try {

            val terminado = withTimeoutOrNull(TIMEOUT_ESCRITURA_MS) {
                if (prenda.esNueva) {
                    coleccion.add(dto)
                } else {
                    coleccion.document(prenda.id).set(dto, merge = true)
                }
            }
            if (terminado == null) {
                Result.failure(DomainException.SinConexion)
            } else {
                Result.success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.aDomainException())
        }
    }
}