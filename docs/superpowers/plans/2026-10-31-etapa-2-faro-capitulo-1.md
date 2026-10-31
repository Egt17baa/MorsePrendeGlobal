# Etapa 2: el Faro, el capítulo 1, el manipulador y la biblioteca. Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que "MorsePrende: El Faro" sea jugable. El objetivo de esta etapa es:
- Una campaña del **capítulo 1, El Faro**, con 8 misiones narrativas: escuchar, recibir, enviar con el **manipulador** y descifrar el mensaje final.
- **Progreso guardado** (estrellas y misiones desbloqueadas).
- Un **Faro** como pantalla principal, con el mapa de misiones.
- La **biblioteca** con fichas históricas verificadas.
- Un APK **firmado con la clave permanente**, para que las siguientes versiones se instalen encima sin perder el progreso.

**Architecture:** Se añade un módulo puro de Kotlin, `:game`, que depende de `:morse-core` y no
de Android. Contiene:
- el modelo y la validación del contenido (JSON);
- la generación de ejercicios de cada misión;
- la sesión de misión, con sus reglas de puntuación;
- las reglas de desbloqueo.

`:morse-core` gana el decodificador del manipulador. `:app` añade:
- Room para el progreso;
- la carga del contenido desde `assets/content/es/`;
- el tono en vivo del manipulador;
- las pantallas de Faro (mapa), Misión y Biblioteca.

**Tech Stack:** lo de la Etapa 1, más:
- kotlinx-serialization-json 1.9.0 y el plugin `org.jetbrains.kotlin.plugin.serialization` 2.2.21;
- Room 2.8.x con KSP `2.2.21-2.0.5`.

**Spec:** `docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md`. En esta etapa se cubren
§7.1 (cap. 1), §7.2 (cap. 1), §7.3, §7.4, §7.8 (cap. 1 y fichas iniciales), §9 (Room y contenido en
ficheros) y §10.

**Fuera de esta etapa:**
- Etapa 3: minijuegos (§7.5), repaso Leitner (§7.6), Destellos, logros y racha (§7.7).
- Etapa 4: capítulos 2 y 3.

En esta etapa, las misiones de repaso eligen los caracteres aprendidos de forma uniforme.

## Global Constraints

- Las mismas de la Etapa 1, salvo una: el APK que se entrega ahora es el **release**, firmado con
  la clave permanente. Además, siguen vigentes:
  - applicationId `io.github.egt17baa.morseprende`;
  - único permiso `VIBRATE` y **sin `INTERNET`**;
  - textos de interfaz en `strings.xml`;
  - límite de destellos.
- **Firma:**
  - La clave se lee de las variables de entorno `MORSEPRENDE_KEYSTORE_B64` (PKCS12 en base64) y
    `MORSEPRENDE_KEYSTORE_PASSWORD`, con alias `morseprende` y la misma contraseña para el almacén
    y para la clave.
  - La clave **nunca** se escribe en el repositorio: se decodifica en `app/build/signing/`, que
    está ignorado por git.
  - `assembleRelease` **falla** con un mensaje claro si faltan esas variables.
  - Las builds debug siguen con la clave de depuración.
- `versionCode 2`, `versionName "0.2.0"`.
- El contenido narrativo y las fichas viven en `app/src/main/assets/content/es/*.json`, separados
  del código. Los textos de interfaz (botones y etiquetas) siguen en `strings.xml`.
- **Orden de letras del capítulo 1 (spec §7.2):** S O → E T → A N → I M → R D.
- Toda palabra o mensaje de una misión usa **solo** caracteres ya aprendidos hasta esa misión.
  Lo comprueba un test sobre el contenido real.
- **Estrellas:** acierto ≥ 0,90 da 3; ≥ 0,70 da 2; cualquier otro resultado da 1 si se completa la
  misión. Se guarda la mejor marca. Se desbloquea la siguiente misión al completar la actual.
- **Manipulador (spec §7.4):**
  - Es punto si la pulsación dura menos de 2 unidades; si no, es raya.
  - Fin de letra con un silencio ≥ 2,5 unidades y fin de palabra con ≥ 6 unidades.
  - La unidad se adapta con una media móvil (α = 0,25) y se mantiene entre 40 y 600 ms.
  - Se ignoran las pulsaciones de menos de 15 ms (rebotes).
  - Hay modo alternativo de dos botones (punto | raya) más "siguiente letra".
- Tono en vivo del manipulador: latencia objetivo < 30 ms. Es el mejor esfuerzo y no bloquea.
- Fichas de la biblioteca:
  - entre 150 y 300 palabras cada una, más un "¿Sabías que…?";
  - cada dato verificado con fuentes fiables, apuntadas en `docs/biblioteca/fuentes.md`;
  - **el usuario revisa** las fichas antes de darlas por buenas.

## Review Focus

1. **Principiante con ritmo irregular en el manipulador:** pausas largas dentro de una letra,
   toques muy cortos o rebotes. Se espera que la unidad se adapte, que no salgan letras basura y
   que exista el modo de dos botones. Lo prueba la Task 2.
2. **Salir de una misión a medias** (botón atrás, botón de inicio, una llamada). Se espera:
   - que no se guarde un progreso falso;
   - que el tono del manipulador y la reproducción se detengan, sin tono atascado;
   - que al volver la misión empiece de nuevo.

   Lo prueban las Tasks 10 y 11.
3. **Respuestas escritas con minúsculas, tildes o espacios de más.** Se espera que se acepten como
   correctas. Lo prueba la Task 6.
4. **Contenido JSON roto o incoherente.** Se espera que un test lo detecte antes de compilar. Si
   aun así falla al cargar, se muestra un mensaje de error en vez de cerrar la app. Lo prueban las
   Tasks 4 y 8.
5. **Repetir una misión y sacar menos estrellas.** Se espera que se conserve la mejor marca y que
   los desbloqueos no cambien. Lo prueba la Task 8.

