package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.remote.PrendaDto
import kotlin.test.Test
import kotlin.test.assertEquals

class FirestorePrendaMapperTest {

    private val dto = PrendaDto(
        userId = "uid1",
        nombre = "Jean",
        descripcion = "Recto",
        categoria = "inferior",
        colores = listOf("azul"),
        ocasiones = listOf("casual"),
        fotoUrl = "https://x/y.jpg",
        etiquetas = listOf("casual", "jean"),
    )

    private fun idaYVuelta(original: PrendaDto): PrendaDto {
        // Lo que se manda a Firestore, convertido a JSON y leído de nuevo como si lo devolviera la API.
        val json = DocumentoFirestore("projects/p/databases/(default)/documents/prendas/p1", original.aCamposFirestore())
            .aJsonCuerpo()
        return DocumentoFirestore.desdeJson(json).aPrendaDto()
    }

    @Test
    fun idaYVueltaConEtiquetas() {
        assertEquals(dto, idaYVuelta(dto))
    }

    @Test
    fun idaYVueltaSinEtiquetas() {
        val sin = dto.copy(etiquetas = emptyList())

        assertEquals(sin, idaYVuelta(sin))
    }

    @Test
    fun unDocumentoSinElCampoEtiquetasSeLeeComoListaVacia() {
        val documento = DocumentoFirestore(
            nombre = null,
            campos = mapOf("nombre" to ValorFirestore.Texto("Remera"), "categoria" to ValorFirestore.Texto("superior")),
        )

        assertEquals(emptyList(), documento.aPrendaDto().etiquetas)
    }
}
