# Etapa 1: núcleo Morse y traductor. Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Primer APK instalable de "MorsePrende: El Faro". Incluye la base del proyecto Android,
un núcleo Morse probado, la reproducción por sonido, vibración, linterna y destello de pantalla,
ajustes básicos y la pantalla **Traductor**. Desde un "Faro" provisional se accede al traductor
y a los ajustes.

**Architecture:** Proyecto Gradle con dos módulos:
- `:morse-core`: Kotlin/JVM puro, sin Android. Contiene la tabla de códigos, la codificación y
  decodificación, los tiempos y la síntesis PCM.
- `:app`: Android con Jetpack Compose, una sola Activity, MVVM y dependencias inyectadas a mano
  desde la clase `Application`.

La reproducción convierte el Morse en una línea de tiempo de `Signal` (encendido/apagado más su
duración). Cada salida la consume a su manera: el audio y la vibración la entregan entera al
hardware; la linterna y la pantalla se encienden y apagan con una corrutina.

**Tech Stack:** Kotlin 2.2.21, AGP 8.13.2, Gradle 8.14.3 (wrapper), JDK 21, compileSdk/targetSdk 36,
minSdk 26, Jetpack Compose (BOM), Navigation Compose, DataStore Preferences, kotlinx-coroutines,
JUnit 4, kotlinx-coroutines-test, Robolectric.

**Spec:** `docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md`. En esta etapa se cubren
§5, §7.9, §7.10 (parcial), §8 (icono y paleta del Faro), §9 y §10.

**Etapas siguientes** (cada una tendrá su propio plan):
- Etapa 2: faro, capítulo 1, manipulador, progreso en Room y biblioteca.
- Etapa 3: minijuegos y repaso Leitner.
- Etapa 4: capítulos 2 y 3.

## Global Constraints

- applicationId y namespace: `io.github.egt17baa.morseprende`. No debe cambiar nunca.
- Nombre visible de la app: `MorsePrende: El Faro`.
- Único permiso declarado: `android.permission.VIBRATE`. **Prohibido `INTERNET`**, también si lo
  introduce alguna dependencia. Se comprueba sobre el APK final.
- 100 % offline: sin analíticas, sin servicios de Google Play y sin descargas en tiempo de ejecución.
- Textos de interfaz solo en `res/values/strings.xml`, en español. Nada de textos fijos en el
  código Compose.
- Estándar: Morse internacional (UIT‑R M.1677‑1). `Ñ` y `!` se marcan como no ITU.
- Unidad de tiempo: `1200 / PPM` ms. Espaciado Farnsworth según la fórmula de ARRL (Task 3).
- Tono de 400 a 1000 Hz (600 por defecto). Velocidad de carácter de 5 a 30 PPM. Rampa de 5 ms.
- La paleta y el icono deben ser propios y distintos de la app de referencia (spec §4.1 y §8).
  Prohibido: fondo negro con dorado, azul marino con cian neón, y un icono de círculo con punto y raya.
- Por defecto, la opción **limitar destellos** está activada. Mientras lo esté, la linterna y la
  pantalla no superan 7 PPM (`FLASH_SAFE_WPM = 7`, unos 3 destellos por segundo).
- El código va en inglés y los textos de interfaz y los commits en español.
- Licencias de recursos ajenos registradas en `CREDITS.md`. En esta etapa no se usa ninguno.

## Review Focus

1. **Texto "real" en el traductor**: minúsculas, tildes (`canción`), `ü`, `ñ`, emojis y saltos de
   línea. Se espera que se normalice (á pasa a A y ñ a Ñ), que los caracteres no soportados se
   listen al usuario y que **nunca** haya un fallo. Lo prueba la Task 2.
2. **Morse mal escrito o pegado de otra web**: espacios dobles, `·`, `−`, `—`, `_`, códigos
   inexistentes o texto vacío. Se espera que se normalice, que un código desconocido se muestre
   como `#` y que no haya fallos. Lo prueba la Task 2.
3. **Pulsar Reproducir dos veces, salir de la pantalla o cambiar de salida mientras suena.** Se
   espera que la reproducción anterior se detenga y que la linterna y la pantalla queden
   **apagadas**. Lo prueban las Tasks 5 y 9.
4. **Móvil sin flash, o con la cámara usando la linterna.** Se espera que la opción no aparezca, o
   que se avise sin cerrarse la app. Lo prueban las Tasks 6 y 9.
5. **Pegar un texto enorme.** Se espera que la entrada se limite a 1000 caracteres y que la
   memoria del audio no crezca con la duración, porque se escribe en streaming. Lo prueban las
   Tasks 4 y 9.

---

## Estructura de ficheros

