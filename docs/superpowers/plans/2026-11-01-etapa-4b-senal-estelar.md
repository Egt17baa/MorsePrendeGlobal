# Etapa 4b: capítulo 3 «Señal Estelar». Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** El capítulo 3 de la spec §7.1 y §7.2. Contactas con una estación orbital… y con algo
desconocido. Incluye:
- **Puntuación** `. , ? / ' !` y los **prosignos AR y BT** (que comparten código con `+` y `=`),
  enseñados con su significado;
- misiones de **velocidad**;
- **señales que llegan solo como luz o solo como vibración**;
- fichas nuevas y el logro final.

Cierra la campaña. Al terminar se entrega una sola versión 0.6.0 con las Etapas 4a y 4b, tras una
revisión independiente conjunta (el usuario lo pidió: «hazlo todo y luego pruebo yo todo»).

**Spec:** §7.1, §7.2, §7.3 y §7.8.

## Global Constraints

- Las de siempre (sin `INTERNET`, textos de interfaz en `strings.xml`, APK firmado, sin regex
  incompatibles y fichas con fuentes). `versionCode 7`, `versionName "0.6.0"`.
- **Orden del capítulo 3** (ids):
  - c3m1 `.` `,` · c3m2 `?` `/` · **c3r1**
  - c3m3 `=` `+` (presentados como **BT**, «separación», y **AR**, «fin de mensaje») · c3m4 `'` `!`
    · **c3r2**
  - **c3l** (repaso: solo luz) · **c3v** (repaso: solo vibración) · **c3s** (repaso: velocidad
    20 PPM) · **c3f** (final a 18 PPM)
- **SK** (`...-.-`) no está en la tabla de caracteres. Se explica en la ficha de prosignos, junto
  con AR y BT, y no se practica como carácter. (Decisión: añadir prosignos como tokens exigiría
  cambiar el códec y la tabla UIT, y la spec pide sobre todo conocer su significado.)
- **Modelo:**
  - `Mission` gana `wpm: Int? = null`, que fija la velocidad de carácter y la efectiva de la misión
    (entre 5 y 30).
  - Gana también `channel: Channel = SOUND`, con `enum class Channel { SOUND, LIGHT, VIBRATION }`,
    que se aplica a los ejercicios de **recibir** y al mensaje final.
- **Luz:** un faro dibujado en una zona pequeña de la misión. Con «limitar destellos», la velocidad
  se limita a 7 PPM. **Vibración:** salida `VIBRATION`. En ambos canales «Escuchar otra vez» repite
  por el mismo canal.
- Las letras de la puntuación se acumulan para los minijuegos como cualquier otra.

## Review Focus

1. **Canal de luz con «limitar destellos» activado.** Se espera que nunca supere 7 PPM, aunque la
   misión pida 20.
2. **Móvil sin vibrador en la misión de vibración.** Se espera un aviso y que se pueda hacer igual
   por sonido; nunca debe quedarse atascado.
3. **Misiones de velocidad y ajustes del usuario.** Se espera que la velocidad de la misión mande
   en esa misión y no cambie los ajustes guardados.
4. **Puntuación al escribir respuestas** (`?`, `/`, `'`, `!` en el teclado del móvil). Se espera
   que se comparen bien, sin que la normalización los elimine.
5. **Capítulo 3 bloqueado hasta c2f.** Se espera que la Ñ opcional no lo bloquee.

---

### Task 1: Modelo `wpm` y `channel`, y su validación (`:game`)
- [ ] Pruebas que fallan:
  - `wpm_and_channel_validated`: `wpm` entre 5 y 30.
  - `channel_parsed`: JSON con `"channel":"LIGHT"`.

  Luego implementar y commit.

### Task 2: Contenido del capítulo 3
- `chapter3.json` con el orden exacto, banco de palabras y frases con signos (al menos 40), y
  mensajes finales con puntuación. Se carga en `AssetContentSource`.
- [ ] Pruebas que fallan:
  - `real_chapter3_is_valid`: validación conjunta de los tres capítulos, ids, pares, canales y
    velocidades.
  - `chapter3_unlocks_after_c2f_without_enye`.
  - `planner_works_for_every_chapter3_mission`.

  Luego escribir y commit.

### Task 3: Velocidad y canal en las misiones
- `MissionViewModel`:
  - La reproducción usa `MorseTiming(mission.wpm ?: ajustes)`.
  - Con el canal LIGHT, la luz es un `SteppedOutput` que actualiza `lampOn`, limitado a 7 PPM si
    `limitFlashes`.
  - Con VIBRATION se usa la salida de vibración. Si falla (no hay vibrador), `channelFallback = true`
    y se reproduce por sonido.
- `MissionUiState` gana `channel`, `lampOn` y `channelFallback`. `MissionScreen` dibuja el faro
  pequeño en el canal LIGHT y muestra un aviso en el de vibración.
- [ ] Pruebas que fallan:
  - `mission_wpm_overrides_settings_without_saving` (Review Focus 3).
  - `light_channel_capped_when_limiting` (Review Focus 1).
  - `vibration_channel_uses_vibration_output`.
  - `vibration_failure_falls_back_to_sound` (Review Focus 2).
  - `punctuation_answers_match` (Review Focus 4).

  Luego implementar y commit.

### Task 4: Biblioteca del capítulo 3 y logro final
- Fichas, investigadas y con fuentes:
  - `lib-prosignos` (AR, BT, SK y su uso), desbloqueada en c3m3.
  - `lib-accesibilidad` (Morse como forma de escribir para personas con movilidad reducida;
    teclado Morse de Gboard), desbloqueada en c3v.
  - `lib-hoy` (el Morse hoy: radioaficionados, balizas…), desbloqueada en c3f.
- Logro `stargazer`: completar c3f.
- [ ] Pruebas que fallan: `real_library_is_valid` con 14 fichas y `stargazer`. Luego escribir y
  commit.

### Task 5: Faro, 0.6.0, capturas, revisión conjunta 4a+4b y entrega
- [ ] El Faro deja de mostrar «Próximamente» al existir el capítulo 3 (lo prueba `chapter3_section_present`).
- [ ] Suite completa, lint y release firmado sin `INTERNET`. Capturas.
- [ ] Escribir `docs/pruebas/etapa-4.md` y actualizar el README.
- [ ] Revisión independiente conjunta de las Etapas 4a y 4b, con foco en las diferencias entre el
  móvil y las pruebas. Arreglos con TDD, commit, push y entrega.
