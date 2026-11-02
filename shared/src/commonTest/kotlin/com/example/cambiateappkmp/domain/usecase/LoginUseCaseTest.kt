package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.data.fake.FakeAuthRepository
import com.example.cambiateappkmp.domain.error.CampoInvalido
import com.example.cambiateappkmp.domain.error.DomainException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LoginUseCaseTest {

    private val repo = FakeAuthRepository()
    private val useCase = LoginUseCase(repo)

    @Test
    fun emailSinFormatoValidoDevuelveEmailInvalido() = runTest {
        val resultado = useCase("hola.com", FakeAuthRepository.PASS_DEMO)

        assertSame(DomainException.EmailInvalido, resultado.exceptionOrNull())
        assertNull(repo.usuarioActual.first())
    }

    @Test
    fun contrasenaVaciaDevuelveValidacionDeContrasena() = runTest {
        val resultado = useCase(FakeAuthRepository.EMAIL_DEMO, "")

        val error = assertIs<DomainException.Validacion>(resultado.exceptionOrNull())
        assertEquals(CampoInvalido.CONTRASENA, error.campo)
        assertNull(repo.usuarioActual.first())
    }

    @Test
    fun datosValidosIniciaSesionConElEmailSinEspacios() = runTest {
        val resultado = useCase("  ${FakeAuthRepository.EMAIL_DEMO}  ", FakeAuthRepository.PASS_DEMO)

        assertTrue(resultado.isSuccess)
        assertEquals(FakeAuthRepository.EMAIL_DEMO, resultado.getOrNull()?.email)
        assertEquals(resultado.getOrNull(), repo.usuarioActual.first())
    }

    @Test
    fun contrasenaIncorrectaDevuelveCredencialesInvalidas() = runTest {
        val resultado = useCase(FakeAuthRepository.EMAIL_DEMO, "otra-clave")

        assertSame(DomainException.CredencialesInvalidas, resultado.exceptionOrNull())
        assertNull(repo.usuarioActual.first())
    }

    @Test
    fun emailNoRegistradoDevuelveCredencialesInvalidas() = runTest {
        val resultado = useCase("nadie@cambiateapp.com", "123456")

        assertSame(DomainException.CredencialesInvalidas, resultado.exceptionOrNull())
    }
}
