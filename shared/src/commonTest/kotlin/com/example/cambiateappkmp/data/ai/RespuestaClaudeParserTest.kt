package com.example.cambiateappkmp.data.ai

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class RespuestaClaudeParserTest {

    private val ahora = Instant.fromEpochSeconds(1_763_000_000)
    private val categorias = mapOf(
        "p1" to Categoria.SUPERIOR,
        "p2" to Categoria.INFERIOR,
        "p3" to Categoria.CALZADO,
        "p4" to Categoria.ABRIGO,
        "p5" to Categoria.SUPERIOR,
        "p6" to Categoria.CALZADO,
    )

    private fun parsear(texto: String) = RespuestaClaudeParser.parsear(texto, Ocasion.CASUAL, categorias, ahora)

    @Test
    fun leeLosOutfitsCompletos() {
        val outfits = parsear("""{"outfits":[{"prendasIds":["p1","p2","p3"]},{"prendasIds":["p5","p2","p6","p4"]}]}""")

        assertEquals(listOf(listOf("p1", "p2", "p3"), listOf("p5", "p2", "p6", "p4")), outfits.map { it.prendasIds })
        assertTrue(outfits.all { it.id.isEmpty() && !it.favorito && it.ocasion == Ocasion.CASUAL && it.fecha == ahora })
    }

    @Test
    fun tomaLoQueEstaEntreLaPrimeraLlaveYLaUltima() {
        val outfits = parsear("Acá van:\n```json\n{\"outfits\":[{\"prendasIds\":[\"p1\",\"p2\",\"p3\"]}]}\n```\nEspero que sirva")

        assertEquals(1, outfits.size)
    }

    @Test
    fun descartaIdsInventados() {
        val outfits = parsear("""{"outfits":[{"prendasIds":["p1","p2","p3","p99","zapato"]}]}""")

        assertEquals(listOf(listOf("p1", "p2", "p3")), outfits.map { it.prendasIds })
    }

    @Test
    fun descartaLosQueNoTienenSuperiorInferiorYCalzado() {
        val outfits = parsear(
            """{"outfits":[{"prendasIds":["p1","p2"]},{"prendasIds":["p2","p3"]},{"prendasIds":["p1","p3"]},{"prendasIds":["p1","p2","p3"]}]}""",
        )

        assertEquals(listOf(listOf("p1", "p2", "p3")), outfits.map { it.prendasIds })
    }

    @Test
    fun descartaRepetidosAunqueVenganEnOtroOrdenOConIdsRepetidos() {
        val outfits = parsear(
            """{"outfits":[{"prendasIds":["p1","p2","p3"]},{"prendasIds":["p3","p1","p2"]},{"prendasIds":["p1","p1","p2","p3"]}]}""",
        )

        assertEquals(1, outfits.size)
    }

    @Test
    fun seQuedaConDiezComoMaximo() {
        val ids = categorias + (1..15).associate { "p3$it" to Categoria.ABRIGO }
        val texto = "{\"outfits\":[" +
            (1..15).joinToString(",") { """{"prendasIds":["p1","p2","p3","p3$it"]}""" } + "]}"

        val outfits = RespuestaClaudeParser.parsear(texto, Ocasion.CASUAL, ids, ahora)

        assertEquals(10, outfits.size)
    }

    @Test
    fun conTextoQueNoEsJsonODeOtraFormaDevuelveVacio() {
        assertEquals(emptyList(), parsear(""))
        assertEquals(emptyList(), parsear("no pude armar outfits"))
        assertEquals(emptyList(), parsear("{no es json}"))
        assertEquals(emptyList(), parsear("""{"otra":[]}"""))
        assertEquals(emptyList(), parsear("""{"outfits":"nada"}"""))
        assertEquals(emptyList(), parsear("""{"outfits":[{"sinIds":true},5,null]}"""))
    }
}
