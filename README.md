# Llegue

Llegue avisa a alguien de confianza dónde estás, enviando tu ubicación por SMS.

No necesita internet en ningún extremo, y quien recibe el aviso no necesita
tener la app instalada: le llega un mensaje de texto común con un link al mapa.

## Cómo funciona

Activás una sesión de viaje eligiendo un único contacto de confianza, un nombre
para el viaje y cada cuánto querés que se avise. A partir de ahí la app manda tu
ubicación sola, en los intervalos que definiste. El contacto también puede
pedirla en cualquier momento enviándote un SMS con la palabra clave que acordaron
en el primer mensaje.

Cada aviso que recibe el contacto incluye el link a Google Maps, cuándo será el
próximo aviso y el nivel de batería de tu teléfono.

## Estado

En desarrollo. El MVP deja fuera, por ahora, la elección de un destino en el mapa
y el acceso directo desde la notificación.

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
