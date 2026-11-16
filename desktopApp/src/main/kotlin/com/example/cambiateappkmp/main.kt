package com.example.cambiateappkmp

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.cambiateappkmp.data.config.ConfigFirebaseEscritorio
import com.example.cambiateappkmp.data.config.DesktopApiKeys
import com.example.cambiateappkmp.di.initKoin
import com.example.cambiateappkmp.domain.config.ApiKeys
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

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "CambiateApp",
            state = rememberWindowState(size = DpSize(480.dp, 900.dp)),
        ) {
            App()
        }
    }
}
