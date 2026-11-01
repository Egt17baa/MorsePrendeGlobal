# Etapa 6: preparar la publicación. Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dejar la app lista para Google Play, sin publicar nada (spec §3: «Debe quedar **preparado**
para publicarse más adelante»). Hace falta:
- créditos y licencias completos, también dentro de la app;
- política de privacidad;
- textos de la ficha de Play y respuestas a los formularios;
- gráficos de la tienda;
- un AAB firmado;
- una guía paso a paso para publicar.

**Architecture:**
- **Licencias:** pantalla en Ajustes que lee `assets/licenses/*.txt`.
- **Documentos:** en `docs/publicacion/`.
- **Gráficos:** salen de una prueba Robolectric que solo se ejecuta si se pasa `-PgraficosDir=…`,
  y se guardan en `docs/publicacion/graficos/`.

**Tech Stack:** Kotlin, Compose, Robolectric (gráficos nativos) y Gradle `bundleRelease`.

**Spec:** `docs/superpowers/specs/2026-10-31-juego-morse-faro-design.md` §3, §5 y §9.

## Global Constraints

- Las de siempre:
  - sin `INTERNET`, solo `VIBRATE`;
  - textos de interfaz en `strings.xml`;
  - `applicationId` `io.github.egt17baa.morseprende` (no cambia nunca);
  - clave de firma solo en variables de entorno.
- **Versión:** `versionCode 11`, `versionName "0.8.0"`.
- **Recursos de terceros:** registrados en `CREDITS.md` (spec §5). Atkinson Hyperlegible va con OFL
  1.1, y las bibliotecas AndroidX, Kotlin y kotlinx con Apache 2.0.
- **Nunca** se escriben datos personales del usuario en el repositorio (correo, nombre real). Donde Play
  pida un contacto, se deja el hueco `‹tu correo de contacto›`.
- **Límites de Play:**
  - título ≤ 30 caracteres, descripción breve ≤ 80 y descripción completa ≤ 4000;
  - icono PNG de 512 × 512;
  - gráfico destacado de 1024 × 500;
  - capturas de 1080 × 1920 (9:16), entre 2 y 8.
- **Formularios:**
  - Seguridad de los datos: la app no recoge ni comparte datos.
  - Anuncios: no.
  - Compras en la app: no.
  - Público objetivo: se recomienda 13+, con la alternativa «todas las edades» explicada, porque la
    app no tiene anuncios ni datos.

## Review Focus

1. **AAB firmado con la misma clave que los APK instalados a mano.** Se espera que la huella sea
   `8662f862…ca637`. Lo comprueba `keytool -printcert -jarfile` en la Task 4.
2. **Textos de la ficha dentro de los límites de Play.** Se espera que ningún campo los pase. Lo
   prueba `store_listing_within_limits`, que lee el fichero de la ficha.
3. **Pantalla de licencias con texto muy grande.** Se espera que se pueda leer y desplazar entera.
   Lo prueba `licenses_screen_shows_all`.
4. **Gráficos con el tamaño exacto.** Se espera que Play no los rechace. Lo prueba la propia prueba de
   gráficos, que comprueba las dimensiones de cada PNG.
5. **Ninguna referencia a la app de referencia ni a marcas ajenas en la ficha (spec §4.1).** Se
   espera que no aparezcan. Revisión manual, más una lista de palabras prohibidas en
   `store_listing_within_limits`.

---

### Task 1: Créditos y pantalla de licencias
- `CREDITS.md`: la fuente (autor, licencia y ruta) y las bibliotecas con su licencia.
- `assets/licenses/apache-2.0.txt` (texto completo) y `assets/licenses/bibliotecas.txt` (lista).
- `LicensesScreen` (ruta `licenses`), abierta desde Ajustes → Acerca de → «Licencias de código abierto».
  Muestra cada fichero de `assets/licenses/` con su título, en una columna desplazable.
- [ ] Prueba `licenses_screen_shows_all`: aparecen «SIL Open Font License» y «Apache License», y el
  final del texto Apache (`END OF TERMS AND CONDITIONS`) se alcanza con `performScrollTo`.
- [ ] Prueba `about_opens_licenses`: el botón llama a `onLicenses`.

### Task 2: Política de privacidad y ficha de Play
- `docs/publicacion/politica-privacidad.md`: en español y con resumen en inglés. Cubre qué se guarda
  (solo en el móvil), la ausencia de red, cuentas y analíticas, los permisos (vibrar), la copia de
  seguridad de Android, los menores y el contacto.
- `docs/publicacion/ficha-google-play.md`, con los campos en bloques `<!-- campo:nombre -->…<!-- /campo -->`:
  - título, descripción breve y descripción completa;
  - categoría Educación y etiquetas;
  - respuestas de clasificación de contenido (IARC), Seguridad de los datos y público objetivo;
  - novedades de la versión.
- [ ] Prueba JVM `store_listing_within_limits` en `game` (lee el fichero desde la raíz del repositorio):
  - título ≤ 30, breve ≤ 80 y completa ≤ 4000;
  - no aparecen las palabras prohibidas (`Morse Mania`, `Morse Code Master`, `gratis*`) ni mayúsculas
    gritadas de más de 6 letras seguidas;
  - incluye «sin anuncios» y «sin conexión».

### Task 3: Gráficos de la tienda
- `app/src/test/.../store/StoreGraphicsTest.kt` con `@GraphicsMode(NATIVE)`. Se salta
  (`assumeTrue`) si la propiedad de sistema `graficos.dir` está vacía. `app/build.gradle.kts` la pasa
  desde `-PgraficosDir`.
- Genera:
  - `icono-512.png`, con el fondo y el primer plano del icono adaptativo, recortados a la zona segura;
  - `destacado-1024x500.png`, con el faro, el nombre y el lema «Aprende Morse jugando»;
  - 6 capturas de 1080 × 1920: Faro, recibir, enviar con el manipulador, biblioteca, Tormenta y traductor.
- [ ] Cada PNG se comprueba con `BitmapFactory`: 512 × 512, 1024 × 500 y 1080 × 1920.
- [ ] Generar en `docs/publicacion/graficos/` y revisarlas a ojo.

### Task 4: AAB, guía y entrega 0.8.0
- `./gradlew :app:bundleRelease`. Se comprueba la firma con `keytool -printcert -jarfile app-release.aab`.
- `docs/publicacion/como-publicar.md`, con los pasos:
  1. crear la cuenta de desarrollador;
  2. crear la app;
  3. activar Play App Signing con esta clave como clave de subida;
  4. hacer una prueba interna;
  5. rellenar los formularios con la ficha;
  6. subir los gráficos y alojar la política.

  Incluye también qué hacer si se pierde la clave.
- README: estado de la Etapa 6 y enlace a `docs/publicacion/`. `docs/pruebas/etapa-6.md` (pantalla de
  licencias).
- [ ] Suite completa, lint, APK y AAB firmados, sin `INTERNET`. Revisión independiente y entrega
  (APK + AAB + documentos).