```
settings.gradle.kts · build.gradle.kts · gradle.properties · gradle/libs.versions.toml
gradle/wrapper/* · gradlew · gradlew.bat · .gitignore · CREDITS.md
scripts/setup-android-sdk.sh            instala el SDK en $HOME/android-sdk y escribe local.properties
morse-core/build.gradle.kts
morse-core/src/main/kotlin/io/github/egt17baa/morseprende/core/
  MorseAlphabet.kt   tabla de códigos
  MorseCodec.kt      normalizar, codificar y decodificar
  Signal.kt          data class Signal
  MorseTiming.kt     tiempos y línea de tiempo
  ToneSynth.kt       PCM de un Signal
morse-core/src/test/kotlin/...  (un fichero de pruebas por cada fichero anterior)
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/kotlin/io/github/egt17baa/morseprende/
  MorsePrendeApp.kt          Application y AppContainer (inyección manual)
  MainActivity.kt
  playback/  OutputKind.kt · SignalOutput.kt · SteppedOutput.kt · VibrationPattern.kt · SignalPlayer.kt
             AudioOutput.kt · VibrationOutput.kt · TorchOutput.kt · OutputFactory.kt
  settings/  AppSettings.kt · SettingsRepository.kt · SettingsViewModel.kt · SettingsScreen.kt
  translator/ TranslatorState.kt · TranslatorViewModel.kt · TranslatorScreen.kt
  ui/theme/  Color.kt · Theme.kt
  ui/home/HomeScreen.kt
  ui/navigation/AppNavHost.kt
app/src/main/res/  values/strings.xml · values/themes.xml · drawable/ic_launcher_foreground.xml
                   drawable/ic_launcher_background.xml · mipmap-anydpi-v26/ic_launcher.xml(+_round)
app/src/test/kotlin/io/github/egt17baa/morseprende/...  (pruebas JVM y Robolectric)
```

---

### Task 1: Base del proyecto y SDK

**Files:**
- Create: todos los ficheros de Gradle de la raíz, `scripts/setup-android-sdk.sh`, `.gitignore`,
  `CREDITS.md`, `morse-core/build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`,
  `MainActivity.kt` (un `Text` provisional), `res/values/strings.xml` (`app_name` = `MorsePrende: El Faro`),
  `res/values/themes.xml`
- Test: `morse-core/src/test/kotlin/io/github/egt17baa/morseprende/core/SanityTest.kt`

**Interfaces:**
- Produces: `./gradlew :morse-core:test`, `./gradlew :app:testDebugUnitTest` y
  `./gradlew :app:assembleDebug` funcionando.

- [ ] **Step 1: Escribir `scripts/setup-android-sdk.sh`**
  El script hace lo siguiente y es idempotente (si el SDK ya existe, no hace nada):
  - Descarga `commandlinetools-linux-9862592_latest.zip` desde `https://dl.google.com/android/repository/`.
  - Lo descomprime en `$HOME/android-sdk/cmdline-tools/latest`.
  - Ejecuta `yes | sdkmanager --licenses` e instala `platform-tools`, `platforms;android-36` y `build-tools;36.0.0`.
  - Escribe `sdk.dir=$HOME/android-sdk` en `local.properties`.
- [ ] **Step 2: Ejecutarlo.** Run: `bash scripts/setup-android-sdk.sh && ls $HOME/android-sdk/platforms`. Expected: `android-36`.
- [ ] **Step 3: Generar el wrapper.** Run: `gradle wrapper --gradle-version 8.14.3`. Expected: aparecen `gradlew` y `gradle/wrapper/gradle-wrapper.properties` con 8.14.3.
- [ ] **Step 4: Escribir el catálogo y los scripts de build.**
  - `libs.versions.toml`: agp 8.13.2, kotlin 2.2.21 con los plugins `org.jetbrains.kotlin.jvm`,
    `org.jetbrains.kotlin.android` y `org.jetbrains.kotlin.plugin.compose`, compose-bom,
    activity-compose, lifecycle (runtime-compose y viewmodel-compose), navigation-compose,
    datastore-preferences, coroutines (core y test), junit 4.13.2, robolectric 4.16.1,
    androidx-test-core y compose ui-test-junit4/ui-test-manifest.
  - **Regla de versiones de AndroidX:** usar la versión estable más reciente que compile con
    compileSdk 36 y AGP 8.13. Si `checkDebugAarMetadata` exige un compileSdk o un AGP mayor,
    bajar a la anterior hasta que pase.
  - `:morse-core`: plugin `kotlin.jvm`, `jvmToolchain(17)`, dependencias de test junit y kotlin-test.
  - `:app`: `namespace` y `applicationId` = `io.github.egt17baa.morseprende`, minSdk 26,
    targetSdk/compileSdk 36, `versionCode 1`, `versionName "0.1.0"`, compose habilitado,
    `testOptions.unitTests.isIncludeAndroidResources = true` y dependencia de `project(":morse-core")`.
