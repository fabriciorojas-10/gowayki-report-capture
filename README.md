# GoWayki Report Capture

Prototipo Android desechable para explorar el flujo de captura de reportes ciudadanos de GoWayki Pulse.

## Alcance

Prototipo de aprendizaje, sin backend, persistencia ni integración con producción.

## Funcionalidades

- Selección de categoría y descripción.
- Captura de vista previa fotográfica o selección desde galería.
- Fotografía obligatoria para determinadas categorías.
- Solicitud de ubicación mediante APIs nativas de Android.
- Edición manual de coordenadas.
- Mapa ilustrativo y confirmación local.

## Tecnologías

- Kotlin y Jetpack Compose.
- Material 3.
- Android Gradle Plugin y Gradle Wrapper.

Consulta los archivos de configuración para conocer las versiones utilizadas.

## Requisitos

- Android SDK compatible con `compileSdk 37`.
- JDK compatible con Gradle.
- Emulador o dispositivo Android con API 26 o superior.

## Compilación

En Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug --console=plain
```

El APK de depuración se genera en:

`app/build/outputs/apk/debug/app-debug.apk`

Para instalarlo en un emulador conectado:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Limitaciones

- No existe conexión con un backend ni una API de producción.
- Los reportes no se guardan ni se envían.
- El mapa es ilustrativo y no usa cartografía real.
- La compilación exitosa no sustituye las pruebas funcionales.

## Rama de trabajo

`prototype/report-capture`

El prototipo se mantiene separado de `main`. No realizar merge a `main` como parte de esta tarea.

## Referencia

Tarea: `GOW-PULSE-26-023`.
