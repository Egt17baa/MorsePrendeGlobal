# Etapa 3a: repaso inteligente y minijuegos. Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:**
- Añadir el **repaso inteligente** (spec §7.6): repetición espaciada por carácter con 5 cajas de
  Leitner. Las letras que más fallas aparecen más a menudo.
- Añadir la **práctica libre** con los tres minijuegos de la spec §7.5 (Tormenta de señales,
  Destello rápido e Interceptación), con récords locales.
- Todo sin perder el progreso ya guardado.

**Architecture:**
- La lógica va en Kotlin puro dentro de `:game`: cajas de Leitner, resultados por carácter, motores
  de los minijuegos con reloj inyectado y reglas de desbloqueo.
- `:app` añade:
  - las tablas Room `char_stats` y `game_records` mediante migración automática 1→2, sin perder
    `mission_progress`;
  - una pantalla **Práctica** desde el Faro;
  - una pantalla por minijuego.
- Las misiones registran los resultados por carácter, y los repasos y minijuegos eligen las letras
  según su caja.

**Tech Stack:** lo de la Etapa 2, más `androidx.room:room-testing` para probar la migración.

**Spec:** `docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md` (§7.5 y §7.6).

**Etapa 3b** (otro plan): Destellos, tienda estética, logros y racha (§7.7).

## Global Constraints

- Las de las Etapas 1 y 2: applicationId, sin `INTERNET`, textos en `strings.xml`, límite de
  destellos y APK de entrega firmado con la clave permanente.
- `versionCode 4`, `versionName "0.3.0"`.
- **Sin expresiones regulares no compatibles con Android.** Lo impide
  `AndroidRegexCompatibilityTest`.
- **La migración de la base de datos de la versión 1 a la 2 conserva `mission_progress`.** Lo
  comprueba un test.
- **Leitner:**
  - Las cajas van de 1 a 5 y un carácter nuevo empieza en la 1.
  - Acierto: sube una caja (como máximo la 5). Fallo: vuelve a la caja 1.
  - Peso para elegir = `6 − caja` (de 5 a 1).
- **Desbloqueo de minijuegos** (spec §7.5, "al avanzar"):
  - Tormenta, al completar `c1m2`.
  - Destello rápido, al completar `c1m3`.
  - Interceptación, al completar `c1r1`.
  - Solo usan las letras aprendidas en misiones completadas.
- En Destello rápido, el faro dibujado parpadea en una zona pequeña de la pantalla. Aun así, con
  **limitar destellos** activado, la velocidad se limita a 7 PPM.

## Review Focus

1. **Actualizar la app con progreso guardado.** Se espera que la migración no pierda las misiones
   completadas ni las estrellas. Lo prueba la Task 3.
2. **Pulsaciones muy rápidas o repetidas en los minijuegos** (doble toque en una letra, responder
   después de terminar la partida). Se espera que no haya doble puntuación ni fallos. Lo prueban
   las Tasks 5 a 7.
3. **Salir de un minijuego a mitad** (atrás o botón de inicio). Se espera que se detengan el sonido
   y el bucle de la partida y que no se guarde un récord parcial como si fuera completo. Lo prueba
   la Task 10.
4. **Un minijuego con muy pocas letras aprendidas** (por ejemplo, solo S, O, E y T). Se espera que
   funcione sin colgarse y con opciones válidas. Lo prueban las Tasks 5 a 7.
5. **Mensaje o palabra con espacios o minúsculas en Interceptación.** Se espera que se acepten como
   en las misiones. Lo prueba la Task 7.

---

### Task 1: Cajas de Leitner y resultados por carácter

**Files:**
- Create: `game/.../review/Leitner.kt`
- Test: `game/src/test/.../review/LeitnerTest.kt`