---

## Estructura de ficheros (nuevos o modificados)

```
settings.gradle.kts                        + include(":game")
gradle/libs.versions.toml                  + serialization, room, ksp
app/build.gradle.kts                       firma sideload, ksp, room, :game, versión 0.2.0
morse-core/.../core/KeyerDecoder.kt        decodificador del manipulador
game/build.gradle.kts
game/src/main/kotlin/io/github/egt17baa/morseprende/game/
  content/ContentModel.kt       Chapter, Mission, MissionType, StoryMessage, LibraryCard
  content/ContentParser.kt      JSON → modelo
  content/ContentValidator.kt   reglas de coherencia
  mission/Exercise.kt           tipos de ejercicio
  mission/MissionPlanner.kt     genera los ejercicios de una misión
  mission/MissionSession.kt     máquina de estados de una partida y su puntuación
  progress/ProgressRules.kt     desbloqueos de misiones y fichas
game/src/test/kotlin/...                   (pruebas de cada fichero)
app/src/main/assets/content/es/chapter1.json · library.json
app/src/main/kotlin/io/github/egt17baa/morseprende/
  data/ContentSource.kt         interfaz y carga desde assets
  data/progress/                MissionProgressEntity · ProgressDao · AppDatabase · ProgressRepository
  playback/Sidetone.kt          tono en vivo del manipulador
  settings/                     + keyerMode
  ui/hub/                       HubScreen · HubViewModel (sustituye a ui/home/HomeScreen)
  ui/mission/                   MissionScreen · MissionViewModel · KeyerPad · MissionPanels
  ui/library/                   LibraryScreen · LibraryViewModel
  ui/navigation/AppNavHost.kt   rutas nuevas
docs/biblioteca/fuentes.md · docs/pruebas/etapa-2.md
```

---

### Task 1: Firma permanente del release

**Files:**
- Modify: `app/build.gradle.kts`, `.gitignore` (asegurar `app/build/`)
- Create: `scripts/print-signing-fingerprint.sh` (imprime el SHA-256 del certificado del APK)

**Interfaces:**
- Produces: `./gradlew :app:assembleRelease` genera `app/build/outputs/apk/release/app-release.apk`
  firmado con la clave permanente.

- [ ] **Step 1: Comprobar que hoy el release no se firma con la clave.**
  Run: `./gradlew :app:assembleRelease` sin las variables de entorno.
  Expected: hoy genera `app-release-unsigned.apk`. Este es el estado "RED".
- [ ] **Step 2: Implementar en `app/build.gradle.kts`.**
  - `signingConfigs.create("sideload")`, que solo se rellena si existen las dos variables: decodifica
    el base64 en `layout.buildDirectory.file("signing/sideload.p12")`, con `storeType = "PKCS12"`
    y alias `morseprende`.
  - `buildTypes.release.signingConfig = signingConfigs["sideload"]` solo si existen las variables.
  - `tasks.matching { it.name == "assembleRelease" || it.name == "packageRelease" }` hace
    `doFirst` y lanza `GradleException("Faltan MORSEPRENDE_KEYSTORE_B64 / MORSEPRENDE_KEYSTORE_PASSWORD: no se puede firmar el APK de entrega")`
    si faltan.
  - `versionCode = 2` y `versionName = "0.2.0"`.
- [ ] **Step 3: Verificar el fallo claro.** Run: `env -u MORSEPRENDE_KEYSTORE_B64 ./gradlew :app:assembleRelease`. Expected: FAIL con el mensaje anterior.
- [ ] **Step 4: Verificar la firma.** Exporta las variables desde el almacén de esta sesión y ejecuta
  `./gradlew :app:assembleRelease && scripts/print-signing-fingerprint.sh app/build/outputs/apk/release/app-release.apk`.
  Expected: un SHA-256 igual al de `keytool -list -v` del almacén, y `apksigner verify` correcto.
- [ ] **Step 5: Comprobar el repositorio.** Run: `git status --porcelain | grep -E 'p12|signing'`. Expected: vacío.
- [ ] **Step 6: Commit.** `git commit -am "Firmar el APK de entrega con la clave permanente desde variables de entorno"`

---

### Task 2: Decodificador del manipulador

**Files:**
- Create: `morse-core/src/main/kotlin/io/github/egt17baa/morseprende/core/KeyerDecoder.kt`
- Test: `morse-core/src/test/kotlin/io/github/egt17baa/morseprende/core/KeyerDecoderTest.kt`

**Interfaces:**
- Consumes: `MorseAlphabet.charFor`
- Produces:
  ```kotlin
  sealed interface KeyerEvent {
      data class LetterEnded(val code: String, val char: Char?) : KeyerEvent   // char null si el código no existe
      data object WordEnded : KeyerEvent
  }
  class KeyerDecoder(initialUnitMs: Long) {   // la UI pasa 1200 / effectiveWpm de los ajustes
      val unitMs: Long                    // estimación actual, redondeada
      val pending: String                 // elementos de la letra en curso, p. ej. ".-"
      fun press(atMs: Long)
      fun release(atMs: Long): Char?      // '.', '-' o null si fue un rebote (< 15 ms)
      fun poll(nowMs: Long): KeyerEvent?  // cierra la letra o la palabra según el silencio
      fun appendElement(element: Char)    // modo dos botones
      fun commitLetter(): KeyerEvent.LetterEnded?
      fun reset()
      companion object { const val MIN_UNIT_MS = 40L; const val MAX_UNIT_MS = 600L; const val DEBOUNCE_MS = 15L }
  }
  ```

