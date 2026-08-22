# Llegue

Llegue avisa a alguien de confianza dónde estás, enviando tu ubicación por SMS.

No necesita internet en ningún extremo, y quien recibe el aviso no necesita tener
la app instalada: le llega un mensaje de texto común con un link al mapa.

## Cómo funciona

Creás una sesión de viaje con un nombre, un único contacto de confianza y, si
querés, cada cuánto avisar. El intervalo es opcional y el mínimo es de 5 minutos.
La sesión puede terminar sola después de una duración que elijas, o quedar
abierta hasta que la cierres.

Mientras la sesión está activa, la app manda tu ubicación por su cuenta en los
intervalos que definiste. El contacto además puede pedirla en cualquier momento
respondiendo el SMS con la palabra clave que se acordó en el primer mensaje, sin
esperar al próximo envío. Esa respuesta funciona incluso en sesiones creadas sin
intervalo, donde los avisos automáticos no existen.

Cada mensaje que recibe el contacto tiene cuatro líneas: la palabra clave, el
link a Google Maps con tu ubicación, cuándo será el próximo aviso y el nivel de
batería de tu teléfono. En las sesiones sin intervalo se omite la línea del
próximo aviso, porque no hay uno.

## Estado

En desarrollo. Por ahora está hecha la base del proyecto: se demolió la interfaz
heredada, se migró a AndroidX apuntando a Android 16 (API 36) y se rehizo la
marca. Todavía faltan el modelo de sesiones, el programador de envíos y las
pantallas definitivas.

El MVP deja fuera, de forma deliberada, la elección de un destino en el mapa y el
acceso directo desde la notificación.

## Requisitos

En el teléfono, Android 8.0 (API 26) o superior, con línea capaz de enviar SMS.
El contacto que recibe los avisos solo necesita poder recibir mensajes de texto.

## Cómo compilar

Requiere Android Studio con JDK 17 o superior y el SDK de Android 36.

```
./gradlew :app:assembleDebug
```

El APK aparece en `app/build/outputs/apk/debug`.

Para generar un APK firmado:

1. Crear el directorio `privatedata` dentro de `app`.
2. Poner ahí el keystore.
3. Crear `app/privatedata/private.properties` con las propiedades
   `llegue.storeFile`, `llegue.storePassword`, `llegue.keyAlias` y
   `llegue.keyPassword`.
4. Ejecutar `LLEGUE_PUBLIC_RELEASE=true ./gradlew assembleRelease`.

## Licencia

Llegue es software libre bajo la GNU General Public License v3, y deriva del
proyecto [Open SMS Locator](https://github.com/rescue-sms-tracker), publicado
bajo la misma licencia. Ver el archivo `LICENSE`.