**Interfaces:**
```kotlin
data class CharOutcome(val char: Char, val correct: Boolean)
object Leitner {
    const val MIN_BOX = 1; const val MAX_BOX = 5
    fun next(box: Int, correct: Boolean): Int               // acierto +1 (máx. 5), fallo 1
    fun apply(boxes: Map<Char, Int>, outcomes: List<CharOutcome>): Map<Char, Int>
    fun weight(box: Int): Int                                // 6 − caja
    fun pick(chars: Collection<Char>, boxes: Map<Char, Int>, random: Random): Char   // ponderado; sin caja = caja 1
    /** Resultado por posición: cada carácter del esperado acierta si coincide en su posición (normalizado). */
    fun outcomesFor(expected: String, response: String): List<CharOutcome>
}
```
- [ ] **Step 1: Escribir las pruebas que fallan**
  - `next_rules`: `next(1,true) == 2`, `next(5,true) == 5`, `next(4,false) == 1`.
  - `apply_accumulates`: con S en la caja 2, un acierto y luego un fallo dejan S en la 1; un carácter
    nuevo con acierto queda en la 2.
  - `weights`: `weight(1) == 5` y `weight(5) == 1`.
  - `pick_prefers_low_boxes`: con S en la caja 1 y O en la 5, de 6000 extracciones con `Random(1)`,
    S sale entre el 78 % y el 88 % (5/6 ≈ 83 %).
  - `pick_single_char`.
  - `outcomes_by_position`: `outcomesFor("SOS", "sos")` son tres aciertos; `outcomesFor("SOS", "SOT")`
    da S y O bien y S mal; `outcomesFor("SOS", "")` son tres fallos. Los espacios del esperado se
    ignoran y la entrada se normaliza con `MorseCodec.normalizeText`.
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.** `git commit -am "Añadir cajas de Leitner y resultados por carácter"`

### Task 2: Resultados por carácter de las misiones y repasos ponderados

**Files:**
- Modify: `game/.../mission/MissionSession.kt` (+ `outcomes: List<CharOutcome>`), `game/.../mission/MissionPlanner.kt` (+ parámetro `boxes`)
- Test: se amplían `MissionSessionTest` y `MissionPlannerTest`

**Interfaces:**
- `MissionSession.outcomes`: resultados de cada ejercicio de **recibir** y del **primer intento**
  de cada envío, calculados con `Leitner.outcomesFor`. El mensaje final no cuenta.
- `MissionPlanner.plan(mission, chapter, random, boxes: Map<Char, Int> = emptyMap())`. En los
  **REVIEW**, las letras sueltas se eligen con `Leitner.pick`. Las palabras prefieren las que
  contienen la letra más débil, es decir, la de caja más baja.
- [ ] **Step 1: Escribir las pruebas que fallan**
  - `outcomes_record_receive_and_first_send`: para `ReceiveTyped("SO")`, la respuesta "SE" da
    S bien y O mal. Para `Send("S")`, fallo y luego acierto dan un único S mal, el del primer intento.
  - `review_prefers_weak_letters`: en c1r1, con A en la caja 1 y el resto en la 5, en 200 planes con
    semillas distintas al menos el 40 % de las letras sueltas son A. Sin pesos, ninguna letra pasa
    del 35 %.
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.**

### Task 3: Base de datos v2: `char_stats` y `game_records` sin perder el progreso

**Files:**
- Modify: `data/progress/AppDatabase.kt` (versión 2, `autoMigrations = [AutoMigration(1, 2)]`), `gradle/libs.versions.toml` y `app/build.gradle.kts` (room-testing; `sourceSets["test"].assets.srcDir("$projectDir/schemas")`)
- Create: `data/review/CharStatsEntity.kt`, `CharStatsDao.kt`, `CharStatsRepository.kt`; `data/games/GameRecordEntity.kt`, `GameRecordDao.kt`, `GameRecordsRepository.kt`
- Test: `app/src/test/.../data/MigrationTest.kt`, `CharStatsRepositoryTest.kt`, `GameRecordsRepositoryTest.kt`, y los fakes de los dos repositorios

