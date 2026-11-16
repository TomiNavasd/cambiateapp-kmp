package com.example.cambiateappkmp.presentation.mapa

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapaGeometriaTest {

    private fun cerca(esperado: Double, real: Double, tolerancia: Double = 1e-6) =
        assertTrue(abs(esperado - real) <= tolerancia, "esperado $esperado pero fue $real")

    @Test
    fun elCentroDelMundoEstaEnElMedioDeLaGrilla() {
        cerca(1.0, tileX(0.0, 1))
        cerca(1.0, tileY(0.0, 1))
        cerca(4.0, tileX(0.0, 3))
    }

    @Test
    fun losExtremosDeLongitudVanDeCeroAlTotalDeTiles() {
        cerca(0.0, tileX(-180.0, 5))
        cerca(32.0, tileX(180.0, 5))
    }

    @Test
    fun buenosAiresCaeEnElTileConocidoDeZoom10() {
        // El Obelisco en OpenStreetMap, zoom 10: x=345, y=617.
        assertEquals(345, tileX(-58.3816, 10).toInt())
        assertEquals(617, tileY(-34.6037, 10).toInt())
    }

    @Test
    fun laLatitudSeLimitaALaProyeccion() {
        assertTrue(tileY(90.0, 4) >= 0.0)
        assertTrue(tileY(-90.0, 4) <= 16.0)
        cerca(tileY(85.0511, 4), tileY(89.9, 4))
    }

    @Test
    fun laConversionInversaDevuelveLasMismasCoordenadas() {
        listOf(-34.6037 to -58.3816, 40.4168 to -3.7038, -33.8688 to 151.2093).forEach { (lat, lng) ->
            listOf(3, 10, 17).forEach { zoom ->
                cerca(lat, latDeTileY(tileY(lat, zoom), zoom), 1e-6)
                cerca(lng, lngDeTileX(tileX(lng, zoom), zoom), 1e-6)
            }
        }
    }

    @Test
    fun conUnaVistaChicaHaceFaltaUnSoloTile() {
        // Vista de 100x100 px en el medio de un tile: alcanza con ese tile.
        val claves = clavesVisibles(centroTileX = 5.5, centroTileY = 7.5, zoom = 4, anchoPx = 100, altoPx = 100)

        assertEquals(listOf(ClaveTile(4, 5, 7)), claves)
    }

    @Test
    fun losTilesVienenOrdenadosDelMasCercanoAlCentro() {
        val claves = clavesVisibles(5.5, 7.5, 4, anchoPx = 600, altoPx = 600)

        assertEquals(ClaveTile(4, 5, 7), claves.first())
        assertEquals(claves.size, claves.distinct().size)
        assertTrue(claves.size >= 9)
    }

    @Test
    fun laLongitudDaLaVueltaAlMundoYLaLatitudNo() {
        val enElBorde = clavesVisibles(centroTileX = 0.0, centroTileY = 0.0, zoom = 2, anchoPx = 512, altoPx = 512)

        assertTrue(enElBorde.all { it.x in 0..3 }, "x se acomoda a la grilla (da la vuelta)")
        assertTrue(enElBorde.any { it.x == 3 }, "del otro lado del mundo aparece el último tile")
        assertTrue(enElBorde.all { it.y in 0..3 }, "y nunca sale de la grilla")
    }

    @Test
    fun unaVistaSinTamanoNoNecesitaTiles() {
        assertEquals(emptyList(), clavesVisibles(1.0, 1.0, 3, 0, 300))
        assertEquals(emptyList(), clavesVisibles(1.0, 1.0, 3, 300, -1))
    }

    @Test
    fun laUrlDelTileUsaOpenTopoMapYReparteLosServidores() {
        assertEquals("https://a.tile.opentopomap.org/10/345/617.png".substringAfter("//").substringAfter("/"), "10/345/617.png")
        val servidores = (0..5).map { urlDeTileOpenTopo(ClaveTile(10, it, 0)).substringAfter("https://").substringBefore(".") }
        assertEquals(setOf("a", "b", "c"), servidores.toSet())
        assertEquals("https://b.tile.opentopomap.org/10/1/0.png", urlDeTileOpenTopo(ClaveTile(10, 1, 0)))
    }
}
