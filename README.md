# CambiateApp KMP

Versión multiplataforma de **CambiateApp**, un armario digital: cada usuario inicia sesión, carga sus prendas y recibe sugerencias de outfits. Trabajo Integrador del Módulo 3 de Aplicaciones Móviles (UNAJ), migración de la app nativa del Módulo 2 a **Kotlin Multiplatform + Compose Multiplatform**. Por ahora corre en **Android**.

Integrantes: Lourdes y Tomás.

## Estado del proyecto

Semana 1: Firebase y autenticación.

- [x] Registro e inicio de sesión con Firebase Authentication (email y contraseña)
- [x] Sesión persistente: la app arranca en el Login o en la pantalla principal según haya sesión
- [x] Cerrar sesión
- [x] Tema claro y oscuro
- [ ] Listado, detalle y alta/edición de prendas (Firestore)
- [ ] Cámara, ubicación, mapa y notificaciones
- [ ] Sugerencias de outfits con IA

## Tecnologías

| Para qué | Con qué |
|---|---|
| Lenguaje y UI | Kotlin Multiplatform, Compose Multiplatform y Material 3 |
| Inyección de dependencias | Koin |
| Autenticación y base de datos | Firebase (Authentication y Firestore) con GitLive firebase-kotlin-sdk |
| Asincronía | Corrutinas y Flow |
| Tests | kotlin.test y kotlinx-coroutines-test |

## Arquitectura

Clean Architecture, en un solo módulo compartido (`shared`) con tres capas. Las dependencias apuntan siempre hacia el dominio.

```
presentation  ->  domain  <-  data
(pantallas y       (modelos,      (Firebase y
 ViewModels)        casos de uso,   otras fuentes)
                    interfaces)
```

- **domain**: Kotlin puro. Modelos, errores (`DomainException`), interfaces de repositorio y casos de uso. No importa Android, Google ni GitLive.
- **data**: implementa las interfaces del dominio (`AuthRepositoryImpl` con Firebase, `FakeAuthRepository` en memoria) y traduce los errores de Firebase al dominio.
- **presentation**: pantallas Compose sin estado (`Screen`), `Route` que las conecta con su ViewModel, ViewModels con `UiState` y `UiEvent`, y la navegación.
- **di**: módulos de Koin que conectan cada interfaz con su implementación.

Para verificar que el dominio es puro (no tiene que imprimir nada):

```
git grep -nE "import (android|androidx|com\.google|dev\.gitlive|org\.koin)" -- shared/src/commonMain/kotlin/com/example/cambiateappkmp/domain
```

### Estructura de carpetas

```
androidApp/   aplicación Android (Application, MainActivity, google-services.json)
desktopApp/   punto de entrada de escritorio generado por el asistente de KMP
shared/src/commonMain/kotlin/com/example/cambiateappkmp/
    domain/         model, error, repository, usecase
    data/           repository, mapper, fake
    presentation/   auth, components, home, navigation, session
    di/             módulos de Koin
    ui/theme/       colores, tipografía y formas (claro y oscuro)
firestore.rules     reglas de seguridad de Firestore
```

## Cómo correrlo

Requisitos: Android Studio (última versión estable con el plugin Kotlin Multiplatform), Git y un emulador o teléfono Android.

1. Clonar el repositorio **fuera de carpetas sincronizadas como OneDrive** (rompen las compilaciones de Gradle):
   ```
   git clone https://github.com/TomiNavasd/cambiateapp-kmp.git
   ```
2. Conseguir el archivo `google-services.json` y copiarlo en `androidApp/`. No está en el repositorio porque contiene claves del proyecto. Se descarga desde la consola de Firebase (proyecto `cambiateapp`, app `com.example.cambiateappkmp`) o se lo pide a otro integrante.
3. Abrir la carpeta en Android Studio y esperar el **Gradle sync**.
4. Ejecutar la configuración **androidApp** en el emulador.

### Modo sin Firebase (fake)

En `androidApp/.../CambiateApplication.kt` está la constante `USAR_AUTH_FAKE`. Con `true` la app usa un repositorio de autenticación en memoria, con la cuenta `demo@cambiateapp.com` / `123456`, sin conectarse a Firebase. Con `false` usa Firebase real. Debe quedar en `false` al hacer commit.

## Tests

```
.\gradlew.bat :shared:jvmTest :shared:testAndroidHostTest
```

Cubren los casos de uso de autenticación, la traducción de errores de Firebase y el módulo de Koin, usando repositorios falsos (no necesitan emulador ni internet).

## Seguridad

Las reglas de Firestore están en [`firestore.rules`](firestore.rules): cada usuario solo puede leer y modificar los documentos cuyo `userId` coincide con el suyo, y todo lo demás está cerrado. No se publica ningún secreto en el repositorio: `google-services.json` y `local.properties` están en `.gitignore`.