**Interfaces:**
```kotlin
interface CharStatsRepository {
    val boxes: Flow<Map<Char, Int>>
    suspend fun record(outcomes: List<CharOutcome>)   // aplica Leitner y suma aciertos y fallos
}
enum class GameId { STORM, FLASH, INTERCEPT }
data class GameRecord(val game: GameId, val bestScore: Int, val bestExtra: Int, val plays: Int)
interface GameRecordsRepository {
    val records: Flow<Map<GameId, GameRecord>>
    /** Guarda la partida; devuelve true si es nuevo récord de puntuación. bestExtra: máx. PPM (Interceptación) o mejor tiempo medio en ms (Destello, menor es mejor). */
    suspend fun recordGame(game: GameId, score: Int, extra: Int): Boolean
}
```
- [ ] **Step 1: Escribir las pruebas que fallan**
  - `migration_1_to_2_keeps_progress` (Review Focus 1): con `MigrationTestHelper` y el esquema
    `1.json` exportado, se crea la v1 con una fila de `mission_progress` (c1m1, 3 estrellas), se
    migra a la v2 y la fila sigue igual. Además las tablas nuevas existen.
  - `char_stats_apply_leitner`.
  - `game_record_keeps_best_and_counts_plays`: puntuaciones 5, 3 y 8 dan `bestScore == 8` y
    `plays == 3`, y devuelven `true` para 5, `false` para 3 y `true` para 8.
  - `flash_extra_lower_is_better`: para FLASH, `bestExtra` guarda el menor valor mayor que 0.
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS). Comprobar que se genera `app/schemas/.../2.json`.
- [ ] **Step 5: Commit.**

### Task 4: Las misiones alimentan el repaso inteligente

**Files:**
- Modify: `ui/mission/MissionViewModel.kt` (consume `CharStatsRepository`: pesos al planificar y guardado de `session.outcomes` al terminar), `MorsePrendeApp.kt`, `AppNavHost.kt`
- Test: se amplía `MissionViewModelTest`

- [ ] **Step 1: Escribir las pruebas que fallan**
  - `completing_records_char_outcomes`: al terminar, `FakeCharStatsRepository.recorded` contiene los
    resultados de la sesión una sola vez.
  - `abandon_records_nothing` (no se registran resultados).
  - `review_uses_boxes`: el planificador recibe las cajas actuales. Se verifica con una misión REVIEW
    y A en la caja 1: una mayoría de letras sueltas son A, con la semilla fija.
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.**

### Task 5: Motor de Tormenta de señales

**Files:** Create `game/.../games/StormGame.kt` · Test `game/src/test/.../games/StormGameTest.kt`

**Reglas:**
- Cada señal cae del cielo al mar en `fallMs`. La primera dura 7000 ms y cada nueva señal tarda un
  3 % menos, con un mínimo de 3500 ms.
- Aparece una señal nueva cada `intervalMs`: empieza en 2500 ms y baja un 3 % por señal, con un
  mínimo de 900 ms.
- La letra se elige con `Leitner.pick` entre las aprendidas. Al aparecer, emite el evento
  `Spawned(char)` para que suene y se dibuje su patrón Morse; no se muestra la letra.
- Escribir una letra elimina la señal de esa letra que más ha caído: suma 1 punto y registra un
  acierto. Una letra sin señal correspondiente no hace nada.
- Si una señal llega al mar se pierde una vida (hay 3) y se registra un fallo. Con 0 vidas la
  partida termina.
- Tras el final, `input` y `tick` no hacen nada.

```kotlin
data class FallingSignal(val id: Int, val char: Char, val spawnedAt: Long, val fallMs: Long) { fun progress(now: Long): Float }
sealed interface StormEvent { data class Spawned(val char: Char) : StormEvent; data class Hit(val char: Char) : StormEvent; data class Splash(val char: Char) : StormEvent; data object GameOver : StormEvent }
class StormGame(chars: Collection<Char>, boxes: Map<Char, Int>, random: Random, startAt: Long) {
    val signals: List<FallingSignal>; val score: Int; val lives: Int; val isOver: Boolean; val outcomes: List<CharOutcome>
    fun tick(now: Long): List<StormEvent>
    fun input(c: Char, now: Long): List<StormEvent>
    companion object { const val LIVES = 3 }
}
```
- [ ] **Step 1: Escribir las pruebas que fallan**
  - `first_signal_spawns_immediately`: `tick(startAt)` da `Spawned` con una señal.
  - `spawn_interval_and_speed_shrink`.
  - `input_hits_lowest_matching_signal`: con dos S, se elimina la más antigua.
  - `wrong_input_does_nothing` y `double_input_scores_once` (Review Focus 2).
  - `signal_reaching_sea_costs_a_life`.
  - `three_splashes_end_the_game`: después, `input` y `tick` no cambian el estado.
  - `few_chars_work` (Review Focus 4): con solo S y O durante 60 s simulados, todas las señales
    son S u O.
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.**

