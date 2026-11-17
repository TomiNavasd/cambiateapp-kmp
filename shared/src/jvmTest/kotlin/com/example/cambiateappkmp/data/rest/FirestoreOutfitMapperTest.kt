package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirestoreOutfitMapperTest {

    private val fecha = Instant.parse("2026-11-16T15:45:00.000000250Z")
    private val outfit = Outfit("o1", Ocasion.SALIDA, listOf("a", "b", "c"), fecha, favorito = true)

    private fun documento(campos: Map<String, ValorFirestore>, id: String = "o1") =
        DocumentoFirestore("projects/p/databases/(default)/documents/outfits/$id", campos)

    private fun idaYVuelta(o: Outfit): Outfit? {
        // Lo que se manda a Firestore, convertido a JSON y leído de nuevo como si lo devolviera la API.
        val json = documento(o.aCamposFirestore("uid1"), o.id).aJsonCuerpo()
        return DocumentoFirestore.desdeJson(
            kotlinx.serialization.json.JsonObject(json + ("name" to kotlinx.serialization.json.JsonPrimitive("projects/p/databases/(default)/documents/outfits/${o.id}"))),
        ).aOutfit()
    }

    @Test
    fun losCamposTienenLosMismosNombresYTiposQueLaNativa() {
        val campos = outfit.aCamposFirestore("uid1")

        assertEquals(setOf("userId", "ocasion", "prendasIds", "fecha", "favorito"), campos.keys)
        assertEquals(ValorFirestore.Texto("uid1"), campos["userId"])
        assertEquals(ValorFirestore.Texto("salida"), campos["ocasion"]) // minúscula, como la nativa
        assertEquals(
            ValorFirestore.Lista(listOf(ValorFirestore.Texto("a"), ValorFirestore.Texto("b"), ValorFirestore.Texto("c"))),
            campos["prendasIds"],
        )
        assertEquals(ValorFirestore.Fecha("2026-11-16T15:45:00.000000250Z"), campos["fecha"])
        assertEquals(ValorFirestore.Booleano(true), campos["favorito"])
    }

    @Test
    fun enElJsonLosValoresVanTipados() {
        val campos = outfit.aCamposFirestore("uid1")
        val json = documento(campos).aJsonCuerpo()["fields"]!!.jsonObject

        assertEquals("uid1", json["userId"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
        assertEquals("salida", json["ocasion"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
        assertEquals("true", json["favorito"]!!.jsonObject["booleanValue"]!!.jsonPrimitive.content)
        assertEquals("2026-11-16T15:45:00.000000250Z", json["fecha"]!!.jsonObject["timestampValue"]!!.jsonPrimitive.content)
        val ids = json["prendasIds"]!!.jsonObject["arrayValue"]!!.jsonObject["values"]!!.jsonArray
        assertEquals(listOf("a", "b", "c"), ids.map { it.jsonObject["stringValue"]!!.jsonPrimitive.content })
    }

    @Test
    fun losCamposEditablesSonSoloOcasionPrendasYFavorito() {
        val editables = outfit.aCamposEditablesFirestore("uid1")

        assertEquals(setOf("ocasion", "prendasIds", "favorito"), editables.keys)
        assertFalse("userId" in editables)
        assertFalse("fecha" in editables)
    }

    @Test
    fun idaYVueltaConservaTodoIncluidaLaFechaConNanosegundos() {
        assertEquals(outfit, idaYVuelta(outfit))
    }

    @Test
    fun idaYVueltaConPrendasVaciasYSinFavorito() {
        val vacio = outfit.copy(prendasIds = emptyList(), favorito = false)

        assertEquals(vacio, idaYVuelta(vacio))
    }

    @Test
    fun elIdEsElUltimoTramoDeLaRutaDelDocumento() {
        val leido = documento(outfit.aCamposFirestore("uid1"), id = "abc123").aOutfit()

        assertEquals("abc123", leido?.id)
    }

    @Test
    fun laFechaSeLeeEnLosFormatosQueDevuelveFirestore() {
        fun fechaDe(texto: String) = documento(
            mapOf("ocasion" to ValorFirestore.Texto("casual"), "fecha" to ValorFirestore.Fecha(texto)),
        ).aOutfit()!!.fecha

        assertEquals(Instant.fromEpochSeconds(1_794_843_900), fechaDe("2026-11-16T15:45:00Z"))
        assertEquals(Instant.fromEpochSeconds(1_794_843_900, 500_000_000), fechaDe("2026-11-16T15:45:00.5Z"))
        assertEquals(Instant.fromEpochSeconds(1_794_843_900, 123_456_789), fechaDe("2026-11-16T15:45:00.123456789Z"))
        assertEquals(Instant.fromEpochSeconds(1_794_843_900), fechaDe("2026-11-16T12:45:00-03:00"))
    }

    @Test
    fun sinFechaOConUnaFechaIlegibleQuedaLaEpoca() {
        val sinFecha = documento(mapOf("ocasion" to ValorFirestore.Texto("casual"))).aOutfit()!!
        val ilegible = documento(
            mapOf("ocasion" to ValorFirestore.Texto("casual"), "fecha" to ValorFirestore.Fecha("ayer")),
        ).aOutfit()!!

        assertEquals(Instant.fromEpochSeconds(0), sinFecha.fecha)
        assertEquals(Instant.fromEpochSeconds(0), ilegible.fecha)
    }

    @Test
    fun unaOcasionDesconocidaDescartaElDocumentoYLaMayusculaSeAcepta() {
        assertNull(documento(mapOf("ocasion" to ValorFirestore.Texto("fiesta"))).aOutfit())
        assertNull(documento(emptyMap()).aOutfit())
        assertEquals(Ocasion.CASAMIENTO, documento(mapOf("ocasion" to ValorFirestore.Texto("CASAMIENTO"))).aOutfit()?.ocasion)
    }

    @Test
    fun unDocumentoDeLaNativaConCamposExtraSeLeeIgnorandoLoQueNoUsamos() {
        val leido = documento(
            mapOf(
                "userId" to ValorFirestore.Texto("uid1"),
                "ocasion" to ValorFirestore.Texto("trabajo"),
                "prendasIds" to ValorFirestore.Lista(listOf(ValorFirestore.Texto("x"), ValorFirestore.Nulo, ValorFirestore.Texto("y"))),
                "favorito" to ValorFirestore.Texto("no es un booleano"),
                "etiqueta" to ValorFirestore.Texto("extra"),
            ),
        ).aOutfit()!!

        assertEquals(listOf("x", "y"), leido.prendasIds)
        assertFalse(leido.favorito)
    }
}