- [ ] **Step 5: Escribir el manifiesto.** Solo `<uses-permission android:name="android.permission.VIBRATE"/>`, con `android:allowBackup="true"`, `android:label="@string/app_name"`, `MainActivity` exportada con LAUNCHER y `android:screenOrientation="portrait"`.
- [ ] **Step 6: Escribir la prueba de humo** `SanityTest.kt` con `assertEquals(4, 2 + 2)`.
- [ ] **Step 7: Verificar.** Run: `./gradlew :morse-core:test :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 8: Verificar los permisos.** Run: `$HOME/android-sdk/build-tools/36.0.0/aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk`. Expected: aparece `android.permission.VIBRATE` (más el permiso `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` propio de la app que añade AndroidX) y **no** aparece `INTERNET`.
- [ ] **Step 9: Commit.** `.gitignore` incluye `build/`, `.gradle/`, `local.properties`, `*.jks`, `*.keystore` e `.idea/`.
  ```bash
  git add -A && git commit -m "Crear proyecto Android base con módulos morse-core y app"
  ```

---

### Task 2: Alfabeto y códec

**Files:**
- Create: `morse-core/src/main/kotlin/io/github/egt17baa/morseprende/core/MorseAlphabet.kt`, `MorseCodec.kt`
- Test: `morse-core/src/test/kotlin/io/github/egt17baa/morseprende/core/MorseAlphabetTest.kt`, `MorseCodecTest.kt`
- Delete: `SanityTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object MorseAlphabet {
      val codes: Map<Char, String>          // carácter en mayúsculas → ".-"
      val nonItu: Set<Char>                 // setOf('Ñ', '!')
      fun codeFor(c: Char): String?         // ignora mayúsculas y minúsculas
      fun charFor(code: String): Char?
  }
  object MorseCodec {
      const val UNKNOWN = '#'
      data class EncodeResult(val morse: String, val unsupported: Set<String>)  // code points sin soporte, p. ej. "😀"
      fun normalizeText(text: String): String   // mayúsculas, quita diacríticos excepto la Ñ, espacios en blanco agrupados en un solo espacio, recortado
      fun normalizeMorse(morse: String): String  // · • → .   − – — _ → -   letras separadas por un espacio, palabras por " / "
      fun encode(text: String): EncodeResult     // letras separadas por " ", palabras por " / "
      fun decode(morse: String): String          // código desconocido → '#'; palabras separadas por un espacio
  }
  ```

- [ ] **Step 1: Escribir las pruebas que fallan**
  - Tabla exacta de 51 entradas. En `MorseAlphabetTest`, la constante `EXPECTED` debe ser
    igual a `codes` entera:
    ```
    A .-  B -...  C -.-.  D -..  E .  F ..-.  G --.  H ....  I ..  J .---  K -.-  L .-..  M --
    N -.  O ---  P .--.  Q --.-  R .-.  S ...  T -  U ..-  V ...-  W .--  X -..-  Y -.--  Z --..
    Ñ --.--
    0 -----  1 .----  2 ..---  3 ...--  4 ....-  5 .....  6 -....  7 --...  8 ---..  9 ----.
    . .-.-.-  , --..--  ? ..--..  ' .----.  ! -.-.--  / -..-.  ( -.--.  ) -.--.-  : ---...
    = -...-  + .-.-.  - -....-  " .-..-.  @ .--.-.
    ```
  - `codes_have_no_duplicate_codes`: `codes.values.toSet().size == codes.size`.
  - `MorseCodecTest`:
    ```kotlin
    @Test fun encode_sos() = assertEquals("... --- ...", MorseCodec.encode("sos").morse)
    @Test fun encode_words() = assertEquals(".... --- .-.. .- / -- ..- -. -.. ---", MorseCodec.encode("Hola mundo").morse)
    @Test fun encode_strips_accents_keeps_enye() =
        assertEquals("-.-. .- -. -.-. .. --- -. / -. .. --.-- ---", MorseCodec.encode("Canción  niño").morse)
    @Test fun encode_dieresis() = assertEquals("--. ..- . .-. .-", MorseCodec.encode("güera").morse)
    ```
    - `encode_reports_unsupported`: `encode("Hi 😀 #")` da `morse == ".... .."` y
      `unsupported == setOf("😀", "#")`. Se recorre por code points. Una palabra que queda vacía
      tras filtrar no deja un ` / ` sobrante.
    - `encode_newlines_are_word_breaks`: `encode("a\nb").morse == ".- / -..."`.
    - `encode_empty`: `encode("   ").morse == ""`.
    - `decode_basic`: `decode("... --- ... / .- ") == "SOS A"`.
    - `decode_unicode_dashes`: `decode("·−·−") == "#"` porque `.-.-` no existe, y
      `decode("·− −···") == "AB"`.
    - `decode_unknown_and_garbage`: `decode(".-.-.-.-. x ..") == "##I"`. Un token con caracteres
      que no son punto ni raya cuenta como desconocido.
    - `decode_extra_spaces`: `decode("  .-   -...  ") == "A B"`. Tres o más espacios separan palabras.
    - `round_trip_all_characters`: para cada `c` de `codes.keys`, `decode(encode(c.toString()).morse) == c.toString()`.
- [ ] **Step 2: Ejecutar para ver que fallan.** Run: `./gradlew :morse-core:test`. Expected: FAIL (referencias sin resolver).
- [ ] **Step 3: Implementar.**
  - `normalizeText`: `java.text.Normalizer` en NFD, quitando las marcas `\p{Mn}`. Para conservar
    la `Ñ` (y la `ñ`), se sustituye antes por un marcador y se restaura después.
  - Separadores de palabra en `normalizeMorse`: `/`, `|` o 3 o más espacios seguidos.
- [ ] **Step 4: Ejecutar las pruebas.** Run: `./gradlew :morse-core:test`. Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir alfabeto Morse y códec texto⇄Morse con normalización"`