### Task 6: Motor de Destello rápido

**Files:** Create `game/.../games/FlashGame.kt` · Test `game/src/test/.../games/FlashGameTest.kt`

**Reglas:**
- 10 rondas. En cada ronda se elige una letra con `Leitner.pick` y la fase es `SHOWING`, mientras
  la UI reproduce el destello.
- Cuando la UI llama a `flashFinished(now)`, la fase pasa a `ANSWERING` con 4 opciones (o menos si
  hay menos letras, nunca menos de 2).
- `answer(c, now)`: si acierta, suma `max(0, 2000 − ms) / 20` puntos (0–100) y guarda el tiempo de
  reacción. Fallo = 0 puntos. En ambos casos se pasa a la ronda siguiente.
- Al terminar la 10.ª ronda, `isOver`. `averageReactionMs` es la media de los aciertos (0 si no
  hay ninguno).
- Una respuesta fuera de `ANSWERING` se ignora.

```kotlin
enum class FlashPhase { SHOWING, ANSWERING, OVER }
class FlashGame(chars: Collection<Char>, boxes: Map<Char, Int>, random: Random) {
    val round: Int; val phase: FlashPhase; val current: Char?; val options: List<Char>; val score: Int
    val averageReactionMs: Int; val outcomes: List<CharOutcome>; val isOver: Boolean
    fun flashFinished(now: Long); fun answer(c: Char, now: Long): Boolean
    companion object { const val ROUNDS = 10 }
}
```
- [ ] **Step 1: Escribir las pruebas que fallan**
  - `round_flow`.
  - `fast_answer_scores_more`: 500 ms dan 75 puntos y 2500 ms dan 0.
  - `wrong_answer_zero`.
  - `answer_during_showing_ignored` y `double_answer_counts_once` (Review Focus 2).
  - `ten_rounds_then_over`.
  - `options_valid_with_two_letters` (Review Focus 4).
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.**

### Task 7: Motor de Interceptación

**Files:** Create `game/.../games/InterceptGame.kt` · Test `game/src/test/.../games/InterceptGameTest.kt`

**Reglas:**
- Palabras del banco del capítulo que solo usan letras aprendidas. Si no hay ninguna, se usan
  combinaciones de 2–3 letras.
- La velocidad empieza en `startWpm` (de los ajustes, entre 5 y 30) y sube 1 PPM cada 2 aciertos
  seguidos, hasta 30. `maxWpm` registra la más alta alcanzada.
- `answer(text)` compara con `MissionSession.matches`. Un fallo suma un strike; con 3 strikes, la
  partida termina. Los resultados se calculan con `Leitner.outcomesFor`.
- La palabra siguiente nunca repite la inmediatamente anterior, salvo que solo haya una.

```kotlin
class InterceptGame(chapterWords: List<String>, chars: Collection<Char>, startWpm: Int, random: Random) {
    val currentWord: String; val wpm: Int; val score: Int; val strikes: Int; val maxWpm: Int; val isOver: Boolean; val outcomes: List<CharOutcome>
    fun answer(text: String): AnswerResult   // de MissionSession; tras el final lanza IllegalStateException
    companion object { const val STRIKES = 3 }
}
```
- [ ] **Step 1: Escribir las pruebas que fallan**
  - `speed_increases_every_two_correct`.
  - `three_strikes_over`.
  - `lenient_matching` (Review Focus 5).
  - `no_immediate_repeat`.
  - `fallback_combos_without_words`.
  - `answer_after_over_throws`.
- [ ] **Step 2–4:** ejecutar (FAIL), implementar y ejecutar (PASS).
- [ ] **Step 5: Commit.**

### Task 8: Reglas de desbloqueo de minijuegos y letras aprendidas

