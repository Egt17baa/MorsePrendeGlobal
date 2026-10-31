# Diseño: "MorsePrende: El Faro", juego educativo de código Morse para Android

Fecha: 2026-10-31 · Estado: borrador para aprobación

> Convención: lo marcado **[decisión del usuario]** lo decidió el usuario en la conversación.
> Lo marcado **[propuesta]** es una decisión tomada por el diseñador que el usuario puede corregir.
> Al aprobar este documento, las propuestas que el usuario no corrija quedan aceptadas.

---

## 1. Intención y porqué

Un videojuego para Android que haga que **cualquier persona quiera aprender código Morse jugando**
[decisión del usuario]. No es un curso con adornos: debe **enganchar** como un juego, y a la vez
enseñar de verdad a recibir (oír/ver Morse y entenderlo) y a enviar (transmitirlo uno mismo).
Además, debe contar **qué es el Morse, cómo y quién lo creó y su historia**, de forma rápida y amena,
para que se aprenda "en todos los aspectos" [decisión del usuario].

Referencia de partida: la app "Código Morse Completo: Aprende" (Sparks Digital Studio). Se toma
**solo como referencia de modos de juego** (lecciones progresivas por letras, recibir/enviar,
práctica, traductor). **Todo lo demás debe ser propio y claramente distinto** para evitar cualquier
problema legal [decisión del usuario] (ver §4.1).

## 2. Objetivos

1. Que un principiante total aprenda las 26 letras, los números y la puntuación básica, recibiendo
   y enviando, a lo largo de una campaña con historia.
2. Que quiera volver: progreso visible, recompensas, récords, historia que avanza.
3. Que entienda el contexto histórico y cultural del Morse (biblioteca).
4. Que tenga una herramienta útil: traductor texto ⇄ Morse con sonido, luz y vibración.

## 3. No-objetivos (v1)

- Nada en red: sin internet, sin cuentas, sin servidores, sin multijugador, sin ranking online
  [decisión del usuario].
- Sin anuncios, sin compras, sin donaciones; totalmente gratis [decisión del usuario].
- Sin publicación en Google Play por ahora; se distribuye como APK instalado a mano
  [decisión del usuario]. Debe quedar **preparado** para publicarse más adelante.
- Sin iOS, web ni escritorio.
- Sin analíticas ni telemetría de ningún tipo.
- Sin notificaciones push/recordatorios en v1 [propuesta: evita presión y permisos; se puede añadir].
- Sin decodificar Morse captado por el micrófono o la cámara en v1 [propuesta: es una ampliación
  atractiva pero compleja].

## 4. Anti-objetivos (sería un fracaso aunque funcione)

### 4.1 Parecerse a la app de referencia
Prohibido reutilizar o imitar: su nombre, icono (círculo con punto y raya), paleta (negro/dorado,
azul marino/cian neón), tipografía, textos, capturas, estructura visual de pantallas (lista vertical
de "Lección N" con botones "Recepción/Envío"), iconos de corona/antena, ni su orden de letras
(E&T, A&N, I&M…). Nuestra estructura es un **mapa/campaña con misiones narrativas**, no una lista.

### 4.2 Otros
- Que se sienta como un examen o una obligación (castigos, culpa por perder rachas, muros de pago).
- Que el Morse suene mal: pitidos con clics, tiempos irregulares o latencia perceptible al enviar.
- Que pida permisos innecesarios. El manifiesto **no** debe declarar `INTERNET`.
- Contenido histórico inventado o erróneo en la biblioteca.
- Riesgo fotosensible: destellos de pantalla/linterna sin aviso ni opción de limitarlos.

## 5. Restricciones

- Plataforma: Android, solo teléfono (vertical) [decisión del usuario: "en principio en el móvil"].
- Nombre: **MorsePrende: El Faro**; identificador (applicationId):
  **`io.github.egt17baa.morseprende`** [decisión del usuario]. No debe cambiar nunca.
- Dispositivo de pruebas: **Honor Magic 8 Pro** del usuario (gama alta reciente, MagicOS sobre
  Android 16) [decisión del usuario]. MagicOS es agresivo cerrando apps en segundo plano y
  personaliza la vibración; hay que comprobar linterna y vibración ahí.
- Tecnología: **Kotlin nativo + Jetpack Compose** [decisión del usuario, opción A].
- 100 % offline; todos los datos guardados en el dispositivo.
- Idioma: **v1 solo en español**; todos los textos en recursos para añadir inglés después
  [propuesta, no corregida por el usuario]. El selector de idioma no aparece en Ajustes hasta
  que exista una segunda traducción completa (incluida la biblioteca).
