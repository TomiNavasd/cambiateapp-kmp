package com.example.cambiateappkmp.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CamposEditablesPrendaTest {

    @Test
    fun losCamposEditablesSonSoloLosQueEditaElFormularioMasLasEtiquetas() {
        assertEquals(
            listOf("nombre", "descripcion", "categoria", "colores", "ocasiones", "fotoUrl", "etiquetas"),
            CAMPOS_EDITABLES_PRENDA,
        )
    }

    @Test
    fun nuncaIncluyenElDuenoNiLoQueGuardaSoloLaNativa() {
        listOf("userId", "temporada", "vecesUsada", "ultimaVezUsada", "lugarCompra", "lat", "lng").forEach {
            assertFalse(it in CAMPOS_EDITABLES_PRENDA, it)
        }
    }

    @Test
    fun todosLosCamposEditablesExistenEnElDto() {
        // Si se agrega un campo editable que el DTO no tiene, la edición escribiría algo que nunca se lee.
        val camposDelDto = com.example.cambiateappkmp.data.remote.PrendaDto.serializer().descriptor
            .let { d -> (0 until d.elementsCount).map { d.getElementName(it) } }

        CAMPOS_EDITABLES_PRENDA.forEach { assertEquals(true, it in camposDelDto, it) }
    }
}
