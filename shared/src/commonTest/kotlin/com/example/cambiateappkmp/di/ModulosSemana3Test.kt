package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeClimaRepository
import com.example.cambiateappkmp.data.fake.FakeLocalesRepository
import com.example.cambiateappkmp.data.fake.FakePreferenciasRepository
import com.example.cambiateappkmp.data.fake.FakeUbicacionProvider
import com.example.cambiateappkmp.domain.notification.Notificador
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import com.example.cambiateappkmp.domain.repository.LocalesRepository
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import com.example.cambiateappkmp.presentation.ajustes.AjustesViewModel
import com.example.cambiateappkmp.presentation.mapa.MapaViewModel
import com.example.cambiateappkmp.presentation.notificaciones.NotificacionesViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Comprueba que los módulos fake de la semana 3 se arman completos: si falta una dependencia, falla acá. */
@OptIn(ExperimentalCoroutinesApi::class)
class ModulosSemana3Test {

    private class NotificadorDePrueba : Notificador {
        override fun programarDiaria(hora: Int, minuto: Int) = Unit
        override fun cancelarDiaria() = Unit
        override fun mostrarAhora(titulo: String, mensaje: String) = Unit
    }

    // El notificador real de Android necesita un Context: en el test lo reemplazamos por uno de prueba.
    private val koin = koinApplication {
        modules(
            climaFakeModule,
            ubicacionFakeModule,
            mapaFakeModule,
            ajustesFakeModule,
            notificacionesFakeModule,
            module { single<Notificador> { NotificadorDePrueba() } },
        )
    }.koin

    @BeforeTest
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun despues() {
        Dispatchers.resetMain()
    }

    @Test
    fun losModulosFakeUsanLosRepositoriosDeEjemplo() {
        assertIs<FakeClimaRepository>(koin.get<ClimaRepository>())
        assertIs<FakeUbicacionProvider>(koin.get<UbicacionProvider>())
        assertIs<FakeLocalesRepository>(koin.get<LocalesRepository>())
        assertIs<FakePreferenciasRepository>(koin.get<PreferenciasRepository>())
    }

    @Test
    fun losRepositoriosSonUnicos() {
        assertSame(koin.get<PreferenciasRepository>(), koin.get<PreferenciasRepository>())
        assertSame(koin.get<ClimaRepository>(), koin.get<ClimaRepository>())
    }

    @Test
    fun seArmanLosTresViewModelsDeLaSemana() {
        koin.get<MapaViewModel>()
        koin.get<AjustesViewModel>()
        koin.get<NotificacionesViewModel>()
    }
}
