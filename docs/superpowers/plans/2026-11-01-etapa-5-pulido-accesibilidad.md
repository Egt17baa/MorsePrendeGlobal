# Etapa 5: pulido y accesibilidad. Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completar lo que la spec pide en §7.10 (Ajustes) y §8 (diseño visual) y aún falta:
- borrar el progreso;
- tamaño de texto;
- alto contraste;
- activar o desactivar la vibración;
- una paleta propia por capítulo;
- una tipografía OFL incluida en el APK.

**Architecture:**
- **Ajustes nuevos:** van en `AppSettings` y DataStore, y la UI los lee como hasta ahora.
- **Tema:** `MorsePrendeTheme` recibe los ajustes de contraste y tamaño de texto, más la tipografía
  propia. Un `ChapterTheme` sobrescribe los colores de acento dentro de las misiones de cada capítulo.
- **Borrar progreso:** lo hace un servicio que vacía Room, las fichas vistas y las partidas guardadas, y
  devuelve los cosméticos a sus valores por defecto.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room 2.8, DataStore, Robolectric.

**Spec:** `docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md` §7.10, §8 y §9.

## Global Constraints

- Las de siempre: sin `INTERNET`, textos de interfaz en `strings.xml`, APK firmado y sin regex
  incompatibles con Android. `versionCode 10`, `versionName "0.7.0"`.
- **Sin cambios de esquema de Room.** Borrar usa `clearAllTables()`.
- **Tipografía:** Atkinson Hyperlegible Regular y Bold (SIL OFL 1.1, Braille Institute), en
  `app/src/main/res/font/`. La licencia va en `app/src/main/assets/licenses/atkinson-hyperlegible-OFL.txt`
  y se menciona en Ajustes, en «Acerca de».
- **Paletas** (§8):
  - Faro: verde azulado de mar, crema y coral (las actuales).
  - Sombra: oliva `#5E6B2E`, ámbar `#E3A33B` y gris `#8C9096`.
  - Estelar: violeta `#3A2A6E`, blanco estelar `#F3F0FF` y magenta `#D4458C`.
  - En modo oscuro el primario es el color claro (ámbar o blanco estelar); en claro, el oscuro (oliva o
    violeta). El secundario es el gris o el magenta.
- **Tamaño de texto:** `textScale` ∈ {1.0, 1.15, 1.3}, que se multiplica por el `fontScale` del
  sistema.
- **Alto contraste:**
  - Oscuro: fondo `#000000`, texto `#FFFFFF`, primario `#FFE14D` y bordes blancos.
  - Claro: fondo `#FFFFFF`, texto `#000000`, primario `#00363D` y bordes negros.
  - Con alto contraste no se aplican las paletas por capítulo.
- **Vibración desactivada:**
  - El manipulador no da respuesta háptica.
  - Las misiones de vibración se hacen por sonido, con el aviso
    `mission_channel_vibration_disabled`.
  - El traductor no cambia: allí la vibración ya la elige el usuario.
- **Borrar progreso:**
  - Borra estrellas, cajas de Leitner, récords, Destellos, objetos, días jugados, logros, fichas
    vistas y partidas a medias.
  - Devuelve los cosméticos equipados a sus valores por defecto.
  - **Mantiene** velocidad, tono, volumen, modo de manipulador y accesibilidad.
  - Pide confirmación.

## Review Focus

1. **Borrar progreso con una misión abierta o con el Faro en pantalla.** Se espera que, al volver,
   todo aparezca desde cero, sin estrellas fantasma ni una partida a medias guardada. Lo prueban
   `reset_clears_everything_but_settings` y `hub_shows_fresh_after_reset`.
2. **Texto «Muy grande» con el tamaño de fuente del sistema también al máximo.** Se espera que los
   botones de la misión, el teclado de la Tormenta y el manipulador sigan cabiendo y pulsándose. Lo
   prueba `huge_text_mission_and_storm_still_usable`.