- Recursos de terceros (fuentes, sonidos, imágenes) solo con licencia libre compatible
  (OFL, CC0, Apache 2.0) y registrados en un fichero `CREDITS`.
- Entorno de compilación disponible: JDK 21 y Gradle 8.14.3; el Android SDK no está instalado
  pero se puede descargar (dl.google.com accesible).

## 6. Enfoques considerados

| Enfoque | A favor | En contra |
|---|---|---|
| **A. Kotlin + Jetpack Compose** ✅ elegido | Acceso directo y fiable a audio de baja latencia, linterna y vibración; APK pequeña; gran parte del juego son pantallas (misiones, biblioteca, traductor) donde Compose brilla; minijuegos 2D sencillos con `Canvas` | Solo Android; un futuro iOS exigiría rehacer la UI |
| B. Godot 4 | Mejor "sensación de juego" (partículas, escenas); multiplataforma | Linterna requiere plugin nativo; pantallas tipo app más torpes; más latencia en audio generado |
| C. Flutter + Flame | Multiplataforma; motor 2D para minijuegos | Plugins de terceros para linterna/vibración; capa extra sin ventaja real aquí |

El usuario eligió **A** tras la recomendación, por ajustarse a Android-only, offline y a la necesidad
de que sonido/luz/vibración del Morse sean impecables.

## 7. El juego

### 7.1 Ambientación y campaña [decisión del usuario: faro + espías + espacio]

Una sola campaña en tres capítulos de dificultad creciente [propuesta aceptada en la conversación]:

1. **Capítulo 1 · El Faro.** Eres el nuevo farero de una isla aislada. Barcos, pescadores y
   náufragos se comunican contigo en Morse. Tutorial y primeras letras, mensajes lentos, mucha
   ayuda visual. Se aprende el SOS al principio.
2. **Capítulo 2 · Operación Sombra.** Bajo el faro hay una estación de escucha secreta. Interceptas
   transmisiones de espías y descubres a un topo. Resto de letras y números, mensajes más rápidos,
   ruido de radio, contrarreloj, algún mensaje trampa.
3. **Capítulo 3 · Señal Estelar.** Contactas con una estación orbital… y con algo desconocido.
   Puntuación y prosignos, frases completas, velocidad alta, señales que llegan solo como luz o solo
   como vibración.

El **faro es la base** (pantalla principal) durante todo el juego y se va mejorando.

### 7.2 Orden de enseñanza [propuesta]

Distinto del de la app de referencia y guiado por la historia. Dos caracteres nuevos por misión.

- **Cap. 1 (10 letras):** S O → E T → A N → I M → R D. Permite palabras como SOS, MAR, RED, ISLA*
  (*la L llega en el cap. 2; el constructor elige palabras con las letras ya vistas).
- **Cap. 2:** U K → W G → H L → F P → C B → J V → X Y → Z Q, luego números 0–9 (en parejas).
- **Cap. 3:** puntuación `. , ? / = ' !` y prosignos `AR SK BT` (con su significado), más misiones de
  velocidad y frases largas.
- **Ñ** (`--.--`, no forma parte del estándar ITU): misión extra opcional en el cap. 2, indicando en
  pantalla que es una extensión española no internacional.

Estándar de referencia: Morse internacional (Recomendación UIT‑R M.1677‑1).

### 7.3 Estructura de una misión

Cada misión es un encargo narrativo corto (p. ej. "Un pesquero pide rumbo") con estas fases:

1. **Escucha**: presentación de los caracteres nuevos con sonido, destello en pantalla y patrón
   visual. Sin trucos mnemotécnicos copiados; el constructor puede crear los suyos.
2. **Recibir**: oyes/ves un carácter o palabra y respondes. Empieza con opción múltiple y pasa a
   teclear la respuesta.
3. **Enviar**: transmites con el **manipulador** en pantalla (ver §7.4).
4. **Mensaje final**: un mensaje de la historia con todo lo aprendido, que hace avanzar la trama.

Puntuación de 1 a 3 estrellas según aciertos. Se puede repetir para mejorarla.
Cada 2–3 misiones hay una **misión de repaso** generada automáticamente (ver §7.6).

### 7.4 Enviar: el manipulador

