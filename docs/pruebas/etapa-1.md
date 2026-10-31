# Pruebas en el móvil: Etapa 1

Móvil de referencia: **Honor Magic 8 Pro** (MagicOS, Android 16).

## Instalar

1. Copia `morseprende-etapa1-debug.apk` al móvil y ábrelo.
2. Si lo pide, permite «Instalar apps desconocidas» para la app con la que lo abres (Archivos, Chrome…).
3. Al ser una versión de pruebas, Play Protect puede avisar: elige «Instalar de todas formas».

> Esta versión va firmada con una clave temporal. Para instalar la de la Etapa 2 habrá que
> desinstalar esta primero (todavía no guarda progreso, así que no se pierde nada).

## Qué probar (marca cada punto)

- [ ] El icono es un faro con un haz de luz, y la app se llama «MorsePrende: El Faro».
- [ ] En la pantalla principal el haz del faro se mueve suavemente.
- [ ] **Traductor → Sonido:** escribe `SOS` y pulsa Reproducir. Se oye `··· ‒‒‒ ···` limpio, sin clics ni chasquidos.
- [ ] **Ajustes:** cambia el tono y la velocidad, pulsa «Probar sonido» y nota la diferencia.
- [ ] **Vibración:** activa solo «Vibración» y reproduce `SOS`. Si apenas se nota, revisa en MagicOS
      Ajustes › Sonido y vibración › Intensidad de vibración.
- [ ] **Linterna:** al activarla por primera vez sale un aviso de destellos. Acéptalo, reproduce
      `SOS` y comprueba que el flash parpadea y **queda apagado** al terminar.
- [ ] **Pantalla:** la pantalla destella en color crema al ritmo del Morse.
- [ ] Con linterna o pantalla activas aparece el aviso «Velocidad limitada a 7 PPM».
- [ ] Pulsa Reproducir con un texto largo y **sal de la pantalla** (botón atrás) mientras suena:
      el sonido, la vibración y la linterna se paran.
- [ ] Pulsa Reproducir dos veces seguidas: se reinicia, no se superponen dos sonidos.
- [ ] Pega un texto con tildes y emojis, por ejemplo `¡Hola, ñandú! 😀`: la Ñ se traduce
      (`‒‒·‒‒`), las tildes se ignoran y aparece el aviso de caracteres omitidos.
- [ ] **Invertir:** pasa a Morse → texto, escribe `.... --- .-.. .-` y sale `HOLA`.
- [ ] **Copiar:** copia el Morse y pégalo en otra app.
- [ ] Gira el móvil: la app se queda en vertical.
- [ ] Modo avión activado: todo funciona igual (la app no usa internet).

## Si algo falla

Anota qué estabas haciendo, qué esperabas y qué pasó (una captura o un vídeo ayuda mucho).
