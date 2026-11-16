package com.example.cambiateappkmp.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalesMapperTest {

    @Test
    fun direccionConCalleNumeroYCiudad() {
        val tags = mapOf("addr:street" to "Florida", "addr:housenumber" to "450", "addr:city" to "Buenos Aires")

        assertEquals("Florida 450, Buenos Aires", direccionDe(tags))
    }

    @Test
    fun direccionSoloConCalle() {
        assertEquals("Florida", direccionDe(mapOf("addr:street" to "Florida")))
    }

    @Test
    fun direccionSoloConCiudad() {
        assertEquals("Buenos Aires", direccionDe(mapOf("addr:city" to "Buenos Aires")))
    }

    @Test
    fun unNumeroSinCalleNoArmaDireccion() {
        assertEquals("", direccionDe(mapOf("addr:housenumber" to "450")))
    }

    @Test
    fun sinEtiquetasLaDireccionQuedaVacia() {
        assertEquals("", direccionDe(null))
        assertEquals("", direccionDe(emptyMap()))
    }

    @Test
    fun laDistanciaEntreElMismoPuntoEsCero() {
        assertEquals(0.0, distanciaMetros(-34.6, -58.4, -34.6, -58.4))
    }

    @Test
    fun ungradoDeLatitudSonCercaDeOncientosOnceKilometros() {
        val metros = distanciaMetros(0.0, 0.0, 1.0, 0.0)

        assertTrue(metros in 110_500.0..111_900.0, "dio $metros metros")
    }

    @Test
    fun laConsultaDeOverpassIncluyeNodosYEdificiosDeTiendasDeRopa() {
        val consulta = consultaOverpass(-34.6037, -58.3816, 1500)

        assertTrue(consulta.contains("node[\"shop\"=\"clothes\"](around:1500,-34.6037,-58.3816)"))
        assertTrue(consulta.contains("way[\"shop\"=\"clothes\"](around:1500,-34.6037,-58.3816)"))
        assertTrue(consulta.contains("out center tags"))
    }
}
