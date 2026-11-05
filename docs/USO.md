# Instalar y usar AssistiveVision

La rama principal de la aplicación es **`main`**. Para instalar la versión actual,
elegir un APK construido desde esa rama.

[Inicio](../README.md) · [Compilar desde código](REPRODUCING.md) · [Repetir los experimentos](BENCHMARK.md)

AssistiveVision es un prototipo de investigación para Android. Al pulsar
**Capturar**, analiza una imagen y anuncia objetos y posiciones en español o
inglés. El detector y las voces vienen incluidos; el uso no requiere internet.

## Instalar la aplicación

Se necesita un teléfono **Android 8.0 o posterior con procesador arm64** y cámara
trasera. El APK universal incluye también x86_64 para emuladores. No sirve para
teléfonos que solo admitan aplicaciones ARM de 32 bits.

1. Desde el teléfono, tocar **[Descargar APK](https://github.com/diomedescerda/AssistiveVision/releases/latest/download/AssistiveVision-1.0-debug.apk)**.
2. Al terminar la descarga, abrir `AssistiveVision-1.0-debug.apk` y permitir la
   instalación desde esa fuente si Android lo solicita.
3. Abrir AssistiveVision y conceder acceso a la cámara.

En [Versiones y comprobantes](https://github.com/diomedescerda/AssistiveVision/releases/latest) se encuentran las versiones publicadas,
el SHA-256 y el manifiesto con el commit exacto. La descarga pública abre el APK
directamente y no requiere cuenta de GitHub ni extraer un ZIP.

Es una compilación de depuración para pruebas de investigación. Si ya hay una
versión firmada con otra clave, Android puede rechazar la actualización; conservar
los datos antes de cambiar la instalación. La guía de [compilación desde código](REPRODUCING.md)
explica cómo generar el paquete desde `main` y cómo instalarlo con `adb`.

## Primera captura

1. Abrir la aplicación y permitir el acceso a la cámara.
2. Apuntar hacia un objeto de las clases admitidas.
3. Pulsar **Capturar**. La imagen se congela durante el procesamiento y el anuncio.
4. Escuchar la descripción y esperar a que termine antes de la siguiente captura.

La primera inicialización de voz puede tardar más. Si no se oye, comprobar el
volumen multimedia y el mensaje de estado. La aplicación no necesita descargar
voces del servicio de texto a voz del teléfono.

Un disparador Bluetooth de cámara puede activar la captura: emparejarlo primero
desde los ajustes Bluetooth de Android y pulsarlo con la pantalla de cámara
abierta. La compatibilidad depende de los códigos de tecla enviados por el mando.

## Ajustes

| Ajuste | Valor inicial | Efecto |
|---|---|---|
| Idioma | Español | Cambia interfaz y voz entre español e inglés |
| Velocidad de voz | 1,0 | Ajustable de 0,5 a 2,5 |
| Confianza | 0,30 | Umbral de detección, entre 0,10 y 0,90; subirlo puede reducir anuncios incorrectos y también perder objetos |
| Máximo de objetos | 2 | De 1 a 3 objetos seleccionados por confianza para el anuncio |
| Vista previa | Activada | Muestra u oculta la imagen, sin convertir el flujo en detección continua |

## Qué significan los anuncios

Se reconocen diez clases: persona, silla, sofá, cama, mesa de comedor, inodoro,
televisor, portátil/computadora, lavamanos y refrigerador. «Computadora» corresponde
a la clase COCO *laptop*. No se reconocen automáticamente todos los objetos posibles.

Izquierda, centro y derecha se combinan con parte superior e inferior para formar
seis zonas de la imagen. Las posiciones son relativas a donde apunta la cámara:
no indican metros, profundidad medida ni una ruta de navegación. En `main`, la captura usa Camera2 y prefiere una cámara trasera ultra gran angular
utilizable, con retorno a la cámara trasera principal. Puede consultar cámaras
físicas y rutas multicámara lógicas; la disponibilidad de 0,5x depende del teléfono.

El [estudio experimental](https://github.com/diomedescerda/indoor-detect)
documenta aciertos, omisiones y falsos positivos. Sus fotografías se reprodujeron
en emulador; no constituyen una validación perceptual del audio con usuarios.