---

### Task 3: Tiempos y línea de tiempo

**Files:**
- Create: `core/Signal.kt`, `core/MorseTiming.kt`
- Test: `core/MorseTimingTest.kt`

**Interfaces:**
- Consumes: `MorseCodec.normalizeMorse`
- Produces:
  ```kotlin
  data class Signal(val on: Boolean, val durationMs: Long)
  class MorseTiming(val charWpm: Int, val effectiveWpm: Int = charWpm) {
      // require(charWpm in 5..30); require(effectiveWpm in 5..charWpm)
      val unitMs: Long; val charGapMs: Long; val wordGapMs: Long
      fun signalsFor(morse: String): List<Signal>   // normaliza primero; ignora los tokens no válidos; sin silencio final
      companion object { fun totalMs(signals: List<Signal>): Long }
  }
  ```

- [ ] **Step 1: Escribir las pruebas que fallan**
  ```kotlin
  @Test fun standard_20wpm() { val t = MorseTiming(20); assertEquals(60, t.unitMs); assertEquals(180, t.charGapMs); assertEquals(420, t.wordGapMs) }
  @Test fun farnsworth_18_10() { val t = MorseTiming(18, 10); assertEquals(67, t.unitMs); assertEquals(621, t.charGapMs); assertEquals(1449, t.wordGapMs) }
  @Test fun signals_E_space_T() = assertEquals(
      listOf(Signal(true, 60), Signal(false, 420), Signal(true, 180)), MorseTiming(20).signalsFor(". / -"))
  @Test fun signals_A() = assertEquals(listOf(Signal(true,60), Signal(false,60), Signal(true,180)), MorseTiming(20).signalsFor(".-"))
  @Test fun signals_letters_gap() = assertEquals(180, MorseTiming(20).signalsFor(". .")[1].durationMs)
  @Test fun signals_empty_or_garbage() = assertEquals(emptyList(), MorseTiming(20).signalsFor("  x / "))
  @Test fun rejects_out_of_range() { assertFailsWith<IllegalArgumentException> { MorseTiming(4) }; assertFailsWith<IllegalArgumentException> { MorseTiming(10, 12) } }
  @Test fun paris_is_one_minute_at_wpm() // "PARIS " = 50 unidades: totalMs(signalsFor(encode("PARIS PARIS"))) + wordGap == 2 * 60000 / wpm (±2 %) con wpm = 12
  ```
- [ ] **Step 2: Ejecutar para ver que fallan.** Run: `./gradlew :morse-core:test --tests '*MorseTimingTest*'`. Expected: FAIL.
- [ ] **Step 3: Implementar.**
  - `unitMs = round(1200.0 / charWpm)`.
  - Si `effectiveWpm == charWpm`: `charGap = 3u` y `wordGap = 7u`.
  - Si no (Farnsworth, ARRL): `ta = (60·c − 37.2·e) / (c·e)` segundos,
    `charGap = round(3·ta/19 · 1000)` y `wordGap = round(7·ta/19 · 1000)`.
  - Raya = 3u y silencio entre elementos = u.
- [ ] **Step 4: Ejecutar las pruebas.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir tiempos Morse (estándar y Farnsworth) y línea de tiempo de señales"`

---

### Task 4: Síntesis de tono

**Files:**
- Create: `core/ToneSynth.kt`
- Test: `core/ToneSynthTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object ToneSynth {
      fun samplesFor(durationMs: Long, sampleRate: Int): Int    // durationMs * sampleRate / 1000
      fun render(signal: Signal, sampleRate: Int, freqHz: Int, volume: Float, rampMs: Int = 5): ShortArray
  }
  ```
  Cada `Signal` se sintetiza por separado. Así el audio se escribe en streaming, una señal cada vez,
  y la memoria es proporcional a la señal más larga, no a todo el mensaje (Review Focus 5).

- [ ] **Step 1: Escribir las pruebas que fallan**
  - `length_matches_duration`: `render(Signal(true,100), 48000, 600, 1f).size == 4800`.
  - `off_is_silence`: `render(Signal(false,50), …)` es todo ceros.
  - `starts_and_ends_near_zero`: en un tono de 100 ms, `abs(s[0]) < 200` y `abs(s.last()) < 200`.
  - `peak_respects_volume`: con `volume = 0.5f`, `max(abs) <= 0.5 * 0.8 * Short.MAX_VALUE + 1`
    y `>= 0.45 * 0.8 * Short.MAX_VALUE`.
  - `frequency_is_correct`: en 1 s a 600 Hz y 48000 muestras/s, los cruces por cero ascendentes
    son 600 ± 2.
  - `ramp_shorter_than_tone`: con un tono de 6 ms y rampa de 5 ms no hay excepción (la rampa se
    recorta a la mitad de la duración).
