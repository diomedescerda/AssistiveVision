# Instalar y usar AssistiveVision

[Inicio](../README.md) · [Compilar desde código](REPRODUCING.md) · [Repetir los experimentos](BENCHMARK.md)

AssistiveVision es un prototipo de investigación para Android. Al pulsar
**Capturar**, analiza una imagen y anuncia objetos y posiciones en español o
inglés. El detector y las voces vienen incluidos; el uso no requiere internet.

## Instalar la aplicación

Se necesita un teléfono **Android 8.0 o posterior con procesador arm64** y cámara
trasera. El APK universal incluye también x86_64 para emuladores. No sirve para
teléfonos que solo admitan aplicaciones ARM de 32 bits.

Para obtener el APK:

1. En GitHub, abrir **Actions → Build and verify Android APK**.
2. Elegir una ejecución completada correctamente para el commit deseado.
3. Descargar el artefacto `AssistiveVision-debug-<commit>` y descomprimirlo.
4. Dentro está `AssistiveVision-1.0-debug.apk`, su comprobante SHA-256 y la
   identificación del código utilizado. GitHub puede pedir iniciar sesión.

Los artefactos se conservan 30 días y aparecen después de publicar y ejecutar
el workflow. Si no existe una ejecución disponible, [compilar desde el código](REPRODUCING.md)
produce el mismo tipo de paquete sin necesitar claves de firma privadas.

Copiar el APK al teléfono, abrirlo y permitir la instalación desde esa fuente
cuando Android lo solicite. Es una compilación de depuración para pruebas de
investigación. Si ya hay una versión firmada por otra persona, Android puede
rechazar la actualización; no desinstalarla sin conservar antes los datos que
se quieran mantener. También se puede instalar con `adb`, como indica la guía.

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
no indican metros, profundidad medida ni una ruta de navegación. La selección
automática de lente busca la focal más corta que Android exponga a CameraX;
que un teléfono tenga lente 0,5x no garantiza que la aplicación pueda seleccionarla.

El [estudio experimental](https://github.com/diomedescerda/indoor-detect)
documenta aciertos, omisiones y falsos positivos. Sus fotografías se reprodujeron
en emulador; no constituyen una validación perceptual del audio con usuarios.