- [ ] **Step 1: Escribir las pruebas que fallan** (unidad inicial de 100 ms):
  - `dot_and_dash_classification`: press 0, release 80 → `'.'`; press 200, release 500 → `'-'`.
  - `letter_ends_after_2_5_units`: tras lo anterior, `poll(700)` da `null`, porque el silencio es
    de 200 ms y el umbral, con la unidad adaptada, es de unos 240 ms. `poll(760)` da
    `LetterEnded(".-", 'A')` y `pending` queda vacío.
  - `unit_adapts`: tras punto de 80 ms la unidad es 95 (0,75·100 + 0,25·80). Tras raya de 300 ms
    es 96, porque la estimación es 300/3 = 100 y se calcula 0,75·95 + 0,25·100 = 96,25.
  - `word_ends_after_6_units`: tras el `LetterEnded`, `poll(1100)` da `WordEnded`, porque
    500 + 6·96 ≤ 1100. Un segundo `poll` da `null`.
  - `bounce_ignored`: press 0, release 10 → `null` y `pending == ""`.
  - `unit_is_clamped`: con inicial de 100 y 20 puntos de 10 000 ms, `unitMs == 600`. La pulsación
    muy larga es raya y la estimación se limita.
  - `slow_beginner_is_followed` (Review Focus 1): con la unidad inicial de la velocidad efectiva
    (10 PPM = 120 ms), un principiante hace punto de 160 y raya de 480. Se clasifican como `.` y
    `-`, y la unidad sube a 128 tras el punto (0,75·120 + 0,25·160 = 130 al redondear). Después,
    una pausa de 300 ms dentro de la letra (< 2,5·u ≈ 325) **no** la corta.
  - `unknown_code_reports_null_char`: `........` cerrado da `LetterEnded("........", null)`.
  - `two_button_mode`: `appendElement('-')`, `appendElement('-')` y `commitLetter()` dan
    `LetterEnded("--", 'M')`. `commitLetter()` sin elementos da `null`.
  - `reset_clears`: `reset()` deja `pending` vacío y `poll` da `null`.
- [ ] **Step 2: Ejecutar.** Run: `./gradlew :morse-core:test`. Expected: FAIL (referencia sin resolver).
- [ ] **Step 3: Implementar.** La unidad se guarda internamente como `Double`. La estimación es
  `duración` para un punto y `duración / 3` para una raya. `poll` usa el tiempo desde el último
  `release`, y la palabra solo se emite una vez después de una letra.
- [ ] **Step 4: Ejecutar.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir decodificador adaptativo del manipulador"`

---

### Task 3: Módulo `:game`, modelo de contenido, parser y validador

**Files:**
- Create: `game/build.gradle.kts` (kotlin jvm, plugin de serialización, `implementation(project(":morse-core"))`, json 1.9.0, junit, kotlin-test, jvmTarget 17 como `:morse-core`)
- Modify: `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts` (`implementation(project(":game"))`)
- Create: `game/.../content/ContentModel.kt`, `ContentParser.kt`, `ContentValidator.kt`
- Test: `game/src/test/.../content/ContentParserTest.kt`, `ContentValidatorTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Serializable enum class MissionType { LESSON, REVIEW, FINALE }
  @Serializable data class StoryMessage(val from: String, val text: String)
  @Serializable data class Mission(
      val id: String, val type: MissionType, val title: String, val briefing: String,
      val newChars: List<Char> = emptyList(), val finalMessage: StoryMessage, val debriefing: String,
  )
  @Serializable data class Chapter(val id: Int, val title: String, val intro: String, val words: List<String>, val missions: List<Mission>)
  @Serializable data class LibraryCard(
      val id: String, val title: String, val body: String, val didYouKnow: String,
      val unlockedBy: String? = null,   // id de misión; null = disponible desde el principio
      val icon: String,                 // "lighthouse" | "telegraph" | "ship" | "radio" | "book" | "wave"
  )
  object ContentParser {
      fun chapter(json: String): Chapter
      fun libraryCards(json: String): List<LibraryCard>   // el JSON es una lista
  }
  object ContentValidator {
      fun learnedCharsUpTo(chapters: List<Chapter>, missionId: String): Set<Char>
      fun validate(chapters: List<Chapter>, cards: List<LibraryCard>): List<String>   // vacío = correcto
  }
  ```
  `ContentParser` usa `Json { ignoreUnknownKeys = false }`. Así una clave mal escrita en el JSON
  falla en vez de pasar desapercibida.

- [ ] **Step 1: Escribir las pruebas que fallan**
  - `parses_chapter`: un JSON mínimo con 1 misión da sus campos, con `newChars == listOf('S','O')`.
  - `rejects_unknown_key`: el JSON con `"tittle"` lanza `SerializationException`.
  - `parses_cards`: una lista de 2 fichas; `unlockedBy` es null si falta.
  - Pruebas del validador, cada una construida a mano y con el error esperado contenido en la lista:
    - `duplicate_mission_ids`;
    - `lesson_needs_1_to_3_new_chars`;
    - `new_char_must_exist_in_alphabet` (por ejemplo `'#'`);
    - `final_message_uses_only_learned_chars` (por ejemplo "SOL" en la misión 1 da un error que
      menciona `L`);
    - `review_cannot_add_new_chars`;
    - `card_unlock_must_reference_mission`;
    - `card_body_150_to_300_words`;
    - `duplicate_card_ids`.
  - `learned_chars_accumulate`: en un capítulo con las misiones S O / repaso / E T, el resultado
    para la tercera es `{S,O,E,T}`.
  - `valid_content_has_no_errors`: un contenido coherente da una lista vacía.
- [ ] **Step 2: Ejecutar.** Run: `./gradlew :game:test`. Expected: FAIL.
- [ ] **Step 3: Implementar.** El validador comprueba los caracteres del `finalMessage` con
  `MorseCodec.normalizeText`, ignorando los espacios. Las palabras del banco solo cuentan para la
  coherencia del alfabeto: cualquier carácter que no esté en `MorseAlphabet.codes` es un error.
- [ ] **Step 4: Ejecutar.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir módulo game con modelo, parser y validador de contenido"`

---

### Task 4: Contenido del capítulo 1

