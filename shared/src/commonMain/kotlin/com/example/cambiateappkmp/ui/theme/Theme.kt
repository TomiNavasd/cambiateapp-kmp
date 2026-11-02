package com.example.cambiateappkmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = Teal700,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    secondary = Teal500,
    onSecondary = SobreTeal,
    secondaryContainer = Teal100,
    onSecondaryContainer = Teal900,
    background = FondoClaro,
    onBackground = TextoClaro,
    surface = SuperficieClara,
    onSurface = TextoClaro,
    surfaceVariant = SuperficieVarianteClara,
    onSurfaceVariant = TextoSecundarioClaro,
    outline = BordeClaro,
    error = ErrorClaro,
    onError = Color.White,
    errorContainer = ErrorContenedorClaro,
    onErrorContainer = SobreErrorContenedorClaro,
)

private val EsquemaOscuro = darkColorScheme(
    primary = Teal300,
    onPrimary = SobreTeal,
    primaryContainer = Teal800,
    onPrimaryContainer = Teal50,
    secondary = Teal500,
    onSecondary = SobreTeal,
    secondaryContainer = TealOscuro,
    onSecondaryContainer = Teal50,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = SuperficieOscura,
    onSurface = TextoOscuro,
    surfaceVariant = SuperficieVarianteOscura,
    onSurfaceVariant = TextoSecundarioOscuro,
    outline = BordeOscuro,
    error = ErrorOscuro,
    onError = SobreTeal,
    errorContainer = ErrorContenedorOscuro,
    onErrorContainer = SobreErrorContenedorOscuro,
)

@Composable
fun CambiateAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = CambiateTypography,
        shapes = CambiateShapes,
        content = content,
    )
}