- [ ] **Step 2: Ejecutar para ver que fallan.** Expected: FAIL.
- [ ] **Step 3: Implementar.** Seno con amplitud `volume * 0.8 * Short.MAX_VALUE` y envolvente de
  coseno alzado al subir y al bajar, de `min(rampMs, durationMs/2)` cada una.
- [ ] **Step 4: Ejecutar las pruebas.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir síntesis PCM de tonos Morse sin clics"`

---

### Task 5: Lógica de reproducción (testable sin hardware)

**Files:**
- Create: `app/.../playback/OutputKind.kt`, `SignalOutput.kt`, `SteppedOutput.kt`, `VibrationPattern.kt`, `SignalPlayer.kt`
- Test: `app/src/test/.../playback/SteppedOutputTest.kt`, `VibrationPatternTest.kt`, `SignalPlayerTest.kt`

**Interfaces:**
- Consumes: `Signal`
- Produces:
  ```kotlin
  enum class OutputKind { SOUND, VIBRATION, TORCH, SCREEN }
  interface SignalOutput {
      suspend fun play(signals: List<Signal>)   // vuelve al terminar; si se cancela, deja la salida apagada
      fun stop()                                // idempotente; deja la salida apagada
  }
  class SteppedOutput(private val setOn: (Boolean) -> Unit) : SignalOutput  // usa delay()
  fun vibrationPattern(signals: List<Signal>): LongArray   // [0, on, off, on, ...] para VibrationEffect.createWaveform
  class SignalPlayer(private val scope: CoroutineScope) {
      val isPlaying: StateFlow<Boolean>
      fun play(signals: List<Signal>, outputs: List<SignalOutput>)  // cancela la reproducción anterior y lanza las salidas en paralelo
      fun stop()
  }
  ```

- [ ] **Step 1: Escribir las pruebas que fallan** (`runTest` con tiempo virtual)
  - `stepped_toggles_at_expected_times`: graba `(currentTime, on)` en cada llamada a `setOn`. Con
    `[on60, off60, on180]` se esperan los eventos `(0,true), (60,false), (120,true), (300,false)`.
  - `stepped_cancel_turns_off`: se lanza `play` con `[on1000]`, se avanza 100 ms y se cancela el
    job. El último evento debe ser `false`.
  - `vibration_pattern`: `vibrationPattern([on60, off60, on180])` da `[0,60,60,180]`, y
    `vibrationPattern([])` da `[]`.
  - `player_second_play_cancels_first` (Review Focus 3): se usan dos `SteppedOutput` falsos `a` y
    `b`. Se hace `play(long, [a])`, se avanza 50 ms y luego `play(short, [b])`. El último estado
    de `a` es `false` y `isPlaying` vuelve a `false` cuando `b` termina.
  - `player_stop_turns_everything_off`: `play([on1000], [a, b])`, avanzar 10 ms, `stop()`. Ambas
    salidas terminan apagadas e `isPlaying == false`.
  - `player_is_playing_lifecycle`: vale `true` durante la reproducción y `false` al terminar.
- [ ] **Step 2: Ejecutar para ver que fallan.** Run: `./gradlew :app:testDebugUnitTest --tests '*playback*'`. Expected: FAIL.
- [ ] **Step 3: Implementar.** `SignalPlayer` guarda un `Job?`. En `play`, cancela el anterior y
  lanza `coroutineScope { outputs.forEach { launch { it.play(signals) } } }` dentro de un
  `try/finally` que llama a `stop()` en todas las salidas y pone `isPlaying` a `false`.
- [ ] **Step 4: Ejecutar las pruebas.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir reproductor de señales con salidas por pasos y patrón de vibración"`

---

### Task 6: Salidas de hardware (audio, vibración, linterna)

**Files:**
- Create: `app/.../playback/AudioOutput.kt`, `VibrationOutput.kt`, `TorchOutput.kt`, `OutputFactory.kt`
- Test: `app/src/test/.../playback/TorchOutputTest.kt` (Robolectric)

**Interfaces:**
- Consumes: `SignalOutput`, `SteppedOutput`, `vibrationPattern`, `ToneSynth`
- Produces:
  ```kotlin
  class AudioOutput(private val freqHz: Int, private val volume: Float) : SignalOutput
  class VibrationOutput(context: Context) : SignalOutput
  class TorchOutput(context: Context) : SignalOutput {
      companion object { fun isAvailable(context: Context): Boolean }
  }
  interface OutputFactory { fun create(kind: OutputKind, settings: AppSettings): SignalOutput }  // SCREEN no lo crea la fábrica (ver Task 9)
  class AndroidOutputFactory(context: Context) : OutputFactory
  ```
  `AppSettings` se define en la Task 7. Esta task puede crear `AppSettings` con sus campos
  definitivos si se ejecuta antes.

- [ ] **Step 1: Escribir las pruebas que fallan (Robolectric)**
  - `torch_unavailable_without_flash`: si `PackageManager.FEATURE_CAMERA_FLASH` falta en
    `ShadowPackageManager`, `isAvailable` devuelve `false`.
  - `torch_play_survives_camera_exception` (Review Focus 4): con un `CameraManager` sombreado que
    no tiene cámaras, `play([on60])` termina sin lanzar excepción.
