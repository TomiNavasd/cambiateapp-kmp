package com.example.cambiateappkmp.presentation.mapa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cambiateappkmp.domain.model.LocalRopa
import com.example.cambiateappkmp.domain.model.Ubicacion

/** Mapa con la ubicación del usuario y un marcador por cada local. Cada plataforma lo dibuja a su manera. */
@Composable
expect fun MapaView(
    ubicacion: Ubicacion?,
    locales: List<LocalRopa>,
    modifier: Modifier = Modifier,
)