- Modo por defecto: **un solo botón grande** (manipulador). Si la pulsación es más corta que
  2 unidades de tiempo es punto; si no, raya. Un silencio de 3 unidades cierra la letra y uno de
  7 unidades cierra la palabra.
- El umbral se **adapta** al ritmo real del jugador (media móvil de sus puntos y rayas) para no
  castigar a principiantes lentos.
- Modo alternativo de accesibilidad: **dos botones** (punto | raya) más un botón "siguiente letra".
- Respuesta inmediata: sonido mientras se pulsa (latencia objetivo < 30 ms), vibración ligera
  y dibujo del patrón en tiempo real.

### 7.5 Minijuegos arcade (práctica libre) [propuesta]

Se desbloquean al avanzar y usan solo los caracteres ya aprendidos. Récords locales.

1. **Tormenta de señales**: señales Morse caen hacia el mar y hay que escribir su letra antes de que
   se hundan. La velocidad aumenta.
2. **Destello rápido**: el faro emite un destello y eliges la letra lo antes posible (reflejos).
3. **Interceptación**: copiar palabras a velocidad creciente sin fin hasta fallar 3 veces.

### 7.6 Repaso inteligente [propuesta]

Repetición espaciada por carácter (sistema de cajas de Leitner, 5 cajas). Los caracteres en los que
más se falla aparecen más en los repasos y en los minijuegos.

### 7.7 Progreso y recompensas [propuesta]

- Moneda del juego: **Destellos**, que se ganan con estrellas, repasos y récords. **Solo sirven para
  cosas estéticas** (color de la luz del faro, sonido del pitido, aspecto del manipulador,
  decoración de la isla). No hay compras reales.
- **Racha diaria** suave: se muestra, pero perderla no tiene castigo. Hay un "día de descanso"
  automático por semana.
- **Logros** (p. ej. "Primer SOS", "Topo descubierto", "20 PPM").
- El faro y la isla cambian visualmente al avanzar.

### 7.8 Biblioteca del faro (pestaña de lectura) [decisión del usuario; formato: propuesta]

Fichas cortas (de 150 a 300 palabras cada una, con una ilustración propia y un "¿Sabías que…?").
Las 3 primeras están disponibles desde el principio y el resto se desbloquean al jugar,
ambientadas por capítulo. Contenido mínimo:

- Qué es el código Morse y cómo funciona (puntos, rayas, tiempos).
- Samuel Morse y Alfred Vail; el telégrafo eléctrico; el primer mensaje oficial (1844).
- Morse americano frente a internacional.
- El SOS: origen, adopción internacional y el Titanic.
- El Morse en el mar y los faros; el fin de su uso obligatorio en la navegación (GMDSS, 1999).
- Radio y guerra; espionaje y mensajes cifrados.
- Jeremiah Denton parpadeando "TORTURE" (1966).
- Radioaficionados hoy.
- Morse y espacio/radio moderna, y su uso en accesibilidad (personas con movilidad reducida).
- Cómo aprender Morse: el método de Koch y el espaciado de Farnsworth.

**Requisito:** cada dato se verifica con una fuente fiable antes de incluirlo. Las fuentes se
apuntan en un fichero interno del proyecto (no hace falta mostrarlas en la app).
Fuentes válidas [propuesta]: enciclopedias de referencia (Britannica), instituciones
(Smithsonian, Library of Congress, UIT, NASA, ARRL/URE) y prensa o libros reconocidos; no foros ni
blogs sin fuentes. El constructor investiga con búsqueda web y el usuario revisa las fichas antes
de dar el capítulo por terminado.

### 7.9 Utilidades

- **Traductor** texto ⇄ Morse en ambos sentidos, con botones para copiar y pegar.
- Reproducir el resultado por **sonido**, **linterna**, **destello de pantalla** y/o **vibración**.
- Velocidad ajustable desde la propia pantalla.
- Diseño propio, distinto de la referencia (ver §4.1).

### 7.10 Ajustes

- Velocidad de carácter (5–30 PPM), espaciado Farnsworth (velocidad efectiva) y tono
  (400–1000 Hz, 600 por defecto).
- Volumen, activar o desactivar la vibración, modo de manipulador (1 o 2 botones).
- Accesibilidad: alto contraste, tamaño de texto, **limitar destellos**.
- Borrar progreso. (Idioma: solo cuando haya inglés, ver §5.)

## 8. Diseño visual y sonoro [propuesta]