- [ ] **Step 2: Ejecutar para ver que fallan.** Expected: FAIL.
- [ ] **Step 3: Implementar.**
  - `AudioOutput`: en `play`, sobre `Dispatchers.IO`, crea un `AudioTrack` en `MODE_STREAM` con
    `USAGE_GAME`, `ENCODING_PCM_16BIT`, mono, frecuencia nativa
    (`AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE`, 48000 si es nula) y
    `PERFORMANCE_MODE_LOW_LATENCY`. Llama a `play()` y escribe `ToneSynth.render(signal…)` por
    cada señal comprobando `isActive`. En `finally` hace `stop()`/`flush()`/`release()`.
  - `VibrationOutput`: `createWaveform(vibrationPattern(signals), -1)` y después
    `delay(totalMs)`. Al cancelar o con `stop()` se llama a `cancel()`. Usa `VibratorManager` en
    API 31+ y `Vibrator` en las anteriores. Si no hay vibrador, `play` vuelve sin hacer nada.
  - `TorchOutput`: es un `SteppedOutput` que llama a `CameraManager.setTorchMode(id, on)` sobre la
    primera cámara con `FLASH_INFO_AVAILABLE`. Captura `CameraAccessException` e
    `IllegalArgumentException`. Si falla, deja de intentarlo durante esa reproducción.
- [ ] **Step 4: Ejecutar las pruebas.** Run: `./gradlew :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir salidas de audio en streaming, vibración y linterna"`

---

### Task 7: Ajustes (DataStore y pantalla)

**Files:**
- Create: `app/.../settings/AppSettings.kt`, `SettingsRepository.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`
- Test: `app/src/test/.../settings/SettingsRepositoryTest.kt` (Robolectric), `SettingsViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class AppSettings(
      val charWpm: Int = 18, val effectiveWpm: Int = 10, val toneHz: Int = 600,
      val volume: Float = 0.8f, val limitFlashes: Boolean = true, val flashWarningAccepted: Boolean = false,
  ) { fun clamped(): AppSettings }  // charWpm 5..30, effectiveWpm 5..charWpm, toneHz 400..1000, volume 0..1
  interface SettingsRepository {
      val settings: Flow<AppSettings>
      suspend fun update(transform: (AppSettings) -> AppSettings)   // guarda transform(actual).clamped()
  }
  class DataStoreSettingsRepository(context: Context) : SettingsRepository
  class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository   // en src/test, también la usa la Task 9
  class SettingsViewModel(repo: SettingsRepository) : ViewModel {
      val state: StateFlow<AppSettings>
      fun setCharWpm(v: Int); fun setEffectiveWpm(v: Int); fun setToneHz(v: Int); fun setVolume(v: Float); fun setLimitFlashes(v: Boolean)
  }
  ```

- [ ] **Step 1: Escribir las pruebas que fallan**
  - `defaults`: el primer valor de `settings` es igual a `AppSettings()`.
  - `persists`: `update { it.copy(toneHz = 750) }` y después un repositorio nuevo sobre el mismo
    fichero lee 750.
  - `clamps`: `update { it.copy(charWpm = 99, effectiveWpm = 50, toneHz = 100) }` da `30`, `30` y `400`.
  - `lowering_char_wpm_lowers_effective`: con `charWpm = 15` y `effectiveWpm = 12`, `setCharWpm(8)`
    deja `effectiveWpm == 8`.
- [ ] **Step 2: Ejecutar para ver que fallan.** Expected: FAIL.
- [ ] **Step 3: Implementar.**
  - DataStore en `preferencesDataStore("settings")`.
  - Pantalla con deslizadores para PPM de carácter (5–30), PPM efectiva (5–carácter),
    tono (400–1000, pasos de 10) y volumen.
  - Interruptor "Limitar destellos (recomendado)" con texto explicativo.
  - Botón "Probar sonido" que reproduce `PARIS` con los ajustes actuales.
- [ ] **Step 4: Ejecutar las pruebas.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir ajustes de velocidad, tono, volumen y limitación de destellos"`

---

### Task 8: Esqueleto de la app (tema, icono, navegación y Faro provisional)

**Files:**
- Create: `MorsePrendeApp.kt`, `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/home/HomeScreen.kt`,
  `ui/navigation/AppNavHost.kt`, recursos del icono
- Modify: `MainActivity.kt`, `AndroidManifest.xml` (`android:name=".MorsePrendeApp"` e icono),
  `res/values/strings.xml`
- Test: `app/src/test/.../ui/HomeScreenTest.kt` (Compose y Robolectric)

**Interfaces:**
- Consumes: `DataStoreSettingsRepository`, `AndroidOutputFactory`, `SignalPlayer`, `TorchOutput.isAvailable`
- Produces:
  ```kotlin
  class AppContainer(context: Context) { val settings: SettingsRepository; val outputs: OutputFactory; val torchAvailable: Boolean }
  object Routes { const val HOME = "home"; const val TRANSLATOR = "translator"; const val SETTINGS = "settings" }
  ```

