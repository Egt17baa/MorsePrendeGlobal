# Créditos y licencias de recursos

Recursos de terceros incluidos en la app y su licencia. Dentro de la app se pueden leer en
Ajustes → Acerca de → «Licencias de código abierto» (textos en `app/src/main/assets/licenses/`).

## Tipografía

- **Atkinson Hyperlegible** (Regular y Bold), © 2020 Braille Institute of America, Inc.
  Licencia: SIL Open Font License 1.1. Ficheros: `app/src/main/res/font/atkinson_regular.ttf` y
  `atkinson_bold.ttf`; licencia en `app/src/main/assets/licenses/atkinson-hyperlegible-OFL.txt`.
  Origen: repositorio de Google Fonts (`ofl/atkinsonhyperlegible`).

## Bibliotecas (Apache 2.0)

- Kotlin (biblioteca estándar), kotlinx.coroutines y kotlinx.serialization, de JetBrains.
- AndroidX Activity, Lifecycle, Navigation, Room y DataStore, y Jetpack Compose (UI, Foundation,
  Material 3 e iconos de Material), de Google / Android Open Source Project.
- Dependencias de las anteriores: Okio (Square), kotlinx atomicfu (JetBrains), Guava
  listenablefuture (Google), JSpecify y anotaciones de JetBrains.

Solo para pruebas (no van en el APK): JUnit (EPL 1.0), Robolectric (MIT), AndroidX Test (Apache 2.0).

## Sonido e imágenes

El sonido se genera por síntesis y los gráficos (faro, iconos, icono de la app) son vectores propios
dibujados para este proyecto.