**Files:** Modify `game/.../progress/ProgressRules.kt` · Test `ProgressRulesTest`
```kotlin
// ProgressRules gana:
fun learnedChars(chapters: List<Chapter>, completed: Set<String>): Set<Char>   // letras nuevas de las misiones completadas
fun gameUnlockMission(game: String): String     // "STORM"→"c1m2", "FLASH"→"c1m3", "INTERCEPT"→"c1r1"
fun isGameUnlocked(game: String, completed: Set<String>): Boolean
```
- [ ] Pruebas que fallan: `learned_from_completed_only` y `game_unlocks`. Luego implementar y commit.

### Task 9: Pantalla Práctica (lista de minijuegos)

**Files:** Create `ui/practice/PracticeScreen.kt`, `PracticeViewModel.kt` · Modify `ui/hub/HubScreen.kt` (botón «Práctica» en la barra inferior), `AppNavHost.kt` (rutas `practice`, `game/storm`, `game/flash`, `game/intercept`), `strings.xml` · Test `PracticeViewModelTest`, `PracticeScreenTest`
- Cada minijuego aparece como tarjeta con icono propio, descripción, récord y número de partidas.
  Las bloqueadas muestran «Se desbloquea al completar «…»».
- [ ] Pruebas que fallan: `locked_until_mission`, `shows_records` y `tapping_unlocked_opens_game`.
  Luego implementar y commit.

### Task 10: Pantallas de los tres minijuegos

**Files:** Create `ui/games/StormScreen.kt` y `StormViewModel.kt`, `ui/games/FlashScreen.kt` y `FlashViewModel.kt`, `ui/games/InterceptScreen.kt` e `InterceptViewModel.kt`, y `ui/games/GameOverPanel.kt` · Test `ui/games/*ViewModelTest.kt` y `GamesScreenTest.kt`

- **Comunes a los tres ViewModels:**
  - Reciben `ContentSource`, `ProgressRepository`, `CharStatsRepository`, `GameRecordsRepository`,
    `SettingsRepository`, `OutputFactory`, un reloj y `Random`.
  - Al terminar la partida, guardan **una vez** el récord y los resultados por carácter.
  - `abandon()` detiene el sonido y no guarda nada (Review Focus 3). La UI lo llama al salir de la
    pantalla y en `ON_STOP`.
- **Tormenta:**
  - Un `Canvas` con el cielo nocturno y el mar. Cada señal es una franja con su patrón Morse (`•`
    y `▬`) que baja.
  - Un teclado con las letras aprendidas y tres vidas dibujadas como faros.
  - El bucle usa `withFrameMillis` y llama a `tick`. Cada `Spawned` reproduce el sonido.
- **Destello rápido:**
  - Un faro grande cuya linterna parpadea al ritmo del Morse (sin linterna real), con la velocidad
    limitada a 7 PPM si «limitar destellos» está activado.
  - Las opciones aparecen al terminar el destello y se muestra el tiempo de reacción.
- **Interceptación:**
  - Se escucha la palabra a la velocidad actual. Hay botón «Repetir»: una vez por palabra y sin
    coste.
  - Campo de texto, velocidad actual y strikes.
- **Fin de partida:** puntuación, «¡Nuevo récord!» si toca, y los botones «Otra vez» y «Volver».
- [ ] Pruebas que fallan:
  - En cada ViewModel: `game_over_saves_once`, `abandon_saves_nothing` y
    `uses_only_learned_chars`.
  - En la UI: `storm_keyboard_shows_learned_letters` y `game_over_panel_shows_record`.

  Luego implementar y commit.

### Task 11: APK de la Etapa 3a, capturas y entrega

- [ ] Ejecutar la suite completa, lint y `assembleRelease`. Comprobar la firma, que no esté
  `INTERNET` y la versión 0.3.0.
- [ ] Revisar capturas de Práctica y de los tres minijuegos con una prueba temporal; corregir lo que
  desentone, con su prueba.
- [ ] Escribir `docs/pruebas/etapa-3a.md`. Comprobaciones:
  - Instalar **encima** y comprobar que se conserva el progreso.
  - Jugar los tres minijuegos.
  - Salir a mitad de una partida.
  - Ver que los repasos insisten en las letras falladas.
- [ ] Actualizar el README, hacer commit, push y la revisión final independiente. Entregar el APK y
  la lista de pruebas.