- Estilo ilustrado plano, cálido y nocturno, dibujado con vectores y `Canvas`, con paleta propia por
  capítulo:
  - Faro: verde azulado de mar, crema de la luz y coral.
  - Sombra: verde oliva, ámbar de válvula de radio y gris.
  - Estelar: violeta profundo, blanco estelar y magenta.
  Debe distinguirse claramente de la referencia (negro/dorado y azul/cian neón).
- Tipografía con licencia OFL incluida en el APK (sin descargas).
- Icono propio: el faro emitiendo un haz que forma un punto y una raya. Debe evitar el círculo con
  punto y raya de la referencia.
- Sonido de Morse **generado por síntesis**: onda senoidal con subida y bajada suaves (~5 ms) para
  evitar clics. Efectos y música, si los hay, propios o CC0.

## 9. Arquitectura técnica (lo decidido)

- Kotlin, Jetpack Compose, Material 3 con tema totalmente personalizado, una sola Activity y
  Navigation Compose. Patrón MVVM con flujo de datos unidireccional.
- **Núcleo Morse** como módulo puro de Kotlin sin dependencias de Android, totalmente probado:
  tabla de códigos, codificar y decodificar, cálculo de tiempos (unidad = 1200 / PPM ms; Farnsworth)
  y clasificador de pulsaciones del manipulador.
- **Audio**: `AudioTrack` con PCM generado y modo de baja latencia
  (`PERFORMANCE_MODE_LOW_LATENCY`, flujo continuo que se activa y silencia con envolvente, en vez
  de crear un sonido por pulsación). Al enviar, el tono suena mientras se pulsa. El objetivo de
  < 30 ms es el mejor esfuerzo, no un requisito bloqueante. **Sin NDK/Oboe en v1** [propuesta]: solo
  se plantearía si en las pruebas reales el retardo resulta molesto.
- **Linterna**: `CameraManager.setTorchMode` (no requiere permiso de cámara). Si el dispositivo no
  tiene flash, se oculta la opción.
- **Vibración**: `Vibrator`/`VibratorManager` (permiso `VIBRATE`, de tipo normal).
- **Datos**: Room (progreso, estrellas, cajas de Leitner, récords, logros) y DataStore (ajustes).
- **Contenido** (misiones, diálogos, fichas de la biblioteca) en ficheros de datos y recursos de
  texto localizables, separado del código, para poder ampliarlo sin tocar la lógica.
- Permisos: solo `VIBRATE`. **Sin `INTERNET`.**
- minSdk 26, targetSdk/compileSdk = el último estable al empezar [propuesta].
- Copia de seguridad automática de Android activada (no hay datos sensibles) [propuesta].
- Distribución: APK de release firmado con una clave local que **no se sube al repositorio**.

## 10. Calidad y pruebas

- Pruebas unitarias del núcleo Morse (tabla completa, ida y vuelta, tiempos, clasificador con
  ritmos variados), del planificador de repaso y de las reglas de puntuación y desbloqueo.
- Pruebas de ViewModel para el flujo de una misión.
- Algunas pruebas de UI con Compose para los flujos principales (abrir misión, completar, guardar).
- Revisión manual en un dispositivo real del sonido, la latencia del manipulador, la linterna y
  la vibración. **La hace el usuario en su propio móvil**, instalando el APK que se genera en cada
  hito; el constructor no tiene dispositivo ni emulador con hardware real. Por eso cada hito debe
  terminar con un APK instalable y una lista breve de cosas que probar.
- Compilación en este entorno: se instalan las herramientas de línea de comandos del Android SDK.
  Las pruebas unitarias corren en la JVM; las de UI con Compose se ejecutan con Robolectric.

## 11. Lo que se deja al constructor

- Textos concretos de la historia, diálogos y nombres de personajes, respetando la ambientación de §7.1.
- Número exacto de misiones por capítulo (orientativo: unas 8–10 en el cap. 1, 12–16 en el cap. 2
  y 8–10 en el cap. 3).
- Palabras y mensajes de cada misión, usando solo los caracteres ya aprendidos.
- Detalles de las ilustraciones, animaciones y diseño de cada pantalla, dentro del estilo de §8.
- Valores finos de puntuación, recompensas y dificultad de los minijuegos.
- Organización interna de módulos y paquetes.
- Orden de construcción por hitos. Se recomienda empezar por el núcleo Morse y el traductor,
  seguir con el faro, el capítulo 1 y la biblioteca, después los minijuegos y por último los
  capítulos 2 y 3, de modo que haya algo jugable cuanto antes.
