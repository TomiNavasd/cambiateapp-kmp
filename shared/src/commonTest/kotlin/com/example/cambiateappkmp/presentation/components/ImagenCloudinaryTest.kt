package com.example.cambiateappkmp.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ImagenCloudinaryTest {

    private val base = "https://res.cloudinary.com/demo/image/upload"

    @Test
    fun unaUrlDeCloudinarySinTransformacionesRecibeLaDeLaGrilla() {
        val url = "$base/v1763000000/prendas/jean.jpg"

        assertEquals(
            "$base/w_400,h_400,c_fill,q_auto,f_auto/v1763000000/prendas/jean.jpg",
            url.conTamanioCloudinary(400),
        )
    }

    @Test
    fun elAnchoSeUsaTambienComoAlto() {
        assertEquals(
            "$base/w_900,h_900,c_fill,q_auto,f_auto/v1/a.png",
            "$base/v1/a.png".conTamanioCloudinary(900),
        )
    }

    @Test
    fun sinRecortarPideLaFotoEnteraConSusProporciones() {
        assertEquals(
            "$base/w_900,c_limit,q_auto,f_auto/v1763000000/jean.jpg",
            "$base/v1763000000/jean.jpg".conTamanioCloudinary(900, recortar = false),
        )
    }

    @Test
    fun siYaTieneTransformacionesNoSeDuplican() {
        val conTamanio = "$base/w_400,h_400,c_fill,q_auto,f_auto/v1763000000/jean.jpg"
        val unaSola = "$base/c_scale,w_200/v1763000000/jean.jpg"

        assertEquals(conTamanio, conTamanio.conTamanioCloudinary(900))
        assertEquals(unaSola, unaSola.conTamanioCloudinary(400))
        // Aplicarla dos veces da lo mismo que una.
        val una = "$base/v1/a.jpg".conTamanioCloudinary(400)
        assertEquals(una, una.conTamanioCloudinary(400))
    }

    @Test
    fun unaUrlQueNoEsDeCloudinaryQuedaIgual() {
        listOf(
            "https://picsum.photos/seed/prenda1/400/500",
            "https://example.com/fotos/jean.jpg",
            "file:///C:/fotos/jean.jpg",
        ).forEach { assertEquals(it, it.conTamanioCloudinary(400), it) }
    }

    @Test
    fun laCadenaVaciaOEnBlancoQuedaIgual() {
        assertEquals("", "".conTamanioCloudinary(400))
        assertEquals("  ", "  ".conTamanioCloudinary(400))
    }

    @Test
    fun conUnAnchoInvalidoNoSeToca() {
        val url = "$base/v1/a.jpg"

        assertEquals(url, url.conTamanioCloudinary(0))
        assertEquals(url, url.conTamanioCloudinary(-10))
    }

    @Test
    fun unaUrlSinVersionTambienSeTransforma() {
        assertEquals(
            "$base/w_400,h_400,c_fill,q_auto,f_auto/jean.jpg",
            "$base/jean.jpg".conTamanioCloudinary(400),
        )
    }

    @Test
    fun soloCambiaLaPartedeUploadYNoLaUrlGuardada() {
        val guardada = "$base/v1763000000/prendas/jean.jpg"

        guardada.conTamanioCloudinary(400)

        assertEquals("$base/v1763000000/prendas/jean.jpg", guardada) // los String son inmutables: la original no se modifica
    }
}
