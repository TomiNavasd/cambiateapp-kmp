package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.data.fake.FakePreferenciasRepository
import com.example.cambiateappkmp.data.preferences.AlmacenClaveValorEnMemoria
import com.example.cambiateappkmp.domain.model.ConfigNotificaciones
import com.example.cambiateappkmp.domain.model.TemaApp
import com.example.cambiateappkmp.domain.model.Ubicacion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class PreferenciasRepositoryImplTest {

    private val almacen = AlmacenClaveValorEnMemoria()
    private val repo = PreferenciasRepositoryImpl(almacen)

    @Test
    fun losValoresPorDefectoSonTemaDelSistemaYRecordatoriosApagadosALas20() = runTest {
        assertEquals(TemaApp.SISTEMA, repo.tema.first())
        assertEquals(ConfigNotificaciones(activadas = false, hora = 20, minuto = 0), repo.notificaciones.first())
        assertNull(repo.ultimaUbicacion())
    }

    @Test
    fun guardarYLeerElTema() = runTest {
        repo.guardarTema(TemaApp.OSCURO)

        assertEquals(TemaApp.OSCURO, repo.tema.first())

        repo.guardarTema(TemaApp.CLARO)

        assertEquals(TemaApp.CLARO, repo.tema.first())
    }

    @Test
    fun guardarYLeerLaConfiguracionDeNotificaciones() = runTest {
        val config = ConfigNotificaciones(activadas = true, hora = 8, minuto = 30)

        repo.guardarNotificaciones(config)

        assertEquals(config, repo.notificaciones.first())
    }

    @Test
    fun loGuardadoSobreviveAUnaNuevaInstanciaConElMismoAlmacen() = runTest {
        repo.guardarTema(TemaApp.OSCURO)
        repo.guardarNotificaciones(ConfigNotificaciones(activadas = true, hora = 7, minuto = 15))
        repo.guardarUltimaUbicacion(Ubicacion(lat = -34.6037, lng = -58.3816))

        // Es como cerrar y volver a abrir la app: se crea un repositorio nuevo sobre el mismo almacén.
        val reabierto = PreferenciasRepositoryImpl(almacen)

        assertEquals(TemaApp.OSCURO, reabierto.tema.first())
        assertEquals(ConfigNotificaciones(true, 7, 15), reabierto.notificaciones.first())
        assertEquals(Ubicacion(lat = -34.6037, lng = -58.3816), reabierto.ultimaUbicacion())
    }

    @Test
    fun unaHoraFueraDeRangoSeCorrige() = runTest {
        repo.guardarNotificaciones(ConfigNotificaciones(activadas = true, hora = 25, minuto = 99))

        assertEquals(ConfigNotificaciones(true, 23, 59), repo.notificaciones.first())
    }

    @Test
    fun guardarYLeerLaUltimaUbicacion() = runTest {
        repo.guardarUltimaUbicacion(Ubicacion(lat = -31.4201, lng = -64.1888))

        assertEquals(Ubicacion(lat = -31.4201, lng = -64.1888), repo.ultimaUbicacion())
    }

    @Test
    fun unValorCorruptoEnElAlmacenVuelveAlPorDefecto() = runTest {
        almacen.guardar("tema", "NO_EXISTE")
        almacen.guardar("notificaciones_hora", "abc")
        almacen.guardar("notificaciones_activadas", "quizas")
        almacen.guardar("ultima_ubicacion_lat", "no-es-un-numero")
        almacen.guardar("ultima_ubicacion_lng", "-58.0")

        val reabierto = PreferenciasRepositoryImpl(almacen)

        assertEquals(TemaApp.SISTEMA, reabierto.tema.first())
        assertEquals(ConfigNotificaciones(false, 20, 0), reabierto.notificaciones.first())
        assertNull(reabierto.ultimaUbicacion())
    }

    @Test
    fun laVersionFakeEnMemoriaTambienTieneLosValoresPorDefectoYGuarda() = runTest {
        val fake = FakePreferenciasRepository()

        assertEquals(TemaApp.SISTEMA, fake.tema.first())
        assertEquals(ConfigNotificaciones(false, 20, 0), fake.notificaciones.first())

        fake.guardarTema(TemaApp.CLARO)
        fake.guardarNotificaciones(ConfigNotificaciones(true, 9, 5))

        assertEquals(TemaApp.CLARO, fake.tema.first())
        assertEquals(ConfigNotificaciones(true, 9, 5), fake.notificaciones.first())
    }
}
