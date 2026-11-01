# Etapa 3b: Destellos, tienda, logros y racha. Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar spec §7.7:
- **Destellos**, la moneda del juego. Se ganan con misiones, repasos, minijuegos y récords, y solo
  sirven para cosas **estéticas**: color de la luz del faro, timbre del pitido, aspecto del
  manipulador y decoración de la isla.
- **Racha diaria suave**, con un día de descanso automático por semana y sin castigo.
- **Logros.**
- **El faro y la isla cambian** con lo que equipas.

**Architecture:**
- La lógica va en Kotlin puro en `:game`: reglas de recompensa, catálogo de la tienda, cálculo de
  la racha y evaluación de logros.
- `:app` añade:
  - la base de datos v3 (migración automática 2→3) con `wallet`, `owned_items`, `played_days` y
    `achievements`;
  - lo equipado, guardado en los ajustes (DataStore);
  - un `RewardsService` que, al terminar una misión o una partida, suma Destellos, marca el día
    jugado y desbloquea logros, y devuelve un resumen para la pantalla final;
  - las pantallas **Tienda** y **Logros**, y en el Faro el saldo y la racha.
- El timbre del pitido entra en `ToneSynth` y en el tono en vivo.

**Tech Stack:** lo de la Etapa 3a. Se usa `java.time.LocalDate`, disponible desde la API 26, que es
el minSdk.

**Spec:** `docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md` §7.7 y §8.

## Global Constraints

- Las de etapas anteriores: sin `INTERNET`, textos en `strings.xml`, APK firmado y sin
  expresiones regulares incompatibles con Android (`AndroidRegexCompatibilityTest`).
- `versionCode 5`, `versionName "0.4.0"`.
- **La migración 2→3 conserva el progreso, las cajas y los récords.** Lo comprueba un test.
- **Sin compras reales. Los Destellos no se pueden comprar ni dan ventaja de juego.**
- **Recompensas** (valores decididos):
  - **Misión completada:** 5 Destellos, más 10 por cada estrella por encima de tu mejor marca
    anterior.
  - Repaso o final: +5 extra.
  - **Minijuego:** 1 Destello por cada 5 puntos (máximo 10), más 15 por récord nuevo con puntuación
    mayor que 0.
  - **Bono diario:** +5 por la primera misión o partida completada del día.
- **Catálogo** (`id`, precio):
  - Luz del faro (se equipa una): `lamp_cream` 0 (de serie), `lamp_emerald` 40, `lamp_harbor` 40
    (rojo puerto), `lamp_polar` 60 (azul polar) y `lamp_amber` 60.
  - Timbre (se equipa uno): `tone_sine` 0 (de serie), `tone_soft` 50 (triangular) y `tone_classic`
    80 (cuadrada suavizada con los armónicos 1, 3 y 5).
  - Manipulador (se equipa uno): `key_coral` 0 (de serie), `key_brass` 50 (latón) y `key_silver` 70.
  - Decoración de la isla (se pueden activar varias): `decor_gulls` 30, `decor_boat` 60,
    `decor_flags` 50 y `decor_lanterns` 80.
- **Racha:**
  - Cuenta días consecutivos con al menos una misión o partida **completada**, mirando hacia atrás
    desde hoy.
  - Si hoy aún no has jugado, la racha **no se rompe**: se cuenta desde ayer.
  - Se perdona **un día sin jugar por semana ISO** (día de descanso automático). Ese día no suma.
  - Nunca hay castigo ni notificaciones; solo se muestra el número.
- **Logros** (id: condición):
  - `first_sos`: completar c1m1.
  - `three_flashes`: alguna misión con 3 estrellas.
  - `keeper`: completar c1f.
  - `perfectionist`: todas las misiones del capítulo 1 con 3 estrellas.
  - `storm_rider`: Tormenta con 20 puntos o más.
  - `quick_eye`: reacción media en Destello de 800 ms o menos (récord mayor que 0).
  - `sharp_ear`: Interceptación con 10 puntos o más.
  - `speed_20`: 20 PPM o más en Interceptación.
  - `steady`: racha de 7 días o más.
  - `collector`: 3 objetos o más comprados (que no sean de serie).
  - `reader`: todas las fichas de la biblioteca abiertas.

