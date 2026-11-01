# Etapa 4a: capítulo 2 «Operación Sombra». Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** El capítulo 2 de la spec §7.1 y §7.2. Bajo el faro hay una estación de escucha secreta:
interceptas transmisiones de espías y descubres a un topo. Incluye:
- el **resto de letras** (U K → W G → H L → F P → C B → J V → X Y → Z Q);
- los **números** (en parejas);
- la **Ñ** como misión extra **opcional**, marcada como extensión española no internacional;
- **ruido de radio** y **contrarreloj** en los ejercicios de recibir;
- fichas nuevas de la biblioteca;
- el Faro con varios capítulos.

**Architecture:**
- **Contenido:** `chapter2.json` y el modelo gana `optional`, `noise` y `timeLimitSeconds`.
- **Reglas:**
  - Las misiones opcionales no bloquean la campaña y no cuentan como letras aprendidas para las
    misiones siguientes.
  - El ruido se mezcla en el PCM del audio.
  - El contrarreloj vive en el `MissionViewModel`.
- **Faro:** una sección por capítulo, con desbloqueo en cadena.

**Spec:** `docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md` §7.1, §7.2, §7.8 y §8.

## Global Constraints

- Las de siempre: sin `INTERNET`, textos de interfaz en `strings.xml`, APK firmado, sin regex
  incompatibles y datos de la biblioteca verificados con fuentes (`docs/biblioteca/fuentes.md`).
- `versionCode 7`, `versionName "0.5.0"`.
- **Sin cambios de esquema de base de datos.** El progreso del capítulo 2 usa las mismas tablas.
- **Orden del capítulo 2** (ids):
  - c2m1 U K · c2m2 W G · c2m3 H L · **c2r1** (repaso)
  - c2m4 F P · c2m5 C B · c2m6 J V · **c2r2**
  - c2m7 X Y · c2m8 Z Q · **c2r3**
  - c2n1 1 2 · c2n2 3 4 · c2n3 5 6 · c2n4 7 8 · c2n5 9 0 · **c2r4**
  - **c2x Ñ** (opcional)
  - **c2f** (final)
- **Ruido** (`noise`, de 0 a 0,5): 0 en c2m1–c2m2, 0,15 en c2m3–c2r1, 0,25 en c2m4–c2r2 y 0,35
  desde c2m7. La amplitud del ruido es `noise × 0,8 × volumen` del máximo, con PCM limitado.
- **Contrarreloj** (`timeLimitSeconds`): 15 s por ejercicio de recibir desde c2r2 y 12 s desde
  c2r3. Si se acaba el tiempo, cuenta como fallo.
- La misión **c2x** (Ñ) es `optional: true`:
  - No bloquea c2f ni las siguientes.
  - Su letra no cuenta como aprendida para otras misiones.
  - En la fase de escuchar se muestra el aviso «La Ñ no es Morse internacional: es una extensión
    española».
- El capítulo 2 se desbloquea al completar c1f, porque se exige completar todas las misiones
  anteriores no opcionales.

## Review Focus

1. **Progreso de un jugador que viene de la 0.4.x.** Se espera que siga intacto y que el capítulo 2
   aparezca bloqueado o desbloqueado según c1f. Lo prueban las Tasks 1 y 5.
2. **Saltarse la Ñ opcional.** Se espera que no bloquee la final ni el porcentaje del capítulo, y
   que la Ñ no aparezca en otros ejercicios. Lo prueban las Tasks 1 y 2.
3. **Contrarreloj al salir y volver, o al responder justo al acabarse el tiempo.** Se espera que no
   haya doble respuesta ni un temporizador fantasma tras abandonar. Lo prueba la Task 4.
4. **Ruido con volumen máximo.** Se espera que no haya saturación (clipping). Lo prueba la Task 3.
5. **Minijuegos con más de 30 caracteres aprendidos.** Se espera que el teclado de la Tormenta
   quepa y se pueda usar. Lo prueba la Task 7.

---

### Task 1: Modelo y reglas: misiones opcionales, ruido y contrarreloj (`:game`)
- `Mission` gana `optional: Boolean = false`, `noise: Float = 0f` y `timeLimitSeconds: Int? = null`.
- `ContentValidator`:
  - `noise` entre 0 y 0,5; `timeLimitSeconds` igual o mayor que 5; `optional` solo en LESSON.
  - Las letras de una misión opcional no cuentan como aprendidas para las demás.
  - Los ids son únicos entre capítulos.
- `ContentValidator.learnedCharsUpTo` excluye las opcionales anteriores pero incluye la propia.
- `ProgressRules`:
  - `isUnlocked` exige las anteriores **no opcionales** completadas.
  - `nextMission` salta las opcionales.
  - `chapterProgress` no cuenta las opcionales.
  - `learnedChars` (minijuegos) sí incluye la Ñ si se completó.
- [ ] Pruebas que fallan:
  - `optional_does_not_block_next`.
  - `optional_chars_not_learned_by_others`.
  - `optional_counts_for_minigames_when_completed`.
  - `noise_and_time_limit_validated`.
  - `chapter2_unlocks_after_chapter1_finale`.
  - `progress_ignores_optional`.

  Luego implementar y commit.

