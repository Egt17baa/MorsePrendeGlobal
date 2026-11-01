# Etapa 7: más enganche. Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dar motivos para volver cada día (spec §2.2, «que quiera volver»), sin castigos (§4.2). Se
añaden:
- un reto diario, el «Mensaje del día»;
- un modo infinito tras la campaña, la «Guardia nocturna»;
- logros nuevos;
- objetos nuevos para la isla.

El usuario eligió esta opción («Más enganche»).

**Architecture:**
- **Lógica pura en `:game`:**
  - `DailyChallenge` elige la frase del día de forma determinista por fecha, entre las que solo usan
    letras aprendidas.
  - `NightWatchGame` es el motor del modo infinito.
- **Contenido:** las frases del reto van en `assets/content/es/daily.json`.
- **App:**
  - `DailyRepository` (DataStore) guarda los días completados; el reset lo borra.
  - `DailyViewModel` y `DailyScreen`, con una tarjeta en el Faro.
  - `GameId.NIGHT_WATCH` reutiliza `GameViewModelBase` (récords en `game_records`, sin cambio de
    esquema).

**Spec:** §2, §4.2, §7.5 y §7.7.

## Global Constraints

- Las de siempre:
  - sin `INTERNET`;
  - textos en `strings.xml`;
  - sin regex en código de la app (Android ICU);
  - sin cambios de esquema de Room;
  - APK firmado.
- `versionCode 13`, `versionName "0.9.0"`.
- **Sin castigos** (§4.2): no pasa nada por no hacer el reto un día. Se cuenta el total de retos
  hechos, no una racha que se pierde.
- **Mensaje del día:**
  - Frase determinista por fecha local: el mismo día, la misma frase. Se elige entre las frases de
    `daily.json` cuyos caracteres están todos aprendidos. Si ninguna encaja, se usan palabras de los
    capítulos con letras aprendidas.
  - Pistas como en el mensaje final: patrón parcial tras 2 fallos y patrón completo tras 4.
  - Recompensa: `Rewards.DAILY_CHALLENGE = 15` Destellos (más el bono diario de siempre), una vez al día.
- **Guardia nocturna:**
  - Se desbloquea al completar `c3f`.
  - Cada mensaje es un carácter, un grupo de cifras o una palabra de cualquier capítulo, y se copia
    escribiéndolo.
  - Cada 5 aciertos pasa una «hora» (empieza a las 22:00): +2 PPM (hasta 30) y +0,05 de ruido (hasta
    0,35).
  - Canal por mensaje: sonido; cada 4.º mensaje, luz (respetando «limitar destellos», 7 PPM); cada
    6.º, vibración (si la vibración está activada; si no, sonido).
  - 3 fallos terminan la partida. Récord = mensajes copiados; extra = hora alcanzada.
- **Logros nuevos** (en este orden, al final de la lista):
  - `daily_first`: 1 reto hecho;
  - `daily_week`: 7 retos hechos en total;
  - `night_owl`: Guardia con 15 o más;
  - `dawn`: llegar a las 06:00 en la Guardia, es decir, la hora 8;
  - `enye`: completar `c2x`;
  - `completionist`: todas las misiones de los 3 capítulos con 3 estrellas.
- **Tienda nueva:**
  - `decor_house` (90), `decor_whale` (120), `decor_stars` (70) y `decor_aurora` (150);
  - `lamp_violet` (70);
  - `key_wood` (60).
  - Cada decoración se dibuja en `LighthouseScene`.

## Review Focus

1. **Cambio de día con la pantalla del reto abierta, o reto hecho a las 23:59.** Se espera que no se
   pague dos veces y que al día siguiente haya frase nueva. Lo prueban `daily_rewards_once_per_day`
   y `daily_message_changes_with_date`.
2. **Jugador nuevo con pocas letras.** Se espera que el reto sea siempre factible (solo letras
   aprendidas) o que esté bloqueado con explicación. Lo prueba `daily_uses_only_learned_chars`.
3. **Guardia con vibración desactivada o «limitar destellos».** Se espera que nunca vibre si está
   desactivada y que la luz nunca pase de 7 PPM. Lo prueba `night_watch_channels_respect_settings`.
4. **Borrar progreso.** Se espera que también borre los retos hechos. Lo prueba `reset_clears_daily`.
5. **Decoraciones nuevas con todas equipadas a la vez.** Se espera que la escena se dibuje sin
   solaparse de forma rara. Revisión con captura.

---

