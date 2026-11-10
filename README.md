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

## Fotos de prendas (cámara, galería y Cloudinary)

### Flujo completo

1. `SelectorFoto` muestra la foto actual y los botones **Galería** y **Cámara**.
2. `SelectorImagen` (expect/actual) abre la galería o la cámara y devuelve la dirección (uri) de la imagen elegida.
3. `SelectorFoto` llama a `ImageStorageRepository.subirImagen(uri)`.
4. `CloudinaryImageStorageRepository` le pide los bytes a `LectorDeImagen` (expect/actual por plataforma) y hace un POST multipart a Cloudinary con Ktor, usando un *upload preset* sin firma.
5. Cloudinary responde con `secure_url`. El repositorio la devuelve y `SelectorFoto` se la entrega a quien lo usa mediante `onFotoSubida`.
6. La URL se guarda en el campo `fotoUrl` de la prenda, en Firestore.
7. `FotoPrenda` muestra la foto con Coil. Si no hay URL muestra "Sin foto", y si falla la carga muestra un mensaje.

### Qué va con expect/actual

| Declaración (`commonMain`) | Android | Escritorio (JVM) |
|---|---|---|
| `rememberSelectorImagen` (`SelectorImagen`) | Galería con `PickVisualMedia` y cámara con `TakePicture`, que guarda la foto en la caché de la app mediante `FileProvider` | Diálogo de archivos de Java (`FileDialog`). No hay cámara: `camaraDisponible` es `false` y el botón **Cámara** no se muestra |
| `lectorImagenModule` (registra el `LectorDeImagen`) | Lee la uri con `ContentResolver`, fuera del hilo principal | Lee el archivo a partir de la uri (`File(URI)`) |
| `ManejarAtras` | `BackHandler` | No hace nada |

### Configuración

- `CloudinaryConfig` tiene el *cloud name* y el *upload preset*, que son datos públicos (los mismos que usa la app nativa). El preset es *unsigned*, por eso no se usa ninguna API Key ni API Secret y no hay secretos en el repositorio.
- En Android la cámara usa un `FileProvider` (`${applicationId}.fileprovider`, con `res/xml/file_paths.xml`). Así la foto se guarda en la caché de la app y no hace falta pedir el permiso `CAMERA`.
- En escritorio no hay cámara: solo se puede elegir un archivo.

### Modo fake vs. real

`USAR_AUTH_FAKE` (en `CambiateApplication.kt`) cambia **juntos** la autenticación, las prendas y la subida de fotos. Con `true` todo funciona en memoria: la subida de fotos devuelve siempre una URL de ejemplo y no se conecta a Cloudinary. Con `false` usa Firebase y Cloudinary reales. Debe estar en `false` al hacer commit.

### Reglas de Firestore (pendiente de publicar)

Las reglas están en [`firestore.rules`](firestore.rules). Antes de publicarlas hay que probarlas en el **Rules Playground** de la consola de Firebase (Firestore, pestaña *Reglas*). Para la lectura, la edición y el borrado se necesita una prenda de prueba que ya exista en la base. Casos a probar en la colección `prendas`:

| Caso | Operación | Usuario | Resultado esperado |
|---|---|---|---|
| Leer una prenda propia | `get` | autenticado, `userId` = su uid | Permitido |
| Leer una prenda de otro usuario | `get` | autenticado, `userId` distinto del suyo | Denegado |
| Leer sin autenticar | `get` | sin autenticar | Denegado |
| Crear con mi `userId` | `create` | autenticado, `userId` = su uid | Permitido |
| Crear con el `userId` de otro | `create` | autenticado, `userId` distinto del suyo | Denegado |
| Editar una prenda propia | `update` | autenticado, `userId` = su uid | Permitido |
| Borrar la prenda de otro usuario | `delete` | autenticado, `userId` distinto del suyo | Denegado |

Publicar las reglas **también afecta a la app nativa**, porque comparte la misma base de datos con esta versión. Por eso se publican recién después de probarlas.

### Cómo correr los tests de fotos

```
.\gradlew.bat :shared:jvmTest
```

Incluye los tests del repositorio de Cloudinary (con el `MockEngine` de Ktor, sin internet) y del almacenamiento falso.