- [ ] **Step 1: Escribir la prueba que falla.** `home_navigates_to_translator`: al pulsar el
  botón con texto `R.string.home_translator` se muestra el texto `R.string.translator_title`.
- [ ] **Step 2: Ejecutar para ver que falla.** Expected: FAIL.
- [ ] **Step 3: Implementar.**
  - **Paleta del Faro** (tema oscuro y claro):

    | Token | Oscuro | Claro |
    |---|---|---|
    | Fondo | `#0E2A2F` (verde azulado de mar nocturno) | `#F4EFE3` |
    | Superficie | `#163C42` | `#FFFFFF` |
    | Primario | `#FFE8B0` (crema de luz de faro) | `#1F5F66` |
    | Secundario | `#FF7A5C` (coral) | `#D9573B` |
    | Texto | `#F4EFE3` | `#0E2A2F` |

    Nada de negro puro ni dorado.
  - **Icono adaptativo vectorial** propio: un faro estilizado sobre fondo de mar. Del faro sale un
    haz de luz y dentro del haz hay un punto y una raya. No usar un círculo como forma principal.
  - **HomeScreen**: título, un faro dibujado con `Canvas` cuyo haz gira suavemente con
    `rememberInfiniteTransition`, y botones "Traductor" y "Ajustes". Una tarjeta deshabilitada
    "Campaña: próximamente" anuncia la Etapa 2.
- [ ] **Step 4: Ejecutar la prueba.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir tema del Faro, icono propio, navegación y pantalla principal provisional"`

---

### Task 9: ViewModel del traductor

**Files:**
- Create: `app/.../translator/TranslatorState.kt`, `TranslatorViewModel.kt`
- Test: `app/src/test/.../translator/TranslatorViewModelTest.kt`

**Interfaces:**
- Consumes: `MorseCodec`, `MorseTiming`, `SignalPlayer`, `SteppedOutput`, `OutputFactory`, `SettingsRepository`
- Produces:
  ```kotlin
  enum class Direction { TEXT_TO_MORSE, MORSE_TO_TEXT }
  data class TranslatorState(
      val input: String = "", val direction: Direction = Direction.TEXT_TO_MORSE, val output: String = "",
      val unsupported: Set<String> = emptySet(), val outputs: Set<OutputKind> = setOf(OutputKind.SOUND),
      val torchAvailable: Boolean = false, val isPlaying: Boolean = false, val screenLit: Boolean = false,
      val speedLimitedForFlashes: Boolean = false, val showFlashWarning: Boolean = false,
  ) { val canPlay: Boolean }   // true si hay Morse que reproducir y al menos una salida
  class TranslatorViewModel(settings: SettingsRepository, outputs: OutputFactory, torchAvailable: Boolean,
                            playerScope: CoroutineScope? = null /* null → viewModelScope */) : ViewModel {
      companion object { const val MAX_INPUT = 1000; const val FLASH_SAFE_WPM = 7 }
      val state: StateFlow<TranslatorState>
      fun onInputChange(text: String); fun swapDirection(); fun toggleOutput(kind: OutputKind)
      fun acceptFlashWarning(); fun dismissFlashWarning(); fun play(); fun stop(); fun clear()
  }
  ```

- [ ] **Step 1: Escribir las pruebas que fallan.** Se usan `FakeSettingsRepository` y una
  `FakeOutputFactory` que devuelve `SteppedOutput` grabadores.
  - `translates_text`: `onInputChange("sos")` da `output == "... --- ..."`.
  - `translates_morse`: `swapDirection()` y luego `onInputChange("... --- ...")` da `output == "SOS"`.
  - `swap_moves_output_to_input`: con `"sos"` en la entrada, `swapDirection()` deja
    `input == "... --- ..."`, `output == "SOS"` y `direction == MORSE_TO_TEXT`.
  - `truncates_input` (Review Focus 5): con una entrada de 5000 caracteres, `input.length == 1000`.
  - `unsupported_listed`: `onInputChange("a😀")` da `unsupported == setOf("😀")`.
  - `torch_not_offered_when_unavailable` (Review Focus 4): con `torchAvailable = false`,
    `toggleOutput(TORCH)` no cambia `outputs`.
  - `first_light_output_asks_warning`: con `flashWarningAccepted = false`, `toggleOutput(SCREEN)`
    da `showFlashWarning == true` y `SCREEN` no entra en `outputs`. `acceptFlashWarning()` lo
    añade y persiste `flashWarningAccepted = true`.
  - `flash_limit_caps_speed`: con `charWpm = 20`, `limitFlashes = true` y `SCREEN` activo,
    `play()` da `speedLimitedForFlashes == true`, y el primer `Signal` que recibe la salida dura
    `round(1200/7) = 171` ms. Con `limitFlashes = false` dura 60 ms.
  - `play_twice_restarts` (Review Focus 3): `play()`, avanzar 20 ms, `play()`: la primera salida
    falsa acaba apagada y `isPlaying` sigue en `true`.
  - `stop_turns_screen_off`: con `SCREEN` activo, `play()`, avanzar 10 ms y `stop()` da
    `screenLit == false` e `isPlaying == false`.
  - `clear_resets`: `clear()` deja `input` y `output` vacíos y detiene la reproducción.