**Files:**
- Create: `app/src/main/assets/content/es/chapter1.json`
- Test: `game/src/test/.../content/RealContentTest.kt` (lee el fichero real con `File("../app/src/main/assets/content/es/chapter1.json")`)

**Contenido decidido** (el constructor escribe los textos, respetando spec §7.1, tono cálido e intriga):

| # | id | tipo | título | letras nuevas | mensaje final (texto) |
|---|---|---|---|---|---|
| 1 | c1m1 | LESSON | La primera luz | S O | `SOS` |
| 2 | c1m2 | LESSON | Niebla en la bocana | E T | Libre; solo letras S O E T (p. ej. `ESTO ES SOS`) |
| 3 | c1m3 | LESSON | El pesquero Gaviota | A N | Libre; S O E T A N |
| 4 | c1r1 | REVIEW | Guardia de noche | — | Libre; S O E T A N |
| 5 | c1m4 | LESSON | La tormenta | I M | Libre; + I M |
| 6 | c1m5 | LESSON | Rescate en el arrecife | R D | Libre; + R D |
| 7 | c1r2 | REVIEW | El cuaderno del farero | — | Libre; las 10 letras |
| 8 | c1f | FINALE | La señal bajo el faro | — | Mensaje largo (≥ 25 letras) que anuncia la Operación Sombra |

- `words`: al menos 30 palabras en español con esas 10 letras (MAR, RED, DATOS, RADIO, ORDEN,
  TIMON, NOTA, MESA…), sin tildes y en mayúsculas.
- `briefing` y `debriefing`: 2–4 frases cada uno, en segunda persona. Los dicen personajes propios:
  la vieja farera Remedios, que es la mentora, y los marineros del Gaviota.

- [ ] **Step 1: Escribir la prueba que falla.** `real_chapter1_is_valid`: parsea el fichero real,
  `ContentValidator.validate(listOf(ch), emptyList())` da una lista vacía, el orden de `newChars`
  de las lecciones es exactamente `[[S,O],[E,T],[A,N],[I,M],[R,D]]`, hay 8 misiones con los ids de
  la tabla y `words.size >= 30`.
- [ ] **Step 2: Ejecutar.** Expected: FAIL (el fichero no existe).
- [ ] **Step 3: Escribir `chapter1.json`.**
- [ ] **Step 4: Ejecutar.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir contenido narrativo del capítulo 1: El Faro"`

---

### Task 5: Generador de ejercicios

**Files:**
- Create: `game/.../mission/Exercise.kt`, `MissionPlanner.kt`
- Test: `game/src/test/.../mission/MissionPlannerTest.kt`

**Interfaces:**
- Consumes: `Chapter`, `Mission`, `ContentValidator.learnedCharsUpTo`, `MorseCodec`
- Produces:
  ```kotlin
  sealed interface Exercise {
      data class Listen(val chars: List<Char>) : Exercise
      data class ReceiveChoice(val answer: String, val options: List<String>) : Exercise
      data class ReceiveTyped(val answer: String) : Exercise
      data class Send(val target: String) : Exercise
      data class FinalMessage(val message: StoryMessage) : Exercise
  }
  object MissionPlanner { fun plan(mission: Mission, chapter: Chapter, random: Random): List<Exercise> }
  ```
- **Estructura** (se ordena por fases: Listen → ReceiveChoice → ReceiveTyped → Send → FinalMessage):
  - **LESSON:**
    - `Listen(newChars)`.
    - 4 `ReceiveChoice`: cada letra nueva aparece al menos una vez como respuesta; el resto son
      palabras del banco que contengan alguna letra nueva, si las hay.
    - 4 `ReceiveTyped`: 2 letras y 2 palabras.
    - `Send` de cada letra nueva y 2 palabras de 2–4 letras.
    - `FinalMessage`.
  - **REVIEW:** 5 `ReceiveChoice`, 5 `ReceiveTyped`, 4 `Send` (2 letras y 2 palabras) y `FinalMessage`.
  - **FINALE:** 4 `ReceiveTyped` (palabras), 2 `Send` (palabras) y `FinalMessage`.
  - **Opciones:** respuesta + hasta 3 distractores del **mismo tipo** (letra con letras, palabra con
    palabras), todos aprendidos y distintos, en orden aleatorio. Si hay menos candidatos, menos
    opciones, nunca menos de 2. En c1m1, con S y O, las opciones de una letra son `{S, O}`.
  - Si el banco no tiene palabras válidas, se usan combinaciones de 2–3 letras aprendidas (por
    ejemplo `SO`, `OS`).

- [ ] **Step 1: Escribir las pruebas que fallan** (con el `chapter1.json` real y `Random(42)`):
  - `lesson_structure`: para c1m3, los tipos en orden y las cantidades exactas descritas.
  - `only_learned_chars` (para todas las misiones): todas las respuestas, opciones y objetivos usan
    solo los caracteres aprendidos hasta esa misión.
  - `every_new_char_is_asked`: en cada LESSON, cada letra nueva aparece como respuesta de algún
    `ReceiveChoice` y como objetivo de algún `Send`.
  - `options_contain_answer_and_are_distinct`: tienen entre 2 y 4 elementos.
  - `first_mission_letter_options`: en c1m1, toda `ReceiveChoice` de una letra tiene opciones
    `{"S","O"}`.
  - `deterministic_with_seed`: dos llamadas con `Random(7)` dan listas iguales.
  - `review_and_finale_structure`: cantidades de c1r1 y c1f.
- [ ] **Step 2: Ejecutar.** Expected: FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Ejecutar.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir generador de ejercicios de misión"`

---

### Task 6: Sesión de misión y puntuación

**Files:**
- Create: `game/.../mission/MissionSession.kt`
- Test: `game/src/test/.../mission/MissionSessionTest.kt`

