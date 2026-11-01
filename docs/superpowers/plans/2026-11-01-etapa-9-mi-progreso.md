# Etapa 9: Mi progreso. Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que el jugador vea cuánto ha avanzado y qué le cuesta, y que pueda practicar eso con un toque.
Es motivación sin castigos (spec §4.2) y aprovecha el repaso inteligente (§7.6), que ya guarda una
caja de Leitner por letra.

**Architecture:**
- **`:game`:** `Mastery` traduce la caja de cada letra a cuatro niveles: sin datos, aprendiendo, bien
  y dominada. También elige las letras más flojas.
- **App:** `StatsViewModel` combina progreso, cajas, récords, mensajes del día y días jugados.
  `StatsScreen` (ruta `stats`) se abre desde un chip «Mi progreso» del Faro. «Practicar estas letras»
  abre Letras a la carta con esas letras ya elegidas (ruta `workshop_letters?letras=…`).

## Global Constraints

- Las de siempre:
  - sin `INTERNET`;
  - textos en `strings.xml`;
  - sin regex en el código de la app;
  - sin cambios de esquema;
  - APK firmado.
- **Versión:** `versionCode 17`, `versionName "0.11.0"`.
- **El nivel no depende solo del color:** cada letra lleva de 0 a 3 marcas y una descripción para
  TalkBack.
- **Letras flojas:**
  - solo entre las aprendidas que siguen en «aprendiendo» (cajas 1 y 2; tras la revisión, las que ya
    van «bien» no se señalan), ordenadas por caja (la más baja primero) y, si empatan, por las más
    recientes;
  - como máximo 4;
  - no se muestran si hay menos de 2.
- **Solo información:** nada de esta pantalla resta ni castiga.

## Review Focus

1. **Jugador nuevo sin datos.** Se espera que la pantalla no falle y que no muestre letras flojas. Lo
   prueba `stats_new_player`.
2. **Letras preelegidas que no están aprendidas** (por ejemplo, tras borrar el progreso). Se espera
   que se ignoren y se use la elección por defecto. Lo prueba `drill_initial_selection_filtered`.
3. **Misiones extra.** Se espera que cuenten en el total, igual que en el logro «Farero legendario».

---

### Task 1: `Mastery` (`:game`)
- [ ] Pruebas:
  - `mastery_levels`;
  - `weakest_lowest_boxes_recent_first`;
  - `weakest_needs_two`.

### Task 2: `StatsViewModel`, `StatsScreen`, el chip del Faro y la preelección en Letras a la carta
- [ ] Pruebas:
  - `stats_summary_counts`;
  - `stats_new_player`;
  - `stats_letters_levels`;
  - `drill_initial_selection`;
  - `drill_initial_selection_filtered`;
  - `stats_practice_opens_drill` (UI);
  - `hub_opens_stats`.

### Task 3: 0.11.0, documentación, revisión y entrega
