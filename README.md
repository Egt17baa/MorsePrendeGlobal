# MorsePrende: El Faro

Juego educativo para Android que enseña **código Morse jugando**. Eres el farero de una isla perdida:
barcos, espías y señales del espacio te hablan en Morse, y para avanzar tienes que aprender a
recibir y a enviar. Totalmente gratis, sin anuncios y sin conexión a internet.

> Estado: **Etapa 3b**: capítulo 1 «El Faro» jugable, repaso inteligente, tres minijuegos con
> récords, Destellos con tienda estética (luz, timbre, manipulador, decoración de la isla), logros,
> racha suave, biblioteca, traductor y ajustes. Próximo: capítulos «Operación Sombra» y «Señal
> Estelar» (Etapa 4).

- Diseño: [`docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md`](docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md)
- Plan de la etapa 1: [`docs/superpowers/plans/2026-10-31-etapa-1-nucleo-y-traductor.md`](docs/superpowers/plans/2026-10-31-etapa-1-nucleo-y-traductor.md)
- Plan de la etapa 2: [`docs/superpowers/plans/2026-10-31-etapa-2-faro-capitulo-1.md`](docs/superpowers/plans/2026-10-31-etapa-2-faro-capitulo-1.md)
- Plan de la etapa 3a: [`docs/superpowers/plans/2026-10-31-etapa-3a-repaso-y-minijuegos.md`](docs/superpowers/plans/2026-10-31-etapa-3a-repaso-y-minijuegos.md)
- Plan de la etapa 3b: [`docs/superpowers/plans/2026-11-01-etapa-3b-destellos-tienda-logros-racha.md`](docs/superpowers/plans/2026-11-01-etapa-3b-destellos-tienda-logros-racha.md)
- Pruebas en el móvil: [`docs/pruebas/etapa-3b.md`](docs/pruebas/etapa-3b.md)
- Fuentes de la biblioteca: [`docs/biblioteca/fuentes.md`](docs/biblioteca/fuentes.md)

## Compilar

Requisitos: JDK 17 o superior y acceso a internet para descargar el Android SDK y las dependencias.

```bash
bash scripts/setup-android-sdk.sh   # instala el Android SDK en ~/android-sdk y crea local.properties
./gradlew :morse-core:test :game:test :app:testDebugUnitTest   # pruebas
./gradlew :app:assembleDebug                                     # APK de desarrollo
./gradlew :app:assembleRelease                                   # APK de entrega (firmado)
```

El APK de entrega se firma con la clave permanente, que **no** está en el repositorio: se lee de
las variables de entorno `MORSEPRENDE_KEYSTORE_B64` (almacén PKCS12 en base64) y
`MORSEPRENDE_KEYSTORE_PASSWORD`. Sin ellas, `assembleRelease` falla a propósito.
`scripts/print-signing-fingerprint.sh <apk>` muestra la huella del certificado.

## Estructura

- `morse-core/`: Kotlin puro, sin Android. Tabla Morse (UIT-R M.1677-1 + Ñ y !), códec texto⇄Morse,
  tiempos (estándar y Farnsworth) y síntesis de tonos.
- `game/`: Kotlin puro. Modelo y validación del contenido, generador de ejercicios, sesión de
  misión (intentos y estrellas), reglas de desbloqueo, repaso Leitner y motores de los minijuegos.
- `app/`: app Android con Jetpack Compose. Reproducción por sonido, vibración, linterna y pantalla;
  tono en vivo del manipulador; progreso (Room); ajustes (DataStore); pantallas Faro, Misión,
  Biblioteca, Traductor y Ajustes. El contenido narrativo está en `app/src/main/assets/content/es/`.

## Privacidad

La app no declara el permiso de internet, no tiene cuentas, anuncios ni analíticas. Solo pide
permiso para vibrar.
