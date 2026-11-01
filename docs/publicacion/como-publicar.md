# Cómo publicar «MorsePrende: El Faro» en Google Play

Guía paso a paso para cuando decidas publicar. Nada de esto está hecho todavía: la app solo se ha
distribuido como APK instalado a mano.

## Lo que ya está preparado

| Qué | Dónde |
|---|---|
| Paquete para Play (AAB firmado) | `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` |
| Textos de la ficha y respuestas de los formularios | `docs/publicacion/ficha-google-play.md` |
| Política de privacidad | `docs/publicacion/politica-privacidad.md` |
| Icono 512, gráfico destacado y 7 capturas | `docs/publicacion/graficos/` |
| Créditos y licencias | `CREDITS.md` y, dentro de la app, Ajustes → Acerca de → Licencias |

La clave de firma es la misma de los APK que ya instalas (huella SHA-256
`86:62:F8:62:…:FC:A6:37`, RSA de 4096 bits, válida hasta 2126). Vive solo en las variables de entorno
`MORSEPRENDE_KEYSTORE_B64` y `MORSEPRENDE_KEYSTORE_PASSWORD`.

## Pasos

Datos comprobados en noviembre de 2026; Google los cambia a menudo, así que Play Console manda.

1. **Cuenta de desarrollador.** Entra en Play Console (play.google.com/console) con tu cuenta de Google
   y regístrate como desarrollador:
   - pago único de 25 US$;
   - verificación de identidad (puede pedir un documento oficial y una tarjeta a tu nombre);
   - en cuentas personales, verificar un móvil Android con la app Play Console.

   Las cuentas personales creadas después del 13 de noviembre de 2023 tienen que hacer una **prueba
   cerrada con al menos 12 probadores durante 14 días seguidos** antes de pedir acceso a producción.
   Esa petición la revisa Google en unos 7 días o menos.
2. **Crear la app.**
   - Nombre: «MorsePrende: El Faro».
   - Idioma predeterminado: español (España).
   - Tipo: Juego (o App).
   - Gratis.
   - Acepta las declaraciones.
3. **Firma (decisión que no se puede deshacer).** Play App Signing es obligatorio: Google firma lo que
   descargan los usuarios. Antes de la primera versión en prueba abierta o en producción, en
   «Integridad de la app → Firma de apps», hay dos opciones:
   - **Recomendada: subir tu clave actual como clave de firma de la app** («Usar una clave distinta» →
     exportar y subir la clave con la herramienta que indica Play Console). Así, las versiones de Play
     **se instalan encima del APK que ya usas, sin desinstalar ni perder el progreso**. Después crea
     una **clave de subida** nueva, distinta (Google lo recomienda), y úsala para firmar los AAB que
     subas.
   - Dejar que Google genere la clave. Es más sencillo, pero las versiones de Play no podrán
     actualizar el APK instalado a mano: habría que desinstalarlo, y eso borra el progreso.

   Tras la primera versión pública ya no se puede cambiar la clave de firma de la app.
4. **Política de privacidad.** Play pide una dirección web pública; dentro de la app ya se puede leer
   (Ajustes → Acerca de). Opciones para la web:
   - Si el repositorio es público: el enlace al fichero en GitHub
     (`…/blob/main/docs/publicacion/politica-privacidad.md`) o GitHub Pages.
   - Si es privado: cópiala en una página gratuita (por ejemplo, Google Sites).
   - Antes de publicar, cambia `‹tu correo de contacto›` por un correo real **en el fichero** y vuelve
     a compilar: así la versión de la app también lo lleva.
5. **Ficha de Play Store.** Copia los textos de `ficha-google-play.md` y sube los gráficos de
   `graficos/`.
6. **Contenido de la app.** Rellena los formularios con las respuestas de `ficha-google-play.md`:
   - política de privacidad;
   - anuncios;
   - acceso a la app;
   - clasificación de contenido;
   - público objetivo (decide entre la opción A y la B);
   - seguridad de los datos;
   - ID de publicidad;
   - el resto de declaraciones.
7. **Prueba interna.** En Pruebas → Prueba interna, crea una versión y sube el AAB. Añade tu correo
   como probador, instálala desde el enlace de Play y repasa las listas de `docs/pruebas/`.
8. **Prueba cerrada y producción.** Con una cuenta personal nueva, la prueba cerrada (12 probadores, 14
   días) es obligatoria. Después pide acceso a producción y publica. La revisión de cada versión puede
   tardar unos días.

## Requisitos que cambian cada año

- **Nivel de API objetivo:** desde el 31 de agosto de 2026, las apps nuevas y las actualizaciones deben
  apuntar a la API 36. La app ya lo hace (`targetSdk = 36`). Cada agosto sube un nivel (probablemente
  la 37 en 2027).
- **Verificación de desarrolladores para APK instalados a mano:** Android empezará a exigir que las
  apps instaladas fuera de Play sean de un desarrollador registrado (desde septiembre de 2026 en
  algunos países y en todo el mundo en 2027). Registrarse en Play Console cubre esto; el paquete
  `io.github.egt17baa.morseprende` quedará asociado a tu cuenta.

## Cada nueva versión

1. Sube `versionCode` (siempre mayor que el anterior) y `versionName` en `app/build.gradle.kts`.
2. `./gradlew :app:bundleRelease` con las variables de la clave cargadas.
3. Comprueba la huella: `keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab`.
   (Si usas una clave de subida distinta, firma el AAB con ella; el script de compilación firma con
   la clave de las variables de entorno.)
4. Súbelo como nueva versión con sus novedades.

## Si pierdes la clave

- **La clave de subida** (la que firma los AAB que subes): en Play Console, «Solicitar el
  restablecimiento de la clave de subida». Creas una clave nueva, la exportas en PEM y Google la
  activa en unos días. La app sigue actualizándose.
- **La clave actual de los APK instalados a mano** (si la subiste como clave de firma, Google guarda
  una copia y Play sigue funcionando): sin ella no podrías firmar APK que se instalen encima de los
  que ya tienes; habría que desinstalar, y eso borra el progreso. Guarda una copia de la clave y su
  contraseña en un lugar seguro, fuera del repositorio (por ejemplo, un gestor de contraseñas).
