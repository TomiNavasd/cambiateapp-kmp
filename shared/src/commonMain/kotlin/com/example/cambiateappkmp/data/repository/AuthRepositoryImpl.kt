package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.data.mapper.aDomainException
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Usuario
import com.example.cambiateappkmp.domain.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val auth: FirebaseAuth = Firebase.auth
) : AuthRepository {

    override val usuarioActual: Flow<Usuario?> = auth.authStateChanged.map { it?.aUsuario() }

    override suspend fun login(email: String, pass: String): Result<Usuario> =
        intentar { auth.signInWithEmailAndPassword(email, pass).user }

    override suspend fun registrar(email: String, pass: String): Result<Usuario> =
        intentar { auth.createUserWithEmailAndPassword(email, pass).user }

    override suspend fun cerrarSesion() = auth.signOut()

    private suspend fun intentar(accion: suspend () -> FirebaseUser?): Result<Usuario> =
        try {
            val usuario = accion()?.aUsuario()
                ?: throw DomainException.Desconocido(IllegalStateException("Firebase no devolvió usuario"))
            Result.success(usuario)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.aDomainException())
        }

    private fun FirebaseUser.aUsuario() = Usuario(id = uid, email = email.orEmpty())
}