3. **Vibración desactivada en la misión «Solo pulso».** Se espera que la misión se haga por sonido,
   con su aviso, y que no vibre. Lo prueba `vibration_disabled_plays_by_sound`.
4. **Ajustes de la 0.6.x sin las claves nuevas.** Se espera que carguen con los valores por defecto
   sin perder los demás. Lo prueba `old_settings_load_with_new_defaults`.
5. **Alto contraste junto con un capítulo con paleta propia.** Se espera que gane el alto contraste
   (texto legible, sin el violeta oscuro sobre negro). Lo prueba `high_contrast_overrides_chapter_palette`.

---

### Task 1: Ajustes nuevos en el modelo y en DataStore
**Files:**
- Modify: `app/src/main/kotlin/io/github/egt17baa/morseprende/settings/AppSettings.kt`
- Modify: `app/src/main/kotlin/io/github/egt17baa/morseprende/settings/SettingsRepository.kt`
- Test: `app/src/test/kotlin/io/github/egt17baa/morseprende/settings/SettingsRepositoryTest.kt`

**Interfaces:**
- Produces:
  - En `AppSettings`: `textScale: Float = 1f`, `highContrast: Boolean = false` y `vibration: Boolean = true`.
  - `clamped()` lleva `textScale` al valor permitido más cercano de `AppSettings.TEXT_SCALES = listOf(1f, 1.15f, 1.3f)`.

- [ ] Prueba `new_settings_persist`: guardar 1.3, true y false, y leer los mismos valores.
- [ ] Prueba `old_settings_load_with_new_defaults`: un fichero que solo tiene `char_wpm = 20` se lee
  con `charWpm == 20`, `textScale == 1f`, `highContrast == false` y `vibration == true`.
- [ ] Prueba `text_scale_clamped`: 2.0 da 1.3 y 1.1 da 1.15.
- [ ] Implementar con las claves `text_scale`, `high_contrast` y `vibration`, y hacer commit.

### Task 2: Tipografía, tamaño de texto y alto contraste en el tema
**Files:**
- Create: `app/src/main/res/font/atkinson_regular.ttf`, `app/src/main/res/font/atkinson_bold.ttf` y `app/src/main/assets/licenses/atkinson-hyperlegible-OFL.txt`
- Create: `app/src/main/kotlin/io/github/egt17baa/morseprende/ui/theme/Type.kt`
- Modify: `ui/theme/Theme.kt` y `MainActivity.kt`
- Test: `app/src/test/kotlin/io/github/egt17baa/morseprende/ui/theme/ThemeTest.kt`

**Interfaces:**
- Produces:
  - `val Atkinson: FontFamily` y `val AppTypography: Typography`: la tipografía de Material 3 por
    defecto con `fontFamily = Atkinson` en todos los estilos.
  - `@Composable fun MorsePrendeTheme(darkTheme: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false, textScale: Float = 1f, content: @Composable () -> Unit)`.
    Multiplica el tamaño de texto con `CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * textScale))`.
  - `fun colorSchemeFor(dark: Boolean, highContrast: Boolean): ColorScheme`, pública para las pruebas.
- `MainActivity` lee los ajustes del contenedor con `collectAsStateWithLifecycle` y se los pasa al tema.

- [ ] Prueba `high_contrast_schemes`:
  - `colorSchemeFor(true, true).background == Color.Black`, `.onBackground == Color.White` y `.primary == Color(0xFFFFE14D)`.
  - En claro: `.background == Color.White` y `.onBackground == Color.Black`.
- [ ] Prueba `text_scale_enlarges_text`: un `Text("A")` dentro de `MorsePrendeTheme(textScale = 1.3f)`
  es más alto (`onNodeWithText("A").fetchSemanticsNode().size.height`) que con 1f, en una proporción
  de al menos 1,25.
- [ ] Prueba `typography_uses_atkinson`: `AppTypography.bodyLarge.fontFamily == Atkinson`.
- [ ] Implementar y hacer commit.

