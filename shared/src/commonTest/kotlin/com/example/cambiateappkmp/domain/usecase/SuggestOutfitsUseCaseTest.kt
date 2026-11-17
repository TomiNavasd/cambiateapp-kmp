package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.data.fake.FakePrendaRepository
import com.example.cambiateappkmp.di.iaFakeModule
import com.example.cambiateappkmp.di.prendasFakeModule
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.OutfitSuggester
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication

class SuggestOutfitsUseCaseTest {

    private class SugeridorEspia : OutfitSuggester {
        var ocasion: Ocasion? = null
        var prendas: List<Prenda>? = null
        var clima: ClimaActual? = null
        override suspend fun sugerir(ocasion: Ocasion, prendas: List<Prenda>, clima: ClimaActual?): Result<List<Outfit>> {
            this.ocasion = ocasion
            this.prendas = prendas
            this.clima = clima
            return Result.success(emptyList())
        }
    }

    @Test
    fun leeLasPrendasActualesYSeLasPasaAlSugeridorConLaOcasionYElClima() = runTest {
        val repo = FakePrendaRepository()
        val espia = SugeridorEspia()
        val clima = ClimaActual(9.0, "Frío", false)

        SuggestOutfitsUseCase(ObservePrendasUseCase(repo), espia)(Ocasion.TRABAJO, clima)

        assertEquals(Ocasion.TRABAJO, espia.ocasion)
        assertEquals(10, espia.prendas?.size)
        assertSame(clima, espia.clima)
    }

    @Test
    fun sinClimaSeLoPasaComoNull() = runTest {
        val espia = SugeridorEspia()

        SuggestOutfitsUseCase(ObservePrendasUseCase(FakePrendaRepository()), espia)(Ocasion.CASUAL)

        assertEquals(null, espia.clima)
    }

    @Test
    fun siNoSePuedenLeerLasPrendasDevuelveElErrorSinLlamarAlSugeridor() = runTest {
        val repo = object : PrendaRepository by FakePrendaRepository() {
            override fun observarPrendas(): Flow<List<Prenda>> = flow { throw DomainException.SinConexion }
        }
        val espia = SugeridorEspia()

        val resultado = SuggestOutfitsUseCase(ObservePrendasUseCase(repo), espia)(Ocasion.CASUAL)

        assertSame(DomainException.SinConexion, resultado.exceptionOrNull())
        assertEquals(null, espia.ocasion)
    }

    @Test
    fun elModuloFakeSugiereConLasPrendasFake() = runTest {
        val koin = koinApplication { modules(prendasFakeModule, iaFakeModule) }.koin

        val outfits = koin.get<SuggestOutfitsUseCase>()(Ocasion.CASUAL).getOrThrow()

        assertTrue(outfits.isNotEmpty())
        val idsFake = (1..10).map { it.toString() }.toSet()
        assertTrue(outfits.all { o -> o.prendasIds.all { it in idsFake } })
        koin.close()
    }
}