**Interfaces:**
- Consumes: `Exercise`, `MorseCodec.normalizeText`
- Produces:
  ```kotlin
  data class AnswerResult(val correct: Boolean, val expected: String, val advanced: Boolean, val attemptsLeft: Int)
  data class MissionResult(val correct: Int, val total: Int, val stars: Int) { val accuracy: Double }
  class MissionSession(val exercises: List<Exercise>) {
      val index: Int; val current: Exercise?; val isFinished: Boolean
      val hintLevel: Int                         // solo en FinalMessage: sube 1 cada 2 fallos (máx. 2)
      fun continueListen()                       // Listen → siguiente
      fun answer(response: String): AnswerResult // ReceiveChoice, ReceiveTyped y FinalMessage
      fun send(decoded: String): AnswerResult    // Send
      fun result(): MissionResult                // requiere isFinished
      companion object {
          const val SEND_ATTEMPTS = 3
          fun stars(accuracy: Double): Int       // ≥0,90→3, ≥0,70→2, si no 1
          fun matches(response: String, expected: String): Boolean  // normaliza mayúsculas, tildes y espacios
      }
  }
  ```
- **Reglas:**
  - Recibir: un solo intento. Se avanza siempre y cuenta si se acierta.
  - Enviar: hasta 3 intentos. Puntúa si se acierta **al primer intento**. Se avanza al acertar o
    tras el tercer fallo.
  - Mensaje final: no puntúa; solo se avanza al acertar. Tiene intentos ilimitados y pistas.
  - `total` = número de ejercicios de recibir + enviar.
  - Llamar a un método que no corresponde al ejercicio actual lanza `IllegalStateException`.

- [ ] **Step 1: Escribir las pruebas que fallan**
  - `matches_is_lenient` (Review Focus 3): `matches("  hola  mundo ", "HOLA MUNDO")`,
    `matches("canción", "CANCION")` y `matches("ñ", "Ñ")` son verdaderos; `matches("hola", "HOLAS")`
    es falso.
  - `receive_one_attempt_always_advances`.
  - `send_three_attempts`: fallo, fallo y acierto avanzan sin puntuar; un acierto al primer
    intento puntúa.
  - `send_advances_after_third_failure`: `attemptsLeft` pasa por 2, 1 y 0.
  - `final_message_needs_correct_answer`: dos fallos dan `hintLevel == 1`, cuatro dan `2` y el
    acierto termina la sesión.
  - `stars_thresholds`: `stars(0.9) == 3`, `stars(0.89) == 2`, `stars(0.7) == 2` y `stars(0.69) == 1`.
  - `result_counts`: un plan con 2 recibir y 2 enviar, con 3 aciertos, da 3/4 = 0,75 y 2 estrellas.
  - `wrong_method_throws`: `send()` sobre un `ReceiveTyped` lanza excepción.
- [ ] **Step 2: Ejecutar.** Expected: FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Ejecutar.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir sesión de misión con reglas de intentos y estrellas"`

---

### Task 7: Reglas de desbloqueo

**Files:**
- Create: `game/.../progress/ProgressRules.kt`
- Test: `game/src/test/.../progress/ProgressRulesTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object ProgressRules {
      fun isUnlocked(missionId: String, chapters: List<Chapter>, completed: Set<String>): Boolean
      fun nextMission(chapters: List<Chapter>, completed: Set<String>): Mission?   // primera sin completar
      fun unlockedCards(cards: List<LibraryCard>, completed: Set<String>): List<LibraryCard>
      fun chapterProgress(chapter: Chapter, completed: Set<String>): Float           // 0..1
  }
  ```
- [ ] **Step 1: Escribir las pruebas que fallan**
  - `first_mission_always_unlocked`.
  - `next_unlocks_after_previous_completed`.
  - `completing_out_of_order_does_not_skip`: con `{c1m2}` completada pero no `c1m1`, c1m3 sigue
    bloqueada.
  - `next_mission_is_first_incomplete` (y `null` si están todas).
  - `cards_without_unlock_always_visible`, `cards_unlock_with_mission`.
  - `chapter_progress_fraction`: 2 de 8 dan 0,25.
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.** `git commit -am "Añadir reglas de desbloqueo de misiones y fichas"`

---

### Task 8: Progreso en Room y carga del contenido

**Files:**
- Modify: `gradle/libs.versions.toml` (room-runtime, room-ktx, room-compiler, ksp), `build.gradle.kts` (plugin ksp `apply false`), `app/build.gradle.kts` (plugin ksp y dependencias; `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`)
- Create: `app/.../data/progress/MissionProgressEntity.kt`, `ProgressDao.kt`, `AppDatabase.kt`, `ProgressRepository.kt`, `app/.../data/ContentSource.kt`
- Modify: `MorsePrendeApp.kt` (`AppContainer` gana `progress` y `content`)
- Test: `app/src/test/.../data/RoomProgressRepositoryTest.kt` (Robolectric, base de datos en memoria), `AssetContentSourceTest.kt`, y `FakeProgressRepository` y `FakeContentSource` en `src/test`

**Interfaces:**
- Produces:
  ```kotlin
  data class MissionProgress(val missionId: String, val bestStars: Int, val attempts: Int, val lastPlayedAt: Long)
  interface ProgressRepository {
      val progress: Flow<Map<String, MissionProgress>>
      suspend fun recordResult(missionId: String, stars: Int, nowMs: Long)   // guarda max(estrellas), attempts + 1
  }
  class RoomProgressRepository(db: AppDatabase) : ProgressRepository
  interface ContentSource {
      fun chapters(): List<Chapter>          // lanza ContentLoadException si el JSON está roto
      fun libraryCards(): List<LibraryCard>
  }
  class ContentLoadException(message: String, cause: Throwable?) : Exception(message, cause)
  class AssetContentSource(context: Context) : ContentSource   // lee content/es/chapter1.json y library.json, con caché
  ```
  - Tabla `mission_progress`: `missionId` es la clave primaria, más `bestStars`, `attempts` y
    `lastPlayedAt`.
  - La base de datos se llama `morseprende.db`, en versión 1, y el esquema se exporta a `app/schemas/`.

