package com.example.cambiateappkmp.presentation.altaedicion

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion

data class AltaEdicionUiState(
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val noEncontrada: Boolean = false,
    val errorCarga: String? = null,

    val nombre: String = "",
    val descripcion: String = "",
    val categoria: Categoria = Categoria.SUPERIOR,
    val colores: String = "",
    val ocasiones: Set<Ocasion> = emptySet(),
    val fotoUrl: String = "",
    /** Etiquetas de la prenda: no se muestran ni se editan, se conservan para guardarlas sin cambios. */
    val etiquetas: List<String> = emptyList(),

    /** La IA está analizando la foto (botón "Escanear prenda"). */
    val analizando: Boolean = false,
    /** Resultado del escaneo para mostrar bajo el botón: lo que se completó o por qué falló. */
    val avisoEscaneo: String? = null,
    val avisoEscaneoEsError: Boolean = false,

    val guardando: Boolean = false,
    val errorNombre: String? = null,
    val errorFoto: String? = null,
    val errorGeneral: String? = null,
)