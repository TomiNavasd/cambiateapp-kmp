package com.example.cambiateappkmp.presentation.components

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColumnasResponsivasTest {

    // Con densidad 1 un dp es un píxel: los números de abajo son dp.
    private val densidad = Density(1f)
    private val columnas = ColumnasResponsivas(minSize = 170.dp)

    private fun celdas(ancho: Int, separacion: Int = 12): List<Int> =
        with(columnas) { with(densidad) { calculateCrossAxisCellSizes(ancho, separacion) } }

    @Test
    fun enUnCelularSiguenSiendoDosColumnas() {
        // 360 dp de pantalla menos 16 + 16 de margen: 328 dp de grilla.
        listOf(280, 328, 360, 411, 500, 599).forEach { ancho ->
            assertEquals(2, celdas(ancho).size, "con $ancho dp")
        }
    }

    @Test
    fun enUnCelularLasDosCeldasRepartenTodoElAncho() {
        val c = celdas(328)

        assertEquals(328, c.sum() + 12)
        assertTrue(c.all { it in 158..158 })
    }

    @Test
    fun enPantallasAnchasEntranTantasCeldasComoPermiteElMinimo() {
        assertEquals(3, celdas(700).size) // (700 + 12) / (170 + 12) = 3
        assertEquals(5, celdas(1068).size) // ventana de 1100 con márgenes de 16
        assertEquals(6, celdas(1100).size) // (1100 + 12) / 182 = 6
    }

    @Test
    fun ningunaCeldaEnPantallaAnchaEsMasChicaQueElMinimo() {
        listOf(600, 700, 820, 1068, 1100).forEach { ancho ->
            assertTrue(celdas(ancho).all { it >= 170 }, "con $ancho dp: ${celdas(ancho)}")
        }
    }

    @Test
    fun elAnchoNoSePierdeNiSobra() {
        listOf(328, 601, 700, 1068, 1101).forEach { ancho ->
            val c = celdas(ancho)
            assertEquals(ancho, c.sum() + 12 * (c.size - 1), "con $ancho dp")
        }
    }

    @Test
    fun elUmbralEsExactamenteElDeLaPantallaAncha() {
        assertEquals(2, celdas(599).size)
        assertEquals(3, celdas(600).size) // (600 + 12) / 182 = 3
    }

    @Test
    fun repartirDaElSobranteALasPrimerasCeldas() {
        assertEquals(listOf(4, 3, 3), repartirColumnas(ancho = 10, separacion = 0, cantidad = 3))
        assertEquals(listOf(5, 5), repartirColumnas(ancho = 12, separacion = 2, cantidad = 2))
    }

    @Test
    fun laCantidadDeColumnasNuncaBajaDeUna() {
        assertEquals(1, cantidadDeColumnas(ancho = 100, separacion = 12, minimo = 500, columnasEnCelular = 2, umbral = 50))
    }
}