- [ ] **Step 1: Escribir las pruebas que fallan**
  - `records_and_reads`.
  - `keeps_best_stars` (Review Focus 5): 3 estrellas y luego 1 dan `bestStars == 3` y `attempts == 2`.
  - `empty_initially`.
  - `asset_content_loads_chapter1`: con Robolectric, `AssetContentSource(context).chapters()` tiene
    8 misiones.
  - `broken_json_throws_content_load_exception` (Review Focus 4): un `AssetContentSource` con un
    lector inyectado que devuelve `"{"` lanza `ContentLoadException`. **Interfaz:** el constructor
    secundario interno es `AssetContentSource(read: (String) -> String)`.
- [ ] **Step 2: Ejecutar.** Expected: FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Ejecutar.** Run: `./gradlew :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "Añadir progreso con Room y carga de contenido desde assets"`

---

### Task 9: Tono en vivo del manipulador y modo de manipulador

**Files:**
- Create: `app/.../playback/Sidetone.kt`
- Modify: `settings/AppSettings.kt` (+ `keyerMode`), `settings/SettingsRepository.kt`, `settings/SettingsViewModel.kt`, `settings/SettingsScreen.kt`, `strings.xml`
- Test: `app/src/test/.../playback/SidetoneTest.kt` (Robolectric), y se amplían `SettingsRepositoryTest` y `SettingsViewModelTest`

**Interfaces:**
- Produces:
  ```kotlin
  enum class KeyerMode { ONE_BUTTON, TWO_BUTTONS }
  // AppSettings gana: val keyerMode: KeyerMode = KeyerMode.ONE_BUTTON
  // SettingsViewModel gana: fun setKeyerMode(mode: KeyerMode)
  interface Sidetone { fun keyDown(); fun keyUp(); fun close() }
  class AudioSidetone(context: Context, freqHz: Int, volume: Float) : Sidetone
  fun interface SidetoneFactory { fun create(settings: AppSettings): Sidetone }
  ```
  `AudioSidetone`:
  - Un hilo con `AudioTrack` en `MODE_STREAM` y `PERFORMANCE_MODE_LOW_LATENCY`, con el **búfer
    mínimo**.
  - Escribe bloques de 96 muestras de tono o de silencio según un `@Volatile keyIsDown`, con una
    rampa de 3 ms al cambiar. La fase es continua entre bloques.
  - `close()` para el hilo y libera el track. Es idempotente.
  - Si el `AudioTrack` falla, `keyDown` y `keyUp` no hacen nada (sin excepción).
  - Además, `keyDown` lanza `performHapticFeedback` desde la UI, no desde aquí.

- [ ] **Step 1: Escribir las pruebas que fallan**
  - `keyer_mode_persists`: escribir `TWO_BUTTONS` y leerlo con un repositorio nuevo.
  - `set_keyer_mode_updates_repo` (ViewModel).
  - `sidetone_close_is_idempotent_and_safe_after_keys`: `keyDown()`, `keyUp()`, `close()` y
    `close()`, todo con Robolectric y sin excepciones.
  - `sidetone_keys_after_close_do_nothing`.
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
  En Ajustes se añade un selector "Manipulador: Un botón / Dos botones (punto y raya)".
- [ ] **Step 5: Commit.** `git commit -am "Añadir tono en vivo del manipulador y elección de modo"`

---

### Task 10: ViewModel de misión

**Files:**
- Create: `app/.../ui/mission/MissionViewModel.kt`, `MissionUiState.kt`
- Test: `app/src/test/.../ui/mission/MissionViewModelTest.kt`

**Interfaces:**
- Consumes: `ContentSource`, `MissionPlanner`, `MissionSession`, `ProgressRepository`, `SettingsRepository`, `OutputFactory`, `SignalPlayer`, `MorseTiming`, `KeyerDecoder`, `SidetoneFactory`
- Produces:
  ```kotlin
  enum class MissionPhase { BRIEFING, EXERCISE, DEBRIEFING, ERROR }
  data class Feedback(val correct: Boolean, val expected: String)
  data class MissionUiState(
      val phase: MissionPhase = MissionPhase.BRIEFING, val title: String = "", val briefing: String = "",
      val debriefing: String = "", val exercise: Exercise? = null, val exerciseIndex: Int = 0, val exerciseCount: Int = 0,
      val feedback: Feedback? = null, val keyerPending: String = "", val sentText: String = "",
      val keyerMode: KeyerMode = KeyerMode.ONE_BUTTON, val attemptsLeft: Int = MissionSession.SEND_ATTEMPTS,
      val hintLevel: Int = 0, val isPlaying: Boolean = false, val stars: Int = 0, val previousBest: Int = 0,
      val showPattern: Boolean = true,   // ayuda visual: el capítulo 1 enseña el patrón de las letras nuevas
  )
  class MissionViewModel(
      missionId: String, content: ContentSource, progress: ProgressRepository, settings: SettingsRepository,
      outputs: OutputFactory, sidetone: SidetoneFactory, random: Random = Random.Default,
      playerScope: CoroutineScope? = null, clock: () -> Long = { SystemClock.uptimeMillis() },
  ) : ViewModel {
      val state: StateFlow<MissionUiState>
      fun start()                       // BRIEFING → EXERCISE; reproduce automáticamente si el ejercicio es de recibir
      fun replay()                      // vuelve a sonar el ejercicio actual (Morse del objetivo, o del mensaje final)
      fun continueListen()
      fun answer(text: String)          // recibir y mensaje final
      fun keyDown(); fun keyUp()        // manipulador de un botón (usa clock y el tono en vivo)
      fun tick()                        // la UI lo llama cada 20 ms mientras hay un Send; cierra letras y palabras
      fun dot(); fun dash(); fun nextLetter()   // modo dos botones
      fun deleteLast(); fun submitSend()
      fun next()                        // quita el feedback y pasa al siguiente ejercicio o a DEBRIEFING
      fun abandon()                     // detiene el sonido y el tono en vivo; no guarda nada
  }
  ```
  - El progreso se guarda **solo** al llegar a `DEBRIEFING`, con `progress.recordResult`.
  - Un contenido roto (`ContentLoadException`) o un `missionId` inexistente llevan a `ERROR`.
  - `sentText`: cada `LetterEnded` añade su carácter, o `?` si el código no existe. `WordEnded`
    añade un espacio, una sola vez.
  - La reproducción de los ejercicios usa solo `SOUND`, con la velocidad de los ajustes.

