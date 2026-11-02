package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.data.fake.FakeAuthRepository
import com.example.cambiateappkmp.domain.error.DomainException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegisterUseCaseTest {

    private val repo = FakeAuthRepository()
    private val useCase = RegisterUseCase(repo)

    @Test
    fun emailSinFormatoValidoDevuelveEmailInvalido() = runTest {
        val resultado = useCase("sin-arroba", "123456")

        assertSame(DomainException.EmailInvalido, resultado.exceptionOrNull())
        assertNull(repo.usuarioActual.first())
    }

    @Test
    fun contrasenaDeCincoCaracteresDevuelveContrasenaDebilYNoCreaLaCuenta() = runTest {
        val rechazado = useCase("nuevo@cambiateapp.com", "12345")

        assertSame(DomainException.ContrasenaDebil, rechazado.exceptionOrNull())
        assertNull(repo.usuarioActual.first())

        val reintento = useCase("nuevo@cambiateapp.com", "123456")
        assertTrue(reintento.isSuccess)
    }

    @Test
    fun contrasenaDeSeisCaracteresRegistraConElEmailSinEspacios() = runTest {
        val resultado = useCase("  nuevo@cambiateapp.com ", "123456")

        assertTrue(resultado.isSuccess)
        assertEquals("nuevo@cambiateapp.com", resultado.getOrNull()?.email)
        assertEquals(resultado.getOrNull(), repo.usuarioActual.first())
    }

    @Test
    fun emailYaRegistradoDevuelveEmailYaRegistrado() = runTest {
        val resultado = useCase(FakeAuthRepository.EMAIL_DEMO, "123456")

        assertSame(DomainException.EmailYaRegistrado, resultado.exceptionOrNull())
        assertNull(repo.usuarioActual.first())
    }
}
