package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.data.remote.ClimaActualDto
import com.example.cambiateappkmp.data.remote.OpenMeteoRespuesta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClimaMapperTest {

    @Test
    fun descripcionesDeLosCodigosDeClima() {
        assertEquals("Despejado", descripcionClima(0))
        assertEquals("Parcialmente nublado", descripcionClima(2))
        assertEquals("Nublado", descripcionClima(3))
        assertEquals("Niebla", descripcionClima(45))
        assertEquals("Llovizna", descripcionClima(53))
        assertEquals("Lluvia", descripcionClima(63))
        assertEquals("Nieve", descripcionClima(73))
        assertEquals("Chubascos", descripcionClima(81))
        assertEquals("Tormenta", descripcionClima(95))
        assertEquals("Tormenta con granizo", descripcionClima(99))
    }

    @Test
    fun unCodigoDesconocidoONuloDiceSinDatos() {
        assertEquals("Sin datos", descripcionClima(null))
        assertEquals("Sin datos", descripcionClima(1234))
    }

    @Test
    fun lluviaLloviznaChubascosYTormentasCuentanComoLluvia() {
        listOf(51, 55, 61, 65, 66, 80, 82, 95, 99).forEach { codigo ->
            assertTrue(llueveSegunCodigo(codigo), "el código $codigo debería contar como lluvia")
        }
    }

    @Test
    fun cieloDespejadoNubladoNieblaYNieveNoCuentanComoLluvia() {
        listOf(0, 1, 2, 3, 45, 71, 75, 85).forEach { codigo ->
            assertFalse(llueveSegunCodigo(codigo), "el código $codigo no debería contar como lluvia")
        }
        assertFalse(llueveSegunCodigo(null))
    }

    @Test
    fun elClimaActualSinTemperaturaNoSeArma() {
        val respuesta = OpenMeteoRespuesta(current = ClimaActualDto(temperatura = null, codigoClima = 0))

        assertEquals(null, respuesta.aClimaActual())
    }

    @Test
    fun elPronosticoSinDatosDiariosNoSeArma() {
        assertEquals(null, OpenMeteoRespuesta().aPronosticoManiana())
    }
}