- [ ] **Step 1: Escribir las pruebas que fallan.** Se usan `FakeContentSource` con un capítulo
  pequeño, `FakeProgressRepository`, `FakeSettingsRepository`, `FakeOutputFactory` (de la Etapa 1),
  un `FakeSidetone` que registra las llamadas, `Random(1)` y un reloj manual.
  - `starts_in_briefing_then_exercise`.
  - `receive_answer_gives_feedback_then_next`.
  - `one_button_keying_builds_text`: con la unidad de 100 ms del ajuste a 12 PPM, se hace
    keyDown/keyUp de 3 puntos y `tick()` tras 300 ms de silencio. Resultado: `sentText == "S"` y el
    sidetone registra `down, up` tres veces.
  - `two_button_keying_builds_text`: `dot()` × 3, `nextLetter()`, `dash()` × 3 y `nextLetter()` dan
    `"SO"`.
  - `delete_last_removes_char`.
  - `submit_send_scores`: con el objetivo `S`, enviar `S` da feedback correcto.
  - `final_message_hint_level_increases`.
  - `completing_records_progress_once`: recorrer todos los ejercicios lleva a DEBRIEFING, una sola
    llamada a `recordResult` con las estrellas calculadas y `previousBest` correcto.
  - `abandon_does_not_record_and_stops_audio` (Review Focus 2): abandonar a mitad no hace ninguna
    llamada, `isPlaying` es falso y el sidetone queda cerrado (`close` llamado).
  - `broken_content_goes_to_error` (Review Focus 4).
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.** `git commit -am "Añadir lógica de misión: recibir, enviar con manipulador y mensaje final"`

---

### Task 11: Pantalla de misión

**Files:**
- Create: `app/.../ui/mission/MissionScreen.kt`, `KeyerPad.kt`, `MissionPanels.kt`
- Modify: `AppNavHost.kt` (ruta `mission/{id}`), `strings.xml`
- Test: `app/src/test/.../ui/mission/MissionScreenTest.kt` (Compose y Robolectric)

**Diseño** (propio y distinto de la referencia):
- **Cabecera:** título de la misión y una barra de progreso con forma de rayo de luz.
- **Briefing y debriefing:** tarjeta tipo "cuaderno de bitácora" con quien habla en coral, el texto
  y un botón grande. En el debriefing, las estrellas se dibujan como 1–3 destellos de faro y se
  indica si es "¡Nueva mejor marca!".
- **Listen:** cada letra grande, su patrón con `MorseDisplay.pretty` y el botón "Escuchar".
- **Recibir:** botón "Escuchar otra vez", y opciones como botones grandes (o un campo de texto en
  los de escribir). Tras responder, el feedback con el correcto y "Continuar".
- **Enviar:**
  - Objetivo grande, con su patrón si `showPattern` y la letra es nueva.
  - Patrón en curso (`keyerPending`) y texto enviado.
  - `KeyerPad`: en un botón, un manipulador circular grande de 160 dp con `pointerInput`
    (`awaitPointerEvent`) que llama a keyDown/keyUp con háptica. En dos botones, Punto | Raya y
    debajo "Siguiente letra".
  - Botones "Borrar" y "Enviar". Intentos restantes.
  - `LaunchedEffect` con bucle de `tick()` cada 20 ms.
- **Mensaje final:** el remitente y "Mensaje entrante…", reproducir, campo de texto y pistas: con
  nivel 1 se muestra el número de letras con guiones; con nivel 2, el Morse con `MorseDisplay`.
- **Salir:** `DisposableEffect` y `LifecycleEventEffect(ON_STOP)` llaman a `abandon()`. Mientras
  hay un Send o una reproducción, `keepScreenOn` está activo.
- **Error:** el mensaje `R.string.mission_error` y un botón para volver.

- [ ] **Step 1: Escribir las pruebas que fallan** (con fakes):
  - `briefing_shows_and_starts`.
  - `choice_options_are_clickable`.
  - `two_button_pad_shown_when_mode_two`.
  - `error_state_shows_message`.
  - `leaving_screen_abandons` (Review Focus 2): al quitar el contenido, el `FakeSidetone` queda
    cerrado.
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.** `git commit -am "Añadir pantalla de misión con manipulador y fases"`

---

### Task 12: El Faro (mapa de la campaña)

**Files:**
- Create: `app/.../ui/hub/HubScreen.kt`, `HubViewModel.kt`
- Delete: `app/.../ui/home/HomeScreen.kt` y su test (sustituidos)
- Modify: `AppNavHost.kt` (inicio en `HUB`; rutas `library`, `translator`, `settings` y `mission/{id}`), `strings.xml`
- Test: `app/src/test/.../ui/hub/HubViewModelTest.kt`, `HubScreenTest.kt`, y se actualiza `AppNavHostTest`

