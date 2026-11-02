package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.data.fake.FakeAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveAuthStateUseCaseTest {

    private val repo = FakeAuthRepository()
    private val useCase = ObserveAuthStateUseCase(repo)

    @Test
    fun sinSesionEmiteNull() = runTest {
        assertNull(useCase().first())
    }

    @Test
    fun alIniciarSesionEmiteElUsuarioYAlCerrarloEmiteNull() = runTest {
        val usuario = repo.login(FakeAuthRepository.EMAIL_DEMO, FakeAuthRepository.PASS_DEMO).getOrThrow()
        assertEquals(usuario, useCase().first())

        repo.cerrarSesion()
        assertNull(useCase().first())
    }
}