- [ ] **Step 2: Ejecutar para ver que fallan.** Expected: FAIL.
- [ ] **Step 3: Implementar.**
  - El Morse que se reproduce es `output` en `TEXT_TO_MORSE` y `normalizeMorse(input)` en `MORSE_TO_TEXT`.
  - La salida `SCREEN` es un `SteppedOutput { lit -> update(screenLit = lit) }` creado por el ViewModel.
  - `onCleared` llama a `stop()`.
  - `isPlaying` refleja `SignalPlayer.isPlaying`.
- [ ] **Step 4: Ejecutar las pruebas.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir lógica del traductor con límite de destellos y reproducción segura"`

---

### Task 10: Pantalla del traductor

**Files:**
- Create: `app/.../translator/TranslatorScreen.kt`
- Modify: `AppNavHost.kt` (ruta `TRANSLATOR`), `strings.xml`
- Test: `app/src/test/.../translator/TranslatorScreenTest.kt` (Compose y Robolectric)

**Interfaces:**
- Consumes: `TranslatorViewModel`, `TranslatorState`, `Direction`, `OutputKind`

- [ ] **Step 1: Escribir las pruebas que fallan.**
  - `typing_shows_morse`: escribir `sos` en el campo de entrada hace que se muestre `... --- ...`.
  - `torch_chip_hidden_without_flash`: con `torchAvailable = false` no existe ningún nodo con el
    texto `R.string.output_torch`.
  - `warning_dialog_appears`: al pulsar el chip de pantalla se muestra `R.string.flash_warning_title`.
- [ ] **Step 2: Ejecutar para ver que fallan.** Expected: FAIL.
- [ ] **Step 3: Implementar.** Diseño propio (no reproducir la referencia):
  - Una "libreta de telegrafista" con dos tarjetas apiladas y, entre ellas, un botón circular
    para invertir el sentido con el icono de dos flechas.
  - El Morse se muestra con puntos y rayas gruesos (`•` y `▬`) y espaciado amplio. Se puede
    copiar con un botón; la entrada admite pegar desde el menú del campo.
  - Fila de `FilterChip` para elegir las salidas: Sonido, Vibración, Linterna (solo si hay flash)
    y Pantalla.
  - Botones grandes Reproducir/Detener y Borrar.
  - Aviso en línea si hay caracteres no soportados o si la velocidad está limitada por los destellos.
  - Mientras `screenLit` es `true`, una capa a pantalla completa de color `#FFE8B0`.
  - Diálogo de aviso de fotosensibilidad: "Destellos de luz. Si tienes epilepsia fotosensible o
    eres sensible a las luces intermitentes, no uses esta opción…".
  - `DisposableEffect` que llama a `stop()` al salir de la pantalla.
  - Todos los textos en `strings.xml`.
- [ ] **Step 4: Ejecutar las pruebas.** Run: `./gradlew :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir pantalla del traductor con salidas y aviso de fotosensibilidad"`

---

### Task 11: APK de la Etapa 1 y entrega

**Files:**
- Create: `docs/pruebas/etapa-1.md` (lista de cosas que probar en el móvil)
- Modify: `README.md` (qué es, cómo compilar con `scripts/setup-android-sdk.sh` y `./gradlew assembleDebug`)

- [ ] **Step 1: Suite completa.** Run: `./gradlew :morse-core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. Expected: `BUILD SUCCESSFUL` y lint sin errores (se admiten avisos).
- [ ] **Step 2: Permisos del APK.** Run: `aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk | grep -c INTERNET`. Expected: `0`.
- [ ] **Step 3: Escribir `docs/pruebas/etapa-1.md`.** Lista de comprobaciones para el Honor
  Magic 8 Pro:
  - Instalar el APK.
  - SOS con sonido sin clics.
  - Cambiar el tono y la velocidad.
  - Linterna a 7 PPM con el límite activado.
  - Vibración (en MagicOS revisar Ajustes > Sonido y vibración > intensidad).
  - Destello de pantalla.
  - Salir de la pantalla mientras suena: todo se apaga.
  - Pegar un texto con tildes y emojis.
  - Girar el móvil: la app se queda en vertical.
- [ ] **Step 4: Commit y push.**
  ```bash
  git add -A && git commit -m "Etapa 1: README, lista de pruebas y verificación del APK"
  git push -u origin ccr-675a91da-h876sj
  ```
- [ ] **Step 5: Entregar.** Copiar el APK a `dist/morseprende-etapa1-debug.apk` (ignorado en git)
  y enviarlo al usuario con `SendUserFile`, junto con la lista de pruebas.
  **Aviso para el usuario:** está firmado con una clave de depuración temporal. Para instalar el
  APK de la siguiente etapa habrá que desinstalar este. Todavía no guarda progreso, así que no se
  pierde nada. La clave permanente se configura al empezar la Etapa 2.