## Review Focus

1. **Comprar sin saldo suficiente o pulsar «Comprar» dos veces seguidas.** Se espera que nunca
   haya saldo negativo ni un cobro doble. Lo prueban las Tasks 2 y 5.
2. **Racha con cambio de semana, días saltados, varios días sin jugar o jugar hoy por primera
   vez.** Se espera que siga las reglas fijadas. Lo prueba la Task 3.
3. **Repetir una misión con las mismas estrellas.** Se espera que solo dé los 5 base (más el bono
   diario si toca), sin estrellas extra. Lo prueba la Task 1.
4. **Actualizar la app.** Se espera que la migración 2→3 conserve todo. Lo prueba la Task 5.
5. **Un logro ya conseguido.** Se espera que no se vuelva a anunciar. Lo prueban las Tasks 4 y 6.

---

### Task 1: Reglas de recompensa (`:game`)
`game/.../economy/Rewards.kt`
```kotlin
object Rewards {
    const val MISSION_BASE = 5; const val PER_NEW_STAR = 10; const val REVIEW_BONUS = 5
    const val RECORD_BONUS = 15; const val DAILY_BONUS = 5; const val GAME_MAX = 10
    fun forMission(type: MissionType, previousBest: Int, stars: Int): Int
    fun forGame(score: Int, isRecord: Boolean): Int
}
```
- [ ] Pruebas que fallan:
  - `first_completion_three_stars`: una LESSON de 0 a 3 estrellas da 35.
  - `replay_same_stars_only_base` (Review Focus 3): de 2 a 2 da 5.
  - `improvement_counts_new_stars`: de 1 a 3 da 25.
  - `review_bonus`: un REVIEW de 0 a 1 da 20.
  - `game_points_capped`: 37 puntos dan 7 y 200 puntos dan 10.
  - `record_bonus_needs_points`: récord con 12 puntos da 2 + 15; récord con 0 puntos da 0.

  Luego implementar y commit.

### Task 2: Catálogo y compra (`:game`)
`game/.../economy/Shop.kt`
```kotlin
enum class ItemCategory { LAMP, TONE, KEYER, DECOR }
data class ShopItem(val id: String, val category: ItemCategory, val price: Int) { val isDefault: Boolean get() = price == 0 }
sealed interface BuyResult { data object Bought : BuyResult; data object AlreadyOwned : BuyResult; data class NotEnough(val missing: Int) : BuyResult; data object Unknown : BuyResult }
object Shop {
    val items: List<ShopItem>                       // catálogo exacto de Global Constraints
    fun item(id: String): ShopItem?
    fun defaults(): Set<String>                     // lamp_cream, tone_sine, key_coral
    fun check(id: String, balance: Int, owned: Set<String>): BuyResult
}
```
- [ ] Pruebas que fallan:
  - `catalog_exact`: ids, precios y categorías.
  - `defaults_free_and_owned`.
  - `not_enough` (Review Focus 1): `lamp_polar` con 59 de saldo da `NotEnough(1)`.
  - `already_owned` y `unknown`.

  Luego implementar y commit.

### Task 3: Racha (`:game`)
`game/.../activity/Streak.kt`
```kotlin
object Streak { fun current(playedDays: Set<LocalDate>, today: LocalDate): Int }
```
- **Algoritmo:**
  - Se empieza en `today`. Si hoy no se ha jugado, se empieza en ayer sin gastar descanso.
  - Se recorre hacia atrás: un día jugado suma 1.
  - Un día sin jugar usa el descanso de su semana ISO (`IsoFields.WEEK_OF_WEEK_BASED_YEAR` y año)
    si no se había usado, y no suma.
  - Si el descanso de esa semana ya estaba usado, se para.
  - No se usa descanso para cruzar hacia días anteriores al primer día jugado.
