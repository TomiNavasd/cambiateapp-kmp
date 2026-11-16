package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.config.ConfigFirebaseEscritorio
import com.example.cambiateappkmp.data.config.DesktopApiKeys
import com.example.cambiateappkmp.data.location.UbicacionProviderJvm
import com.example.cambiateappkmp.data.notification.NotificadorEscritorio
import com.example.cambiateappkmp.data.preferences.AlmacenClaveValor
import com.example.cambiateappkmp.data.preferences.AlmacenClaveValorEnMemoria
import com.example.cambiateappkmp.data.rest.AuthRepositoryRest
import com.example.cambiateappkmp.data.rest.PrendaRepositoryRest
import com.example.cambiateappkmp.data.rest.TokenProvider
import com.example.cambiateappkmp.domain.config.ApiKeys
import com.example.cambiateappkmp.domain.notification.Notificador
import com.example.cambiateappkmp.domain.repository.AuthRepository
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import com.example.cambiateappkmp.domain.repository.ImageStorageRepository
import com.example.cambiateappkmp.domain.repository.LocalesRepository
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import com.example.cambiateappkmp.presentation.ajustes.AjustesViewModel
import com.example.cambiateappkmp.presentation.altaedicion.AltaEdicionViewModel
import com.example.cambiateappkmp.presentation.auth.LoginViewModel
import com.example.cambiateappkmp.presentation.auth.RegisterViewModel
import com.example.cambiateappkmp.presentation.avisos.AvisosEnApp
import com.example.cambiateappkmp.presentation.detalle.DetallePrendaViewModel
import com.example.cambiateappkmp.presentation.mapa.MapaViewModel
import com.example.cambiateappkmp.presentation.navigation.NavegacionViewModel
import com.example.cambiateappkmp.presentation.notificaciones.NotificacionesViewModel
import com.example.cambiateappkmp.presentation.session.SessionViewModel
import com.example.cambiateappkmp.presentation.wardrobe.WardrobeViewModel
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

/**
 * Arma el grafo de Koin de escritorio con los módulos REALES (no los fake) y comprueba que todo se puede crear.
 * No hace pedidos de red. Si falta una dependencia en algún módulo, falla acá y no al abrir la ventana.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DesktopKoinTest {

    private val koin = koinApplication {
        modules(
            authModule,
            prendasModule,
            imagenesModule,
            climaModule,
            ubicacionModule,
            mapaModule,
            ajustesModule,
            notificacionesModule,
            outfitsModule,
            iaModule,
            presentationModule,
            // Lo mismo que registra main.kt, más un almacén en memoria para no escribir en el registro de Windows.
            module {
                single { ConfigFirebaseEscritorio(apiKey = "clave-de-prueba", projectId = "proyecto-de-prueba") }
                single<ApiKeys> { DesktopApiKeys() }
                single<AlmacenClaveValor> { AlmacenClaveValorEnMemoria() }
            },
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
    fun enEscritorioLosRepositoriosRealesSonLasVersionesRest() {
        assertIs<AuthRepositoryRest>(koin.get<AuthRepository>())
        assertIs<PrendaRepositoryRest>(koin.get<PrendaRepository>())
        assertIs<UbicacionProviderJvm>(koin.get<UbicacionProvider>())
        assertIs<NotificadorEscritorio>(koin.get<Notificador>())
    }

    @Test
    fun elMismoObjetoAtiendeComoRepositorioDeAuthYComoProveedorDelToken() {
        assertSame<Any>(koin.get<AuthRepository>(), koin.get<TokenProvider>())
    }

    @Test
    fun seArmanLosRepositoriosYServiciosCompartidos() {
        koin.get<ClimaRepository>()
        koin.get<LocalesRepository>()
        koin.get<ImageStorageRepository>()
        koin.get<PreferenciasRepository>()
        koin.get<AvisosEnApp>()
        koin.get<ApiKeys>()
    }

    @Test
    fun seArmanLosViewModelsDeTodasLasPantallas() {
        koin.get<LoginViewModel>()
        koin.get<RegisterViewModel>()
        koin.get<SessionViewModel>()
        koin.get<NavegacionViewModel>()
        koin.get<WardrobeViewModel>()
        koin.get<DetallePrendaViewModel>()
        koin.get<AltaEdicionViewModel>()
        koin.get<MapaViewModel>()
        koin.get<AjustesViewModel>()
        koin.get<NotificacionesViewModel>()
    }
}