### Task 3: Paleta propia por capítulo
**Files:**
- Create: `app/src/main/kotlin/io/github/egt17baa/morseprende/ui/theme/ChapterTheme.kt`
- Modify: `ui/theme/Color.kt` (con las constantes de Global Constraints), `ui/mission/MissionUiState.kt`, `ui/mission/MissionViewModel.kt`, `ui/mission/MissionScreen.kt` y `ui/hub/HubScreen.kt`
- Test: `ui/theme/ThemeTest.kt` y `ui/mission/MissionViewModelTest.kt`

**Interfaces:**
- Produces:
  - `fun chapterColors(base: ColorScheme, chapterId: Int, dark: Boolean, highContrast: Boolean): ColorScheme`.
    Con el capítulo 1 o con `highContrast` devuelve `base`. Si no, cambia `primary`, `onPrimary`,
    `secondary`, `primaryContainer` y `outline`.
  - `@Composable fun ChapterTheme(chapterId: Int, content: @Composable () -> Unit)`. Lee el esquema
    actual de `MaterialTheme` y el alto contraste de `LocalHighContrast`, un `staticCompositionLocalOf`
    que provee `MorsePrendeTheme`.
  - `MissionUiState.chapterId: Int = 1`, rellenado en `briefingState` desde el capítulo de la misión.
- `MissionContent` se envuelve en `ChapterTheme(state.chapterId)`. En el Faro, el título de cada
  capítulo desbloqueado usa el `primary` de `chapterColors` de su capítulo.

- [ ] Prueba `chapter_palettes`:
  - En el capítulo 2, `chapterColors(dark, 2, true, false).primary == Amber` y `(light, 2, false, false).primary == Olive`.
  - En el capítulo 3, `(dark, 3, true, false).primary == StarWhite` y `.secondary == Magenta`.
  - En el capítulo 1 se devuelve `base` sin cambios.
- [ ] Prueba `high_contrast_overrides_chapter_palette` (Review Focus 5): `chapterColors(hc, 3, true, true) == hc`.
- [ ] Prueba `mission_knows_its_chapter`: una misión del capítulo 2 en `FakeContentSource` da `state.chapterId == 2`.
- [ ] Implementar y hacer commit.

### Task 4: Vibración activable
**Files:**
- Modify: `ui/mission/MissionViewModel.kt`, `ui/mission/MissionScreen.kt`, `ui/mission/KeyerPad.kt`, `settings/SettingsViewModel.kt`, `settings/SettingsScreen.kt` y `res/values/strings.xml`
- Test: `ui/mission/MissionViewModelTest.kt` y `ui/mission/MissionScreenTest.kt`

**Interfaces:**
- Consumes: `AppSettings.vibration` (Task 1).
- Produces:
  - `MissionUiState.vibrationEnabled: Boolean = true`, copiado de los ajustes en el `combine` de `state`.
  - `KeyerPad(mode, actions, ring, haptics: Boolean = true)`.
  - `SettingsViewModel.setVibration(Boolean)`.
- En `replay()`, con `!prefs.vibration` el canal VIBRATION se trata como fallback: sonido, sin
  marcar `channelFallback`. `ChannelBanner` muestra `mission_channel_vibration_disabled`, «Vibración
  desactivada en Ajustes: esta misión se hace por sonido.», y no muestra el botón «usar sonido».

- [ ] Prueba `vibration_disabled_plays_by_sound` (Review Focus 3): con
  `channelEnv(channel = VIBRATION, settings = AppSettings(vibration = false))`, la última salida creada
  es SOUND y no se crea ninguna VIBRATION.
- [ ] Prueba `vibration_disabled_notice`: el estado con `channel = VIBRATION` y `vibrationEnabled = false`
  muestra el texto del aviso y no muestra `mission_vibration_use_sound`.
- [ ] Implementar, poner el interruptor «Vibración» en la sección de sonido de Ajustes y hacer commit.

