# MorsePrende: El Faro

Juego educativo para Android que enseña **código Morse jugando**. Eres el farero de una isla perdida:
barcos, espías y señales del espacio te hablan en Morse, y para avanzar tienes que aprender a
recibir y a enviar. Totalmente gratis, sin anuncios y sin conexión a internet.

> Estado: **Etapa 1** (núcleo Morse, traductor con sonido/vibración/linterna/pantalla y ajustes).
> La campaña, los minijuegos y la biblioteca llegan en las siguientes etapas.

- Diseño: [`docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md`](docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md)
- Plan de la etapa 1: [`docs/superpowers/plans/2026-10-31-etapa-1-nucleo-y-traductor.md`](docs/superpowers/plans/2026-10-31-etapa-1-nucleo-y-traductor.md)
- Pruebas en el móvil: [`docs/pruebas/etapa-1.md`](docs/pruebas/etapa-1.md)

## Compilar

Requisitos: JDK 17 o superior y acceso a internet para descargar el Android SDK y las dependencias.

```bash
bash scripts/setup-android-sdk.sh   # instala el Android SDK en ~/android-sdk y crea local.properties
./gradlew :morse-core:test :app:testDebugUnitTest   # pruebas
./gradlew :app:assembleDebug                         # APK en app/build/outputs/apk/debug/
```

## Estructura

- `morse-core/`: Kotlin puro, sin Android. Tabla Morse (UIT-R M.1677-1 + Ñ y !), códec texto⇄Morse,
  tiempos (estándar y Farnsworth) y síntesis de tonos.
- `app/`: app Android con Jetpack Compose. Reproducción por sonido, vibración, linterna y pantalla;
  ajustes (DataStore); pantallas Faro, Traductor y Ajustes.

## Privacidad

La app no declara el permiso de internet, no tiene cuentas, anuncios ni analíticas. Solo pide
permiso para vibrar.