### Task 2: Contenido del capítulo 2
- `app/src/main/assets/content/es/chapter2.json`:
  - Personajes: Remedios, que fue operadora de la estación; la agente «Gaviota Gris», contacto
    de la red; y el topo, que se descubre en la final.
  - Tono de intriga sin violencia gráfica. Banco de al menos 60 palabras con todas las letras,
    más grupos de cifras para los números (por ejemplo 12, 21, 112, 1234, 007, 2026).
- `AssetContentSource` carga `chapter1.json` y `chapter2.json`.
- [ ] Prueba que falla, `real_chapter2_is_valid`:
  - Validación conjunta de los capítulos 1 y 2 sin errores.
  - Orden y pares exactos de Global Constraints.
  - c2x opcional con `['Ñ']`.
  - Ruido y tiempos según la tabla.
  - Los mensajes finales solo usan caracteres aprendidos y sin Ñ, salvo el de c2x.

  También `planner_works_for_every_chapter2_mission`. Luego escribir el contenido y commit.

### Task 3: Ruido de radio en el audio
- `ToneSynth.render(..., noise: Float = 0f, random: Random = Random(0))` suma ruido uniforme de
  amplitud `noise × 0,8 × volumen × MAX` a **todas** las muestras, también en los silencios, y limita
  a ±`Short.MAX_VALUE`.
- `AudioOutput` gana `noise` y también lo aplica al silencio inicial y final y a los silencios
  intermedios.
- `OutputFactory.create(kind, settings, onFailure, noise = 0f)`.
- [ ] Pruebas que fallan:
  - `noise_zero_is_identical`.
  - `noise_fills_silence`.
  - `noise_never_clips_at_full_volume` (Review Focus 4).
  - `factory_passes_noise`.

  Luego implementar y commit.

### Task 4: Contrarreloj y ruido en las misiones
- `MissionUiState` gana `secondsLeft: Int?` y `noisy: Boolean`.
- `MissionViewModel`:
  - En un ejercicio de recibir con límite, al terminar el sonido arranca una cuenta atrás de un
    segundo por paso, con el reloj inyectado.
  - Al llegar a 0 se hace `answer("")` una sola vez.
  - Responder detiene la cuenta, y `abandon()` y `next()` la cancelan.
  - El sonido de los ejercicios pasa `noise = mission.noise`.
- `MissionScreen` muestra «⏱ N s» y una etiqueta «Interferencias» si hay ruido, y en la fase de
  escuchar muestra el aviso de la Ñ.
- [ ] Pruebas que fallan:
  - `timer_counts_down_and_times_out_once`.
  - `answering_stops_timer`.
  - `abandon_cancels_timer` (Review Focus 3).
  - `noise_passed_to_audio`.
  - `enye_notice_in_listen`.

  Luego implementar y commit.

### Task 5: El Faro con varios capítulos
- `HubUiState` pasa a tener `chapters: List<ChapterSection>`, con
  `ChapterSection(id, title, nodes, unlocked, progress)`. Se mantiene `glow` como progreso global
  sin opcionales.
- Cada capítulo es una sección con su camino. Los bloqueados muestran su título y «Se desbloquea
  al terminar el capítulo N». El capítulo 3 sigue como «Próximamente».
- Los nodos opcionales se dibujan como una estrella pequeña con la etiqueta «Extra».
- [ ] Pruebas que fallan:
  - `chapter2_locked_until_c1f`.
  - `optional_node_marked`.
  - `glow_counts_both_chapters`.

  Luego implementar (y actualizar las pruebas del Faro) y commit.

### Task 6: Biblioteca del capítulo 2 y logro
- **Investigar** con búsqueda web y apuntar las fuentes en `docs/biblioteca/fuentes.md`. Cuatro
  fichas de 150–300 palabras:
  - `lib-guerra`: el Morse en la radio de la Segunda Guerra Mundial y la «mano» de cada operador.
    Se desbloquea en c2m3.
  - `lib-denton`: Jeremiah Denton parpadea «TORTURE» (1966). Se desbloquea en c2m6.
  - `lib-numeros`: las estaciones de números. Se desbloquea en c2n5.
  - `lib-q`: abreviaturas y códigos Q (QTH, QRZ, CQ…) de radioaficionados. Se desbloquea en c2f.
- Logro nuevo `mole_hunter`: completar c2f.
- [ ] Pruebas que fallan:
  - `real_library_is_valid` ampliada a 11 fichas con los dos capítulos.
  - `mole_hunter`.
  - Actualizar `ids_in_order`.

  Luego escribir y commit.

### Task 7: Teclado de la Tormenta con muchas letras
- Con más de 12 letras, las teclas pasan a 44 dp en un `FlowRow` con desplazamiento vertical
  limitado a 3 filas visibles. El cielo mantiene una altura mínima.
- [ ] Prueba que falla, `storm_keyboard_with_36_keys_all_reachable` (Review Focus 5): se puede
  pulsar la última tecla con `performScrollTo`. Luego implementar y commit.

### Task 8: APK 0.5.0, capturas, pruebas, revisión final y entrega
- [ ] Suite completa, lint, `assembleRelease`, firma y sin `INTERNET`.
- [ ] Capturas del Faro con dos capítulos, una misión con contrarreloj e interferencias, y la
  biblioteca.
- [ ] Escribir `docs/pruebas/etapa-4a.md` y actualizar el README. Commit, push, revisión
  independiente (con foco en las diferencias entre el móvil y las pruebas) y entrega.
