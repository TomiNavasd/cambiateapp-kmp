package com.example.cambiateappkmp.presentation.mapa

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cambiateappkmp.domain.model.LocalRopa
import com.example.cambiateappkmp.domain.model.Ubicacion
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * De dónde salen las imágenes del mapa (igual que en la app nativa). Si los servidores de OpenStreetMap
 * bloquean los pedidos (se ven carteles "Access blocked"), cambiar OpenTopo por Mapnik.
 */
private val FuenteDeMapa = TileSourceFactory.OpenTopo

@Composable
actual fun MapaView(
    ubicacion: Ubicacion?,
    locales: List<LocalRopa>,
    modifier: Modifier,
) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current

    val mapView = remember {
        configurarOsmdroid(contexto)
        MapView(contexto).apply {
            setTileSource(FuenteDeMapa)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    // Para centrar el mapa solo cuando cambia la ubicación, y no pisar lo que el usuario desplazó.
    val ultimaCentrada = remember { arrayOfNulls<Ubicacion>(1) }

    // El mapa tiene que enterarse de cuándo la pantalla se pausa o se reanuda.
    DisposableEffect(ciclo, mapView) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        ciclo.lifecycle.addObserver(observador)
        onDispose {
            ciclo.lifecycle.removeObserver(observador)
            mapView.onDetach()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { vista ->
                vista.overlays.clear()
                locales.forEach { local ->
                    vista.overlays.add(
                        Marker(vista).apply {
                            position = GeoPoint(local.lat, local.lng)
                            title = local.nombre
                            snippet = local.direccion.ifBlank { null }
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        },
                    )
                }
                if (ubicacion != null) {
                    val punto = GeoPoint(ubicacion.lat, ubicacion.lng)
                    vista.overlays.add(
                        Marker(vista).apply {
                            position = punto
                            title = "Estás acá"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        },
                    )
                    if (ultimaCentrada[0] != ubicacion) {
                        vista.controller.setCenter(punto)
                        ultimaCentrada[0] = ubicacion
                    }
                }
                vista.invalidate()
            },
        )

        // La licencia del mapa exige mostrar esta atribución (sale de la fuente elegida).
        Text(
            text = FuenteDeMapa.copyrightNotice,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(4.dp),
        )
    }
}

/** osmdroid guarda las imágenes del mapa en la caché de la app (no pide permiso de almacenamiento). */
private fun configurarOsmdroid(contexto: Context) {
    val configuracion = Configuration.getInstance()
    configuracion.load(contexto, contexto.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    // Los servidores de mapas piden un User-Agent que identifique a la app.
    configuracion.userAgentValue = "CambiateApp/1.0 (trabajo practico UNAJ; app Android)"
    val base = File(contexto.cacheDir, "osmdroid")
    configuracion.osmdroidBasePath = base
    configuracion.osmdroidTileCache = File(base, "tiles_v3")
}
