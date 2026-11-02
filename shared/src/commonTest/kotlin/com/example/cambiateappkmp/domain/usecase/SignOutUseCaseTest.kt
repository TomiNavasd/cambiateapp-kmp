package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.data.fake.FakeAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SignOutUseCaseTest {

    private val repo = FakeAuthRepository()
    private val useCase = SignOutUseCase(repo)

    @Test
    fun cerrarSesionDejaSinUsuario() = runTest {
        repo.login(FakeAuthRepository.EMAIL_DEMO, FakeAuthRepository.PASS_DEMO)
        assertNotNull(repo.usuarioActual.first())

        useCase()

        assertNull(repo.usuarioActual.first())
    }

    @Test
    fun cerrarSesionSinSesionIniciadaNoFalla() = runTest {
        useCase()

        assertNull(repo.usuarioActual.first())
    }
}
