# Etapa 8: Taller del farero y biblioteca ampliada. Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Práctica libre, sin puntos ni presión, para lo que más cuesta: enviar con el manipulador y
las letras que se atascan. Además, más lectura en la biblioteca, que es uno de los ganchos del juego
(spec §7.8). El usuario pidió seguir sin parar y revisarlo todo al final.

**Architecture:**
- **Taller** (dos pantallas nuevas en Práctica, sin récords ni Destellos):
  - **Envío libre:** el manipulador (el mismo `KeyerPad`) y `KeyerDecoder` muestran en vivo el texto
    que sale de lo que tecleas y la velocidad a la que vas.
  - **Letras a la carta:** eliges letras entre las aprendidas. Suena una al azar y la marcas en un
    teclado con solo esas letras. Ves aciertos por letra, y cada respuesta alimenta el repaso
    inteligente (`charStats`, modo suave).
- **Biblioteca:** 8 fichas nuevas en `library.json`, cada dato con su fuente en
  `docs/biblioteca/fuentes.md`, que se desbloquean con misiones que aún no daban ficha.

## Global Constraints

- Las de siempre:
  - sin `INTERNET`, solo `VIBRATE`;
  - textos en `strings.xml`;
  - sin regex en el código de la app;
  - sin cambios de esquema de Room;
  - APK y AAB firmados.
- **Versión:** `versionCode 16`, `versionName "0.10.0"`.
- **El Taller no puntúa:** no guarda récords, no da Destellos y no tiene vidas.
- **Envío libre:**
  - el tono del manipulador se prepara al entrar y se cierra al salir o al ir a segundo plano;
  - el texto se limita a 200 caracteres (se descarta el principio);
  - las letras que no existen se muestran como `MorseCodec.UNKNOWN`.
- **Letras a la carta:**
  - hacen falta al menos 2 letras elegidas;
  - nunca suena la misma letra dos veces seguidas;
  - por defecto vienen elegidas las 4 últimas letras aprendidas;
  - respeta la velocidad y el tono de los ajustes.
- **Fichas:**
  - de 150 a 300 palabras (`ContentValidator`) más «¿Sabías que…?»;
  - solo fuentes fiables (spec §7.8);
  - lo que las fuentes discuten se cuenta como discutido;
  - los iconos son los que ya existen (`book`, `telegraph`, `wave`, `radio`, `ship`, `lighthouse`).

## Review Focus

1. **Salir de Envío libre con la tecla pulsada.** Se espera que el tono nunca quede sonando. Lo prueba
   `free_send_exit_stops_tone`.
2. **Letras a la carta con 1 sola letra elegida.** Se espera que no empiece. Lo prueba
   `drill_needs_two_letters`.
3. **Respuestas en Letras a la carta.** Se espera que vayan al repaso, una por respuesta. Lo prueba
   `drill_records_outcomes`.
4. **Fichas nuevas.** Se espera que cumplan el validador y que cada dato tenga fuente. Lo prueban
   `RealContentTest` y una revisión manual de `fuentes.md`.

---

### Task 1: Envío libre
- `KeyerActions` (`keyDown`, `keyUp`, `dot`, `dash` y `nextLetter`). `MissionActions` la extiende y
  `KeyerPad` la recibe.
- `FreeSendViewModel(settings, sidetones, clock)`:
  - estado `FreeSendUiState(text, pending, wpm, keyerMode, keyerSkin)`;
  - acciones `keyDown`, `keyUp`, `tick`, `dot`, `dash`, `nextLetter`, `deleteLast`, `clear`,
    `enter()` y `leave()`.
- `FreeSendScreen` (ruta `workshop_send`).
- [ ] Pruebas:
  - `free_send_decodes_letters_and_words`;
  - `free_send_shows_speed`;
  - `free_send_exit_stops_tone`;
  - `free_send_text_capped`.

### Task 2: Letras a la carta
- `game`: `LetterDrill(selected, random)` con `next()` (nunca repite seguida), `answer(c)` y `stats`.
- `LetterDrillViewModel`, que guarda cada respuesta en `charStats.record(…, gentle = true)`, y
  `LetterDrillScreen` (ruta `workshop_letters`).
- [ ] Pruebas:
  - `drill_never_repeats_consecutively`;
  - `drill_needs_two_letters`;
  - `drill_records_outcomes`;
  - `drill_defaults_last_learned`.

### Task 3: Taller en Práctica
- Sección «Taller del farero» con las dos tarjetas encima de los minijuegos.
- [ ] Prueba `practice_shows_workshop`.

### Task 4: Biblioteca ampliada
- 8 fichas y sus fuentes. Se actualizan la cuenta de fichas del README y la ficha de Play.
- [ ] Pasan `RealContentTest` y `ContentValidator`.

### Task 5: 0.10.0, documentación, revisión y entrega
- Versión, README y `docs/pruebas/etapa-8.md`.
- Suite completa, lint, APK y AAB firmados, revisión independiente, arreglos, commit, push y entrega.
