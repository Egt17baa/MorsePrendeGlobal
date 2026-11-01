# MorsePrende: El Faro

Juego educativo para Android que enseña **código Morse jugando**. Eres el farero de una isla perdida:
barcos, espías y señales del espacio te hablan en Morse, y para avanzar tienes que aprender a
recibir y a enviar. Totalmente gratis, sin anuncios y sin conexión a internet.

> Estado: **Etapa 6 (0.8.0)**, lista para publicar en Google Play cuando se quiera (ver
> [`docs/publicacion/`](docs/publicacion/como-publicar.md)): campaña completa en tres capítulos, «El Faro», «Operación Sombra»
> (todas las letras, números, la Ñ como extra, ruido de radio y contrarreloj) y «Señal Estelar»
> (signos de puntuación, prosignos AR y BT, misiones de velocidad y señales solo de luz o solo de
> vibración). También: repaso inteligente, tres minijuegos con récords, Destellos con tienda
> estética, logros, racha suave, biblioteca con 14 fichas, traductor y ajustes con accesibilidad
> (tamaño de texto, alto contraste, limitar destellos, vibración) y borrar progreso. Paleta propia por
> capítulo y tipografía Atkinson Hyperlegible (OFL) incluida.

- Diseño: [`docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md`](docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md)
- Plan de la etapa 1: [`docs/superpowers/plans/2026-10-31-etapa-1-nucleo-y-traductor.md`](docs/superpowers/plans/2026-10-31-etapa-1-nucleo-y-traductor.md)
- Plan de la etapa 2: [`docs/superpowers/plans/2026-10-31-etapa-2-faro-capitulo-1.md`](docs/superpowers/plans/2026-10-31-etapa-2-faro-capitulo-1.md)
- Plan de la etapa 3a: [`docs/superpowers/plans/2026-10-31-etapa-3a-repaso-y-minijuegos.md`](docs/superpowers/plans/2026-10-31-etapa-3a-repaso-y-minijuegos.md)
- Plan de la etapa 3b: [`docs/superpowers/plans/2026-11-01-etapa-3b-destellos-tienda-logros-racha.md`](docs/superpowers/plans/2026-11-01-etapa-3b-destellos-tienda-logros-racha.md)
- Plan de la etapa 4a: [`docs/superpowers/plans/2026-11-01-etapa-4a-operacion-sombra.md`](docs/superpowers/plans/2026-11-01-etapa-4a-operacion-sombra.md)
- Plan de la etapa 4b: [`docs/superpowers/plans/2026-11-01-etapa-4b-senal-estelar.md`](docs/superpowers/plans/2026-11-01-etapa-4b-senal-estelar.md)
- Plan de la etapa 5: [`docs/superpowers/plans/2026-11-01-etapa-5-pulido-accesibilidad.md`](docs/superpowers/plans/2026-11-01-etapa-5-pulido-accesibilidad.md)
- Plan de la etapa 6: [`docs/superpowers/plans/2026-11-01-etapa-6-preparar-publicacion.md`](docs/superpowers/plans/2026-11-01-etapa-6-preparar-publicacion.md)
- Publicación: [`docs/publicacion/como-publicar.md`](docs/publicacion/como-publicar.md), ficha, política de privacidad y gráficos
- Pruebas en el móvil: [`etapa-4`](docs/pruebas/etapa-4.md), [`etapa-5`](docs/pruebas/etapa-5.md) y [`etapa-6`](docs/pruebas/etapa-6.md)
- Fuentes de la biblioteca: [`docs/biblioteca/fuentes.md`](docs/biblioteca/fuentes.md)

## Compilar

Requisitos: JDK 17 o superior y acceso a internet para descargar el Android SDK y las dependencias.

```bash
bash scripts/setup-android-sdk.sh   # instala el Android SDK en ~/android-sdk y crea local.properties
./gradlew :morse-core:test :game:test :app:testDebugUnitTest   # pruebas
./gradlew :app:assembleDebug                                     # APK de desarrollo
./gradlew :app:assembleRelease                                   # APK de entrega (firmado)
./gradlew :app:bundleRelease                                     # AAB para Google Play (firmado)
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