- [ ] Pruebas que fallan (Review Focus 2), con fechas fijas:
  - `empty_is_zero`.
  - `today_only_is_one`.
  - `not_played_today_keeps_yesterday_streak`: con lunes, martes y miércoles jugados y hoy jueves
    sin jugar, la racha es 3.
  - `one_rest_day_per_week_forgiven`: lunes, martes, (miércoles sin jugar), jueves y viernes, con
    hoy viernes, dan 4.
  - `two_missing_days_same_week_break`: lunes, (martes), (miércoles), jueves y viernes dan 2.
  - `rest_days_in_different_weeks`: un hueco en cada una de dos semanas seguidas no rompe la racha.
  - `does_not_count_before_first_day`.

  Luego implementar y commit.

### Task 4: Logros (`:game`)
`game/.../achievements/Achievements.kt`
```kotlin
data class AchievementInput(
    val missionStars: Map<String, Int>, val chapterMissionIds: List<String>,
    val stormBest: Int, val flashBestReactionMs: Int, val interceptBest: Int, val interceptMaxWpm: Int,
    val streak: Int, val boughtItems: Int, val seenCards: Int, val totalCards: Int,
)
object Achievements {
    val ids: List<String>          // en el orden de Global Constraints
    fun earned(input: AchievementInput): Set<String>
}
```
- [ ] Pruebas que fallan: una por logro, con su umbral exacto (por ejemplo `quick_eye`: 800 sí, 801
  no y 0 no); `none_initially` y `reader_needs_all_cards`. Luego implementar y commit.

### Task 5: Base de datos v3 y repositorios
`data/progress/AppDatabase.kt` (versión 3, `AutoMigration(2, 3)`), `data/economy/*`, `data/activity/*` y `data/achievements/*`, con sus fakes en `src/test`.
```kotlin
interface EconomyRepository {
    val balance: Flow<Int>; val owned: Flow<Set<String>>          // owned incluye los de serie
    suspend fun earn(amount: Int)
    suspend fun buy(itemId: String): BuyResult                     // transacción: comprueba y cobra
}
interface ActivityRepository {
    val playedDays: Flow<Set<LocalDate>>
    suspend fun markPlayed(day: LocalDate): Boolean                // true si es la primera actividad del día
}
interface AchievementsRepository {
    val unlocked: Flow<Map<String, Long>>
    suspend fun unlock(ids: Set<String>, now: Long): Set<String>   // devuelve solo los nuevos
}
```
- [ ] Pruebas que fallan:
  - `migration_2_to_3_keeps_everything` (Review Focus 4): se crean filas en `mission_progress`,
    `char_stats` y `game_records` en la v2, se migra y siguen ahí.
  - `earn_and_buy`.
  - `buy_twice_charges_once` (Review Focus 1).
  - `buy_without_balance_keeps_balance`.
  - `mark_played_first_of_day`.
  - `unlock_returns_only_new` (Review Focus 5).

  Luego implementar y commit.

### Task 6: `RewardsService`: recompensas, día jugado y logros al terminar
`app/.../rewards/RewardsService.kt`
```kotlin
data class RewardSummary(val destellos: Int, val dailyBonus: Boolean, val newAchievements: List<String>)
class RewardsService(content, progress, records, seenCards, economy, activity, achievements, today: () -> LocalDate, now: () -> Long) {
    suspend fun onMissionCompleted(mission: Mission, previousBest: Int, stars: Int): RewardSummary
    suspend fun onGameFinished(score: Int, isRecord: Boolean): RewardSummary
    suspend fun refreshAchievements(): List<String>     // p. ej. tras abrir fichas o comprar
}
```
- **Orden en cada evento:**
  1. Marcar el día jugado (el bono diario es +5 si es el primero del día).
  2. Sumar los Destellos.
  3. Evaluar los logros con datos frescos (`first()` de cada flujo) y desbloquear los nuevos.