### Task 1: `DailyChallenge` y `daily.json` (`:game`)
- `object DailyChallenge`:
  - `fun messageFor(date: LocalDate, phrases: List<String>, fallbackWords: List<String>, learned: Set<Char>): String?`
  - `fun hint(message: String, level: Int): String?`, que devuelve el Morse parcial (nivel 1, la
    primera mitad de los caracteres) o el completo (nivel 2).
- Elige con el índice `floorMod(date.toEpochDay() * 7919 + 13, n)` sobre la lista filtrada y ordenada.
- `daily.json`: `{"phrases": [...]}` con al menos 60 frases cortas (≤ 24 caracteres) de faro, mar,
  espías y espacio. Todas con caracteres de la tabla y repartidas desde frases solo con E T A N S O.
- [ ] Pruebas:
  - `daily_message_changes_with_date`;
  - `daily_message_stable_same_day`;
  - `daily_uses_only_learned_chars`;
  - `daily_falls_back_to_words`;
  - `daily_null_without_letters`;
  - `daily_hint_levels`;
  - `real_daily_phrases_valid` (≥ 60, longitud y tabla).

### Task 2: Mensaje del día en la app
- `DailyRepository`:
  - `val completed: Flow<Set<LocalDate>>`;
  - `suspend fun markCompleted(day: LocalDate): Boolean` (true si es nuevo);
  - `suspend fun clear()`.
  - Implementación `DataStoreDailyRepository`.
- `RewardsService.onDailyChallenge(): RewardSummary`, que da 15 Destellos más el bono diario.
- `DailyViewModel(content, progress, daily, settings, outputs, rewards, today, playerScope)` con el estado
  `DailyUiState(phase: LOADING|LOCKED|READY|DONE, message (oculto en la UI), hintLevel, feedback, reward, totalDone)`.
  Acciones `play()`, `answer(text)` y `abandon()`.
- `DailyScreen`, la ruta `daily` y la tarjeta en el Faro: «Mensaje del día · pendiente / hecho ✓».
- `AppProgressReset` también llama a `daily.clear()`.
- [ ] Pruebas:
  - `daily_rewards_once_per_day`;
  - `daily_done_shows_come_back_tomorrow`;
  - `daily_hint_after_failures`;
  - `hub_shows_daily_card`;
  - `reset_clears_daily`.

### Task 3: Guardia nocturna
- `:game`:
  - `class NightWatchGame(words: List<String>, chars: Collection<Char>, startWpm: Int, random: Random, vibrationAllowed: Boolean)`
    con `current: NightMessage(text, channel)`, `wpm`, `noise`, `hour` (0 = 22:00), `score`,
    `strikes`, `answer(text): AnswerResult`, `isOver` y `outcomes`.
  - `ProgressRules` desbloquea `NIGHT_WATCH` con `c3f`.
- App:
  - `GameId.NIGHT_WATCH` y `NightWatchViewModel` (con la luz por `SteppedOutput`, el límite de 7 PPM
    con `limitFlashes` y la vibración por `OutputFactory`).
  - `NightWatchScreen`, con la hora «⏱ 23:00», un faro pequeño en los mensajes de luz y un aviso en
    los de vibración.
  - La tarjeta en Práctica y la ruta de juego.
- [ ] Pruebas:
  - `night_hour_advances_every_5`;
  - `night_speed_and_noise_capped`;
  - `night_channels_pattern`;
  - `night_watch_channels_respect_settings`;
  - `night_three_strikes_end`;
  - `night_locked_until_c3f`.

### Task 4: Logros y tienda
- `AchievementInput` gana `dailyDone`, `nightBest`, `nightHour` y `allMissionIds` (las de los tres
  capítulos).
- 6 logros con sus textos. `RewardPanel` y la pantalla de logros los muestran.
- Los 6 objetos en `Shop`, los colores en `Cosmetics` (`lamp_violet` `#C9A7FF` y `key_wood`
  `#A9744F`), los textos de la tienda y los dibujos de las 4 decoraciones en `LighthouseScene`.
- [ ] Pruebas:
  - `ids_in_order`, actualizada;
  - un caso por logro nuevo;
  - `shop_new_items_priced`;
  - `scene_draws_all_decorations` (Robolectric: no falla con las 8 decoraciones).

### Task 5: 0.9.0, documentación, revisión y entrega
- Versión, README, `docs/pruebas/etapa-7.md`, «Novedades» de la ficha de Play y capturas de revisión.
- Suite completa, lint, APK y AAB firmados sin `INTERNET`. Revisión independiente, arreglos con TDD,
  commit, push y entrega.
