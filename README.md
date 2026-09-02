# Descargador (Android)

App de Android para **descargar vídeos** (y solo audio en MP3) de cientos de
sitios, apoyándose en `yt-dlp`. Si el vídeo llega en otro formato, lo deja
siempre en **`.mp4`** (remux, sin recodificar).

Kotlin + Jetpack Compose + Material 3. Motor:
[`youtubedl-android`](https://github.com/JunkFood02/youtubedl-android) (empaqueta
Python + `yt-dlp` + FFmpeg + QuickJS como librerías nativas).

## Funciones

- Pegar uno o varios enlaces, o **compartir** un enlace desde otra app.
- Calidad: mejor / 1080p / 720p / 480p / 360p, o **solo audio (MP3)**.
- Descarga en **servicio en primer plano** con notificación de progreso y cancelar.
- Los vídeos van a `Movies/Descargador/` y el audio a `Music/Descargador/`.
- **Historial** con botón para abrir el archivo, y **Ajustes** (calidad por
  defecto, actualizar `yt-dlp`).

## ⚠️ Sobre YouTube

A día de hoy **YouTube no funciona de forma fiable** en Android con este motor
(ni con este, ni con apps como Seal): `yt-dlp` obtiene el título y la lista de
formatos, pero al descargar el stream YouTube responde **HTTP 403** por su
control anti-bot / *PO token*. No es un fallo de esta app: es una limitación
actual de todo el ecosistema `yt-dlp` en móvil.

Qué sí funciona: **el resto de sitios** compatibles con `yt-dlp` (los que no
imponen ese reto). Y hay un botón **«Actualizar yt-dlp»** en Ajustes para
recoger las mejoras que vayan saliendo upstream.

Si tu único objetivo es YouTube, hoy por hoy es más práctico usar
[Seal](https://github.com/JunkFood02/Seal) o `yt-dlp` en un PC.

## Compilar

Requisitos: Android Studio (o SDK + JDK 17/21). Luego:

```bash
# APK de depuración (no necesita ninguna clave)
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

### APK de release firmado

El repo **no incluye** ninguna clave de firma. Tienes dos opciones:

**A) Usar tu propia clave** (recomendado)

```bash
keytool -genkeypair -v -keystore mi-clave.jks -alias mialias \
  -keyalg RSA -keysize 2048 -validity 10000
cp keystore.properties.example keystore.properties
# edita keystore.properties con storeFile / storePassword / keyAlias / keyPassword
./gradlew assembleRelease
```

**B) No poner clave**: si no existe `keystore.properties`, el build de release
se firma automáticamente con la clave de **depuración**. Es suficiente para
instalarlo tú mismo por USB (`adb install`), pero no para publicarlo.

`splits.abi` genera un APK por arquitectura (`arm64-v8a` ≈ 65 MB) más uno
`universal`. Salida en `app/build/outputs/apk/release/`.

## Instalar

`adb install app/build/outputs/apk/release/app-arm64-v8a-release.apk`
(o copia el APK al teléfono y ábrelo, permitiendo «instalar apps desconocidas»).

## Estructura

```
app/src/main/java/com/gatotote/descargador/
  DescargadorApp.kt        Application: inicializa el motor (2º plano)
  MainActivity.kt          Navegación + 3 pantallas + recibir "Compartir"
  data/
    YtdlpEngine.kt          Envoltorio de youtubedl-android (info, descarga, update)
    DownloadService.kt      Servicio en primer plano + notificación
    MediaStoreSaver.kt      Guardar en Movies/ o Music/
    Historial.kt            Historial en JSON (filesDir)
    Ajustes.kt              Preferencias (DataStore)
  ui/descargar | historial | ajustes | theme
generar_iconos_android.py   Genera los PNG del icono (Pillow)
```

## Licencia / uso

Herramienta de uso general. Descarga solo contenido sobre el que tengas
derechos. Descargar material protegido sin permiso puede infringir los
términos del sitio y la ley aplicable en tu país.
