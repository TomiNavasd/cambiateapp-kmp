package com.example.cambiateappkmp

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.cambiateappkmp.data.config.ConfigFirebaseEscritorio
import com.example.cambiateappkmp.data.config.DesktopApiKeys
import com.example.cambiateappkmp.di.initKoin
import com.example.cambiateappkmp.domain.config.ApiKeys
import com.example.cambiateappkmp.presentation.components.configurarImagenesEscritorio
import java.awt.Dimension
import org.koin.dsl.module

fun main() {
    // Por defecto usa Firebase real. Con -Dcambiate.fake=true arranca con datos de ejemplo, sin conexión.
    val usarFake = System.getProperty("cambiate.fake") == "true"
    val configFirebase = ConfigFirebaseEscritorio.desdeEntorno()

    if (!usarFake && !configFirebase.completa) {
        System.err.println(
            "Falta FIREBASE_API_KEY o FIREBASE_PROJECT_ID en local.properties: no se va a poder iniciar sesión. " +
                "Para probar sin Firebase: .\\gradlew.bat :desktopApp:run -Pcambiate.fake=true",
        )
    }

    initKoin(usarFake = usarFake) {
        modules(
            module {
                single<ApiKeys> { DesktopApiKeys() }
                single { configFirebase }
            },
        )
    }

    configurarImagenesEscritorio()

    application {
        val icono = remember { BitmapPainter(useResource("icon.png", ::loadImageBitmap)) }
        // Ventana inicial de 1100 x 760 dp, centrada en la pantalla, y no se puede achicar por debajo de 800 x 600.
        val estadoVentana = rememberWindowState(position = WindowPosition(Alignment.Center), size = DpSize(1100.dp, 760.dp))
        Window(
            onCloseRequest = ::exitApplication,
            title = "CambiateApp",
            // El ícono se ve en la barra de título y en la barra de tareas. Está en desktopApp/src/main/resources.
            icon = icono,
            state = estadoVentana,
        ) {
            LaunchedEffect(Unit) { window.minimumSize = Dimension(800, 600) }
            App()
        }
    }
}