### Task 5: Borrar progreso
**Files:**
- Create: `app/src/main/kotlin/io/github/egt17baa/morseprende/data/ProgressReset.kt`
- Modify:
  - `data/SeenCardsRepository.kt`: añadir `suspend fun clear()`.
  - `data/save/MissionSaveStore.kt`: añadir `suspend fun clearAll()`.
  - `MorsePrendeApp.kt`, `ui/navigation/AppNavHost.kt`, `settings/SettingsViewModel.kt`, `settings/SettingsScreen.kt` y `res/values/strings.xml`.
- Test: `app/src/test/kotlin/io/github/egt17baa/morseprende/data/ProgressResetTest.kt` y `settings/SettingsScreenTest.kt`

**Interfaces:**
- Produces:
  - `fun interface ProgressReset { suspend fun resetAll() }`.
  - `class AppProgressReset(db: AppDatabase, seen: SeenCardsRepository, saves: MissionSaveStore, settings: SettingsRepository) : ProgressReset`.
    Llama a `db.clearAllTables()` en `Dispatchers.IO`. Después ejecuta
    `settings.update { it.copy(lampColor = d.lampColor, toneTimbre = d.toneTimbre, keyerSkin = d.keyerSkin, decorations = d.decorations) }`
    con `d = AppSettings()`.
  - `SettingsViewModel(repo, outputs, reset: ProgressReset? = null)` con `fun resetProgress()`.

- [ ] Prueba `reset_clears_everything_but_settings` (Review Focus 1), con Room en memoria, almacenes
  reales sobre carpetas temporales y ajustes `charWpm = 22` con `lampColor = "lamp_red"`:
  - Antes: se guardan una estrella, un récord, Destellos, un logro, una ficha vista y una partida.
  - Tras `resetAll()`, todo está vacío, el saldo es 0, `charWpm == 22` y `lampColor == "lamp_cream"`.
- [ ] Prueba `hub_shows_fresh_after_reset`: el `HubViewModel` sobre el mismo repositorio de progreso
  pasa a tener 0 estrellas y solo c1m1 desbloqueada, sin crear un ViewModel nuevo, porque los flujos
  de Room emiten.
- [ ] Prueba `reset_needs_confirmation`:
  - Pulsar `settings_reset` muestra el diálogo y todavía no llama al reset.
  - «Cancelar» no lo llama. «Borrar» lo llama una vez.
- [ ] Implementar y hacer commit.

### Task 6: Ajustes de accesibilidad, «Acerca de», pruebas con texto grande y entrega 0.7.0
**Files:**
- Modify: `settings/SettingsScreen.kt` (sección «Accesibilidad»: tamaño de texto con 3 opciones,
  interruptor de alto contraste y «Limitar destellos», que se mueve aquí; sección «Acerca de» con la
  versión y la línea de la tipografía y su licencia), `res/values/strings.xml`, `app/build.gradle.kts`,
  `README.md` y `docs/pruebas/etapa-5.md`.
- Test: `settings/SettingsScreenTest.kt` y `ui/games/GamesScreenTest.kt`

- [ ] Prueba `accessibility_controls_update_settings`: pulsar «Muy grande» lleva a `textScale 1.3`
  y el interruptor de contraste a `highContrast true`.
- [ ] Prueba `huge_text_mission_and_storm_still_usable` (Review Focus 2):
  - Con `@Config(fontScale = 1.3f)` y `MorsePrendeTheme(textScale = 1.3f)`, en una misión de
    elección se pulsan las cuatro opciones con `performScrollTo` y aparece el manipulador.
  - En la Tormenta con 45 teclas, la última tecla está `assertIsDisplayed`.
- [ ] Versión 0.7.0 (code 10). Suite completa, lint y release firmado sin `INTERNET`. Capturas en
  oscuro, claro, alto contraste y capítulos 2 y 3.
- [ ] Revisión independiente con foco en las diferencias entre el móvil y las pruebas. Arreglos con
  TDD, commit, push y entrega del APK con la lista de pruebas.
