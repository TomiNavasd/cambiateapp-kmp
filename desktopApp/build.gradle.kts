import java.io.StringReader
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Las claves salen de local.properties (no se sube al repositorio) o de variables de entorno.
// Si faltan, quedan vacías y el build no falla.
val localProperties = Properties().apply {
    val texto = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText
    if (texto.isPresent) StringReader(texto.get()).use { load(it) }
}

fun leerClave(nombre: String): String =
    (localProperties.getProperty(nombre) ?: providers.environmentVariable(nombre).orNull ?: "").trim()

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    // Koin: hace falta para llamar a initKoin() desde el main de escritorio
    implementation(libs.koin.core)
}

compose.desktop {
    application {
        mainClass = "com.example.cambiateappkmp.MainKt"

        // Para arrancar con datos de ejemplo, sin Firebase: .\gradlew.bat :desktopApp:run -Pcambiate.fake=true
        providers.gradleProperty("cambiate.fake").orNull?.let { jvmArgs += "-Dcambiate.fake=$it" }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "CambiateApp"
            packageVersion = "1.0.0"
            description = "CambiateApp: armario digital con sugerencias de outfits"

            // Íconos: .ico para Windows y .png para Linux (los dos salen de la misma imagen). Para macOS haría falta un .icns.
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "CambiateApp"
                shortcut = true
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}

// Las claves llegan a la JVM como variables de entorno (y no como -D) para que no queden a la vista
// en la línea de comandos del proceso. La app las lee como propiedad de sistema o, si no, como variable de entorno.
tasks.withType<JavaExec>().configureEach {
    environment("FIREBASE_API_KEY", leerClave("FIREBASE_API_KEY"))
    environment("FIREBASE_PROJECT_ID", leerClave("FIREBASE_PROJECT_ID"))
    environment("ANTHROPIC_API_KEY", leerClave("ANTHROPIC_API_KEY"))
}