**Interfaces:**
- Produces:
  ```kotlin
  data class MissionNode(val id: String, val title: String, val type: MissionType, val stars: Int, val unlocked: Boolean, val isNext: Boolean)
  data class HubUiState(val chapterTitle: String = "", val nodes: List<MissionNode> = emptyList(), val glow: Float = 0f,
                        val error: Boolean = false, val newCards: Int = 0)
  class HubViewModel(content: ContentSource, progress: ProgressRepository) : ViewModel { val state: StateFlow<HubUiState> }
  ```
  - `glow` = `chapterProgress` y modula el brillo y el alcance del haz del faro (de 0,3 a 1).
  - `newCards` = fichas desbloqueadas sin abrir. Se guarda el conjunto de fichas vistas en
    DataStore, con la clave `seen_cards`, en un pequeño `SeenCardsRepository` con interfaz y fake.
- **Diseño:**
  - Arriba, el faro animado de la Etapa 1, con el brillo según `glow`.
  - Debajo, un camino serpenteante dibujado con `Canvas`, con los nodos de las misiones: círculo
    crema si está desbloqueada, gris con candado si no; anillo coral en la siguiente; estrellas
    bajo cada nodo; las misiones de repaso como un rombo y la final como un faro pequeño.
  - Tarjetas bloqueadas "Operación Sombra" y "Señal Estelar" con "Próximamente".
  - Barra inferior: Biblioteca (con insignia si `newCards > 0`), Traductor y Ajustes.
- [ ] **Step 1: Escribir las pruebas que fallan**
  - `nodes_reflect_progress`: con c1m1 a 3 estrellas, el nodo 1 tiene 3 estrellas, el 2 está
    desbloqueado y marcado `isNext`, y el 3 está bloqueado.
  - `glow_is_chapter_progress`.
  - `content_error_sets_error`.
  - `tapping_unlocked_node_navigates`.
  - `locked_node_not_clickable`.
  - `app_starts_on_hub` (en `AppNavHostTest`).
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.** `git commit -am "Añadir el Faro: mapa de la campaña con progreso y brillo del faro"`

---

### Task 13: Biblioteca del faro

**Files:**
- Create: `app/src/main/assets/content/es/library.json`, `docs/biblioteca/fuentes.md`, `app/.../ui/library/LibraryScreen.kt`, `LibraryViewModel.kt`
- Test: se amplía `RealContentTest` (valida `library.json` junto al capítulo), `app/src/test/.../ui/library/LibraryViewModelTest.kt`

**Fichas de esta etapa** (7 en total):

| id | título | desbloqueo |
|---|---|---|
| lib-que-es | Qué es el código Morse | inicio |
| lib-morse-vail | Samuel Morse y Alfred Vail | inicio |
| lib-aprender | Cómo se aprende Morse: Koch y Farnsworth | inicio |
| lib-1844 | El primer mensaje oficial (1844) | c1m2 |
| lib-americano | Morse americano e internacional | c1m3 |
| lib-sos | SOS, el Titanic y la llamada de socorro | c1m5 |
| lib-mar | El Morse en el mar y su retirada (GMDSS, 1999) | c1f |

- [ ] **Step 1: Investigar.** Con búsqueda web se comprueba cada dato: fechas, nombres, cifras y
  citas. En `docs/biblioteca/fuentes.md` se apunta cada afirmación con al menos una fuente fiable
  (Britannica, Smithsonian, Library of Congress, UIT, IMO, ARRL o URE), con su URL y fecha de
  consulta. Lo que no se pueda verificar no se incluye.
- [ ] **Step 2: Escribir la prueba que falla.** `real_library_is_valid`: validar el capítulo junto
  con `library.json` no da errores, hay 7 fichas con los ids de la tabla y cada `body` tiene entre
  150 y 300 palabras.
- [ ] **Step 3: Escribir `library.json`.** Ejecutar la prueba. Expected: PASS.
- [ ] **Step 4: Pruebas del ViewModel, que fallan:**
  - `locked_cards_show_unlock_mission_title`.
  - `opening_card_marks_seen`.
  - `unlocked_after_mission`.

  Después se implementan `LibraryViewModel` y `LibraryScreen`:
  - una lista de tarjetas con icono vectorial propio según `icon` (dibujado con `Canvas`);
  - las bloqueadas, con candado y "Se desbloquea al completar: «…»";
  - el detalle, con título, cuerpo y un recuadro coral "¿Sabías que…?".
- [ ] **Step 5: Ejecutar.** Run: `./gradlew :game:test :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 6: Commit.** `git commit -am "Añadir biblioteca del faro con fichas históricas verificadas"`

---

### Task 14: APK de la Etapa 2 y entrega

**Files:**
- Create: `docs/pruebas/etapa-2.md`
- Modify: `README.md` (estado de la Etapa 2, firma por variables de entorno y cómo ejecutar `assembleRelease`)

- [ ] **Step 1: Suite completa.** Run: `./gradlew :morse-core:test :game:test :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`. Expected: `BUILD SUCCESSFUL` y lint con 0 errores.
- [ ] **Step 2: Permisos y firma.** `aapt2 dump permissions` sin `INTERNET`, y `scripts/print-signing-fingerprint.sh` con la huella de la clave permanente.
- [ ] **Step 3: Capturas.** Con una prueba temporal de Robolectric en modo gráfico nativo, que no
  se sube al repositorio, se capturan el Faro, una misión de envío y la biblioteca, y se revisan.
  Si algo desentona, se corrige con su prueba.
- [ ] **Step 4: Escribir `docs/pruebas/etapa-2.md`.** Comprobaciones:
  - **Desinstalar la Etapa 1 una sola vez** (cambia la clave) e instalar.
  - Recorrer c1m1–c1m3.
  - Manipulador en ambos modos y retardo del tono al pulsar.
  - Salir a mitad de una misión.
  - Progreso tras cerrar y abrir la app.
  - Biblioteca: leer y revisar las fichas, y enviar correcciones.
  - Comprobar que una actualización futura se instala encima.
- [ ] **Step 5: Commit y push.** `git push -u origin ccr-675a91da-h876sj`
- [ ] **Step 6: Entregar** `dist/morseprende-etapa2.apk` (release firmado) y la lista de pruebas con `SendUserFile`.