- [ ] Pruebas que fallan (con fakes):
  - `mission_rewards_and_daily_bonus`: la primera misión del día da 35 + 5 y la segunda, 5.
  - `first_sos_achievement_once` (Review Focus 5).
  - `game_rewards`.
  - `streak_achievement`: 7 días jugados desbloquean `steady`.

  Luego implementar y commit.

### Task 7: Conectar las recompensas a misiones y minijuegos
- `MissionViewModel` y `GameViewModelBase` reciben un `RewardsService` opcional (nulo en pruebas
  antiguas) y lo llaman tras guardar el resultado.
- `MissionUiState` y los estados de los juegos ganan `reward: RewardSummary?`.
- El debriefing y el fin de partida muestran «+N Destellos», «Bono diario +5» si toca y los logros
  nuevos.
- [ ] Pruebas que fallan:
  - `debriefing_shows_reward`.
  - `game_over_shows_reward`.
  - `abandon_gives_nothing`.

  Luego implementar y commit.

### Task 8: Ajustes equipados y timbres de sonido
- `AppSettings` gana `lampColor`, `toneTimbre`, `keyerSkin` y `decorations: Set<String>`, con los
  valores de serie.
- `ToneSynth.render(..., timbre: Timbre = Timbre.SINE)`, con `enum class Timbre { SINE, SOFT, CLASSIC }`:
  - SOFT es una onda triangular.
  - CLASSIC es la suma de los armónicos 1, 3 y 5 con amplitudes 1, 1/3 y 1/5, normalizada al mismo
    pico.

  Lo usan `AudioOutput` y `AudioSidetone`, este último con la forma de onda equivalente por muestra.
- [ ] Pruebas que fallan:
  - `soft_and_classic_differ_from_sine_but_keep_peak_and_ramps` (la rampa no hace clics: la primera
    y la última muestra están cerca de 0).
  - `settings_persist_cosmetics`.
  - `defaults_are_house_items`.

  Luego implementar y commit.

### Task 9: Tienda
`ui/shop/ShopScreen.kt` y `ShopViewModel.kt`:
- Saldo arriba y secciones por categoría.
- Cada objeto con una vista previa dibujada: color de la luz, botón «Escuchar» para el timbre, aro
  del manipulador o icono de la decoración.
- Botones: «Comprar (N)», «Equipar», «Equipado», y «Activar/Quitar» en las decoraciones.
- Comprar deshabilita el botón mientras se procesa.
- [ ] Pruebas que fallan:
  - `buy_then_equip`.
  - `cannot_buy_without_balance_shows_missing`.
  - `double_tap_buy_charges_once` (Review Focus 1).
  - `decor_toggle`.
  - `buying_third_item_unlocks_collector`.

  Luego implementar y commit.

### Task 10: Logros, saldo y racha en el Faro; cosméticos aplicados
- `ui/achievements/AchievementsScreen.kt`: lista con icono, título, descripción y fecha si está
  conseguido; los no conseguidos, atenuados.
- **Faro:**
  - Una fila superior con chips «✦ N Destellos» (abre la Tienda), «☀ N días» (racha) y «Logros».
  - `LighthouseScene` usa el color de luz equipado y dibuja las decoraciones activas: gaviotas,
    barca, banderines y farolillos.
- `StraightKey` usa el aro del manipulador equipado. Destello rápido usa el color de luz equipado.
- Al abrir fichas se llama a `refreshAchievements()` (logro `reader`).
- [ ] Pruebas que fallan:
  - `hub_shows_balance_and_streak`.
  - `achievements_screen_lists_all`.
  - `lamp_color_applied`, en el estado del Faro.

  Luego implementar y commit.

### Task 11: APK 0.4.0, capturas, pruebas y entrega
- [ ] Suite completa, lint, `assembleRelease`, firma, sin `INTERNET` y versión 0.4.0.
- [ ] Capturas de Tienda, Logros, Faro decorado y debriefing con recompensa. Corregir lo que
  desentone.
- [ ] Escribir `docs/pruebas/etapa-3b.md`, actualizar el README, hacer commit y push, pasar la
  revisión final independiente y entregar.
