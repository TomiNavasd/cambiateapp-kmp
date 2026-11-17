package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.data.fake.FakeOutfitsRepository
import com.example.cambiateappkmp.di.outfitsFakeModule
import com.example.cambiateappkmp.domain.error.CampoInvalido
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication

class OutfitsUseCasesTest {

    private val fecha = Instant.fromEpochSeconds(1_763_000_000)

    private fun outfit(id: String = "", ocasion: Ocasion = Ocasion.CASUAL, vararg prendas: String, favorito: Boolean = false) =
        Outfit(id, ocasion, prendas.toList(), fecha, favorito)

    @Test
    fun noSeGuardaUnOutfitSinPrendas() = runTest {
        val repo = FakeOutfitsRepository(emptyList())

        val resultado = SaveOutfitUseCase(repo)(outfit(prendas = emptyArray()))

        val error = resultado.exceptionOrNull()
        assertIs<DomainException.Validacion>(error)
        assertEquals(CampoInvalido.PRENDAS, error.campo)
        assertEquals(emptyList(), repo.listarOutfits().getOrThrow())
    }

    @Test
    fun seGuardaUnOutfitValido() = runTest {
        val repo = FakeOutfitsRepository(emptyList())

        val resultado = SaveOutfitUseCase(repo)(outfit(ocasion = Ocasion.SALIDA, prendas = arrayOf("1", "4", "6")))

        assertTrue(resultado.isSuccess)
        assertEquals(1, repo.listarOutfits().getOrThrow().size)
    }

    @Test
    fun noSeGuardaUnOutfitConLaMismaOcasionYElMismoConjuntoDePrendas() = runTest {
        val repo = FakeOutfitsRepository(listOf(outfit("o1", Ocasion.CASUAL, "1", "4", "6")))

        // Mismo conjunto en otro orden: es el mismo outfit.
        val resultado = SaveOutfitUseCase(repo)(outfit(ocasion = Ocasion.CASUAL, prendas = arrayOf("6", "1", "4")))

        assertSame(DomainException.OutfitYaGuardado, resultado.exceptionOrNull())
        assertEquals(1, repo.listarOutfits().getOrThrow().size)
    }

    @Test
    fun conOtraOcasionOOtrasPrendasNoEsDuplicado() = runTest {
        val repo = FakeOutfitsRepository(listOf(outfit("o1", Ocasion.CASUAL, "1", "4", "6")))
        val guardar = SaveOutfitUseCase(repo)

        assertTrue(guardar(outfit(ocasion = Ocasion.TRABAJO, prendas = arrayOf("1", "4", "6"))).isSuccess)
        assertTrue(guardar(outfit(ocasion = Ocasion.CASUAL, prendas = arrayOf("1", "4", "7"))).isSuccess)
        assertEquals(3, repo.listarOutfits().getOrThrow().size)
    }

    @Test
    fun actualizarUnOutfitExistenteNoLoTomaPorDuplicadoDeSiMismo() = runTest {
        val repo = FakeOutfitsRepository(listOf(outfit("o1", Ocasion.CASUAL, "1", "4", "6")))

        val resultado = SaveOutfitUseCase(repo)(outfit("o1", Ocasion.CASUAL, "1", "4", "6", favorito = true))

        assertTrue(resultado.isSuccess)
    }

    @Test
    fun siNoSePuedenLeerLosGuardadosElErrorSePropagaYNoSeGuarda() = runTest {
        val error = DomainException.SinConexion
        val repo = object : OutfitsRepository by FakeOutfitsRepository(emptyList()) {
            override suspend fun listarOutfits(): Result<List<Outfit>> = Result.failure(error)
        }

        assertSame(error, SaveOutfitUseCase(repo)(outfit(prendas = arrayOf("1"))).exceptionOrNull())
    }

    @Test
    fun alternarFavoritoPoneYSacaLaMarca() = runTest {
        val repo = FakeOutfitsRepository(listOf(outfit("o1", Ocasion.CASUAL, "1", favorito = false)))
        val alternar = AlternarFavoritoUseCase(repo)

        alternar(repo.listarOutfits().getOrThrow().single())
        assertEquals(true, repo.listarOutfits().getOrThrow().single().favorito)

        alternar(repo.listarOutfits().getOrThrow().single())
        assertEquals(false, repo.listarOutfits().getOrThrow().single().favorito)
    }

    @Test
    fun borrarQuitaElOutfitYObservarLoRefleja() = runTest {
        val repo = FakeOutfitsRepository(listOf(outfit("o1", Ocasion.CASUAL, "1"), outfit("o2", Ocasion.SALIDA, "2")))

        assertTrue(DeleteOutfitUseCase(repo)("o1").isSuccess)

        assertEquals(listOf("o2"), ObserveOutfitsUseCase(repo)().first().map { it.id })
    }

    @Test
    fun elModuloFakeArmaLosCasosDeUso() {
        val koin = koinApplication { modules(outfitsFakeModule) }.koin

        assertIs<SaveOutfitUseCase>(koin.get<SaveOutfitUseCase>())
        assertIs<DeleteOutfitUseCase>(koin.get<DeleteOutfitUseCase>())
        assertIs<AlternarFavoritoUseCase>(koin.get<AlternarFavoritoUseCase>())
        assertIs<ObserveOutfitsUseCase>(koin.get<ObserveOutfitsUseCase>())
        koin.close()
    }
}
