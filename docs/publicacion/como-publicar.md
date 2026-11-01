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

1. **Cuenta de desarrollador.** Entra en Play Console (play.google.com/console) con tu cuenta de
   Google y regístrate como desarrollador. Hay un pago único de registro y Google pide verificar tu
   identidad. Las cuentas personales nuevas tienen que hacer una **prueba cerrada** con un grupo de
   personas durante un tiempo antes de poder publicar en producción: Play Console te indica el número
   de probadores y los días que pide en ese momento.
2. **Crear la app.**
   - Nombre: «MorsePrende: El Faro».
   - Idioma predeterminado: español (España).
   - Tipo: Juego (o App).
   - Gratis.
   - Acepta las declaraciones.
3. **Firma.** Al subir el primer AAB, acepta **Play App Signing**: Google guarda su propia clave de firma
   y la tuya pasa a ser la **clave de subida**. Ventaja: si algún día pierdes tu clave, Google puede
   cambiar la clave de subida sin que la app deje de actualizarse.
   - Ojo: los APK firmados por Google serán distintos de los que instalas a mano con tu clave. Para
     pasar de uno a otro en un mismo móvil hay que desinstalar antes, y eso borra el progreso.
4. **Política de privacidad.** Play pide una dirección web pública. Opciones:
   - Si el repositorio es público: el enlace al fichero en GitHub
     (`…/blob/main/docs/publicacion/politica-privacidad.md`) o GitHub Pages.
   - Si es privado: cópiala en una página gratuita (por ejemplo, Google Sites) y pon esa dirección.
   - Antes de publicarla, cambia `‹tu correo de contacto›` por un correo real.
5. **Ficha de Play Store.** Copia los textos de `ficha-google-play.md` y sube los gráficos de
   `graficos/`.
6. **Contenido de la app.** Rellena los formularios con las respuestas de `ficha-google-play.md`:
   - política de privacidad;
   - anuncios;
   - acceso a la app;
   - clasificación de contenido;
   - público objetivo;
   - seguridad de los datos;
   - y el resto de declaraciones.
7. **Prueba interna.** En Pruebas → Prueba interna, crea una versión y sube el AAB. Añade tu correo
   como probador, instálala desde el enlace de Play y repasa las listas de `docs/pruebas/`.
8. **Prueba cerrada y producción.** Cuando la prueba interna vaya bien, pasa a prueba cerrada (si tu
   cuenta lo exige) y después a producción. La primera revisión de Google puede tardar unos días.

## Cada nueva versión

1. Sube `versionCode` (siempre mayor que el anterior) y `versionName` en `app/build.gradle.kts`.
2. `./gradlew :app:bundleRelease` con las variables de la clave cargadas.
3. Comprueba la huella: `keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab`.
4. Súbelo como nueva versión con sus novedades.

## Si pierdes la clave

- **Con Play App Signing** (paso 3): pide en Play Console que cambien la clave de subida. La app
  sigue actualizándose.
- **Sin Play App Signing**: no se podrían publicar actualizaciones de la misma app. Por eso conviene
  guardar una copia de la clave y su contraseña en un lugar seguro, fuera del repositorio (por
  ejemplo, un gestor de contraseñas).
