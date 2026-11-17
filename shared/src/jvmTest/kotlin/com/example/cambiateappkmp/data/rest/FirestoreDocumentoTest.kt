package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.mapper.CAMPOS_EDITABLES_PRENDA
import com.example.cambiateappkmp.data.mapper.aDomain
import com.example.cambiateappkmp.data.mapper.aDto
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class FirestoreDocumentoTest {

    private val prenda = Prenda(
        id = "abc123",
        nombre = "Campera impermeable",
        descripcion = "Para los días de lluvia",
        categoria = Categoria.ABRIGO,
        colores = listOf("azul", "negro"),
        ocasiones = listOf(Ocasion.CASUAL, Ocasion.TRABAJO),
        fotoUrl = "https://res.cloudinary.com/demo/campera.jpg",
    )

    @Test
    fun cadaTipoDeValorSobreviveAlIdaYVuelta() {
        val valores = listOf(
            ValorFirestore.Texto("hola"),
            ValorFirestore.Texto(""),
            ValorFirestore.Entero(9_007_199_254_740_993L),
            ValorFirestore.Decimal(-34.6037),
            ValorFirestore.Booleano(true),
            ValorFirestore.Booleano(false),
            ValorFirestore.Fecha("2026-11-16T15:45:00Z"),
            ValorFirestore.Nulo,
            ValorFirestore.Lista(listOf(ValorFirestore.Texto("a"), ValorFirestore.Entero(2))),
            ValorFirestore.Lista(emptyList()),
            ValorFirestore.Mapa(
                mapOf(
                    "anidado" to ValorFirestore.Mapa(mapOf("x" to ValorFirestore.Decimal(1.5))),
                    "lista" to ValorFirestore.Lista(listOf(ValorFirestore.Booleano(true))),
                ),
            ),
        )

        valores.forEach { original ->
            assertEquals(original, original.aJson().aValorFirestore(), "con $original")
        }
    }

    @Test
    fun cadaTipoUsaElNombreDeCampoDeLaApiRest() {
        assertEquals("stringValue", ValorFirestore.Texto("x").aJson().keys.single())
        assertEquals("integerValue", ValorFirestore.Entero(1).aJson().keys.single())
        assertEquals("doubleValue", ValorFirestore.Decimal(1.0).aJson().keys.single())
        assertEquals("booleanValue", ValorFirestore.Booleano(true).aJson().keys.single())
        assertEquals("timestampValue", ValorFirestore.Fecha("2026-01-01T00:00:00Z").aJson().keys.single())
        assertEquals("arrayValue", ValorFirestore.Lista(emptyList()).aJson().keys.single())
        assertEquals("mapValue", ValorFirestore.Mapa(emptyMap()).aJson().keys.single())
        assertEquals("nullValue", ValorFirestore.Nulo.aJson().keys.single())
    }

    @Test
    fun losEnterosSeEnvianComoTextoYLaListaVaciaSinValues() {
        assertEquals(JsonPrimitive("42"), ValorFirestore.Entero(42).aJson()["integerValue"])
        assertEquals(JsonObject(emptyMap()), ValorFirestore.Lista(emptyList()).aJson()["arrayValue"])
    }

    @Test
    fun unTipoQueLaAppNoUsaQuedaComoNulo() {
        val geopunto = """{"geoPointValue":{"latitude":1.0,"longitude":2.0}}"""

        assertEquals(ValorFirestore.Nulo, jsonFirebase.parseToJsonElement(geopunto).aValorFirestore())
    }

    @Test
    fun laPrendaIdaYVueltaPorUnDocumentoQuedaIgual() {
        val campos = prenda.aDto("uid1").aCamposFirestore()
        val cuerpo = DocumentoFirestore(null, campos).aJsonCuerpo()
        // Firestore le agrega la ruta al devolver el documento.
        val respuesta = JsonObject(
            cuerpo + ("name" to JsonPrimitive("projects/p/databases/(default)/documents/prendas/abc123")),
        )

        val documento = DocumentoFirestore.desdeJson(respuesta)
        val vuelta = documento.aPrendaDto().aDomain(documento.id)

        assertEquals("abc123", documento.id)
        assertEquals(prenda, vuelta)
    }

    @Test
    fun losCamposTienenLosMismosNombresYTiposQueUsaAndroid() {
        val campos = prenda.aDto("uid1").aCamposFirestore()

        assertEquals(
            setOf("userId", "nombre", "descripcion", "categoria", "colores", "ocasiones", "fotoUrl", "etiquetas"),
            campos.keys,
        )
        assertEquals(ValorFirestore.Texto("uid1"), campos["userId"])
        // La categoría y las ocasiones van en minúsculas, como las guarda Android.
        assertEquals(ValorFirestore.Texto("abrigo"), campos["categoria"])
        assertEquals(
            ValorFirestore.Lista(listOf(ValorFirestore.Texto("casual"), ValorFirestore.Texto("trabajo"))),
            campos["ocasiones"],
        )
        assertEquals(ValorFirestore.Lista(listOf(ValorFirestore.Texto("azul"), ValorFirestore.Texto("negro"))), campos["colores"])
    }

    @Test
    fun losCamposEditablesNuncaIncluyenElUserId() {
        val editables = prenda.aDto("uid1").aCamposEditablesFirestore()

        assertEquals(CAMPOS_EDITABLES_PRENDA.toSet(), editables.keys)
        assertEquals(false, "userId" in editables)
    }

    @Test
    fun unDocumentoDeLaAppNativaConCamposExtraSeLeeIgnorandoLoQueNoUsamos() {
        val documentoNativo = """
            {"name":"projects/p/databases/(default)/documents/prendas/nat1","fields":{
              "userId":{"stringValue":"uid1"},"nombre":{"stringValue":"Jean"},"descripcion":{"stringValue":""},
              "categoria":{"stringValue":"inferior"},
              "colores":{"arrayValue":{"values":[{"stringValue":"azul"}]}},
              "ocasiones":{"arrayValue":{"values":[{"stringValue":"casual"},{"stringValue":"inexistente"}]}},
              "etiquetas":{"arrayValue":{"values":[{"stringValue":"verano"}]}},
              "temporada":{"stringValue":"verano"},"fotoUrl":{"stringValue":"https://x/y.jpg"},
              "vecesUsada":{"integerValue":"3"},"ultimaVezUsada":{"timestampValue":"2026-10-01T12:00:00Z"},
              "lugarCompra":{"stringValue":"Feria"},"lat":{"doubleValue":-34.6},"lng":{"nullValue":null}
            }}
        """.trimIndent()

        val documento = DocumentoFirestore.desdeJson(jsonFirebase.parseToJsonElement(documentoNativo))
        val prendaLeida = documento.aPrendaDto().aDomain(documento.id)

        assertEquals("nat1", documento.id)
        assertEquals(ValorFirestore.Entero(3), documento.campos["vecesUsada"])
        assertEquals(ValorFirestore.Fecha("2026-10-01T12:00:00Z"), documento.campos["ultimaVezUsada"])
        assertEquals(ValorFirestore.Decimal(-34.6), documento.campos["lat"])
        assertEquals(ValorFirestore.Nulo, documento.campos["lng"])
        assertEquals(Categoria.INFERIOR, prendaLeida?.categoria)
        // La ocasión desconocida se descarta, igual que en Android.
        assertEquals(listOf(Ocasion.CASUAL), prendaLeida?.ocasiones)
    }

    @Test
    fun siLaCategoriaEsDesconocidaLaPrendaNoSeArma() {
        val documento = DocumentoFirestore(
            "projects/p/databases/(default)/documents/prendas/x",
            mapOf("userId" to ValorFirestore.Texto("uid1"), "categoria" to ValorFirestore.Texto("sombrero")),
        )

        assertNull(documento.aPrendaDto().aDomain(documento.id))
    }

    @Test
    fun unDocumentoSinCamposNiNombreSeLeeSinFallar() {
        val documento = DocumentoFirestore.desdeJson(jsonFirebase.parseToJsonElement("{}"))

        assertEquals("", documento.id)
        assertEquals(emptyMap(), documento.campos)
        assertEquals("", documento.aPrendaDto().nombre)
    }

    @Test
    fun elCuerpoDeEscrituraEsUnObjetoConFields() {
        val cuerpo = DocumentoFirestore(null, mapOf("nombre" to ValorFirestore.Texto("Remera"))).aJsonCuerpo()

        assertEquals(setOf("fields"), cuerpo.keys)
        assertEquals(JsonPrimitive("Remera"), cuerpo["fields"]!!.jsonObject["nombre"]!!.jsonObject["stringValue"])
    }
}
