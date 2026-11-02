package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeAuthRepository
import com.example.cambiateappkmp.domain.repository.AuthRepository
import com.example.cambiateappkmp.domain.usecase.LoginUseCase
import com.example.cambiateappkmp.domain.usecase.ObserveAuthStateUseCase
import com.example.cambiateappkmp.domain.usecase.RegisterUseCase
import com.example.cambiateappkmp.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class AuthModuleTest {

    private val koin = koinApplication { modules(authFakeModule) }.koin

    @Test
    fun elModuloFakeUsaElFakeComoUnicoRepositorio() {
        val repo = koin.get<AuthRepository>()

        assertIs<FakeAuthRepository>(repo)
        assertSame(repo, koin.get<AuthRepository>())
    }

    @Test
    fun elModuloFakeArmaLosCuatroCasosDeUso() {
        koin.get<LoginUseCase>()
        koin.get<RegisterUseCase>()
        koin.get<SignOutUseCase>()
        koin.get<ObserveAuthStateUseCase>()
    }

    @Test
    fun losCasosDeUsoCompartenLaMismaSesion() = runTest {
        val observar = koin.get<ObserveAuthStateUseCase>()
        assertNull(observar().first())

        val usuario = koin.get<LoginUseCase>()(FakeAuthRepository.EMAIL_DEMO, FakeAuthRepository.PASS_DEMO).getOrThrow()
        assertEquals(usuario, observar().first())

        koin.get<SignOutUseCase>()()
        assertNull(observar().first())
    }
}
