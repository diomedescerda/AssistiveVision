from pathlib import Path

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.util import Inches, Pt

ROOT = Path(__file__).parent
OUT = Path(__file__).parent / "AssistiveVision_RetroalimentacionDeVoz.pptx"

NAVY = RGBColor(8, 15, 30)
PANEL = RGBColor(15, 28, 49)
PANEL_2 = RGBColor(20, 39, 65)
CYAN = RGBColor(55, 221, 235)
GREEN = RGBColor(111, 235, 164)
ORANGE = RGBColor(255, 175, 86)
RED = RGBColor(255, 104, 119)
WHITE = RGBColor(240, 247, 255)
MUTED = RGBColor(157, 177, 201)
GRID = RGBColor(42, 66, 94)

W, H = 13.333, 7.5
prs = Presentation()
prs.slide_width = Inches(W)
prs.slide_height = Inches(H)


def rect(slide, x, y, w, h, fill, radius=False, line=None):
    shape = slide.shapes.add_shape(
        MSO_SHAPE.ROUNDED_RECTANGLE if radius else MSO_SHAPE.RECTANGLE,
        Inches(x), Inches(y), Inches(w), Inches(h),
    )
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.color.rgb = line or fill
    if radius:
        shape.adjustments[0] = 0.08
    return shape


def text(slide, value, x, y, w, h, size=18, color=WHITE, bold=False,
         font="Aptos", align=PP_ALIGN.LEFT, valign=MSO_ANCHOR.TOP):
    box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.clear()
    tf.word_wrap = True
    tf.vertical_anchor = valign
    p = tf.paragraphs[0]
    p.alignment = align
    run = p.add_run()
    run.text = value
    run.font.name = font
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color
    return box


def base(title, section="ASSISTIVEVISION / AUDIO ASISTIVO"):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    bg = slide.background.fill
    bg.solid()
    bg.fore_color.rgb = NAVY
    rect(slide, 0, 0, W, 0.06, CYAN)
    text(slide, section, 0.55, 0.22, 8.5, 0.25, 9, CYAN, True)
    text(slide, title, 0.55, 0.58, 12.2, 0.55, 27, WHITE, True)
    text(slide, f"{len(prs.slides):02d}", 12.25, 0.25, 0.5, 0.25, 10, MUTED, True, "Aptos", PP_ALIGN.RIGHT)
    return slide


def bullet(slide, value, x, y, w, color=WHITE, size=16, accent=CYAN, bold=False):
    rect(slide, x, y + 0.12, 0.08, 0.08, accent, radius=True)
    text(slide, value, x + 0.22, y, w - 0.22, 0.48, size, color, bold)


def card(slide, x, y, w, h, title, body, color=CYAN):
    rect(slide, x, y, w, h, PANEL, True, GRID)
    rect(slide, x, y, 0.08, h, color)
    text(slide, title, x + 0.3, y + 0.22, w - 0.6, 0.3, 13, color, True, "Courier New")
    text(slide, body, x + 0.3, y + 0.66, w - 0.6, h - 0.85, 15, WHITE)


# 1. Cover
slide = prs.slides.add_slide(prs.slide_layouts[6])
slide.background.fill.solid()
slide.background.fill.fore_color.rgb = NAVY
rect(slide, 0, 0, W, 0.08, CYAN)
text(slide, "DIOMEDES CERDA · UNIVERSIDAD DEL MAGDALENA", 0.75, 0.72, 8.0, 0.3, 11, CYAN, True)
text(slide, "AssistiveVision\nRetroalimentacion de voz", 0.75, 1.6, 9.0, 1.7, 42, WHITE, True, "Aptos Display")
text(slide, "Como la deteccion de objetos se convierte en patrones auditivos accionables\npara un usuario con discapacidad visual.", 0.8, 3.55, 8.6, 0.9, 19, MUTED)
rect(slide, 0.8, 5.0, 2.2, 0.05, GREEN)
text(slide, "MODULOS NUEVOS", 0.8, 5.35, 3.4, 0.28, 11, GREEN, True)
text(slide, "AudioModule · SpeechFormatter · CameraLensPolicy\nAndroid TTS · Kotlin · MVVM", 0.8, 5.75, 5.5, 0.6, 15, WHITE)
text(slide, "AssistiveVision", 9.0, 6.75, 3.4, 0.25, 11, CYAN, True, "Courier New", PP_ALIGN.RIGHT)

# 2. El problema
slide = base("El usuario no depende de la vista")
text(slide, "La vista es la via natural para confirmar detecciones, pero el usuario con discapacidad\nvisual no puede usar esa via para los resultados de la app.", 0.58, 1.35, 8.6, 0.8, 19, WHITE)
rect(slide, 0.62, 2.6, 5.9, 3.3, PANEL, True, GRID)
text(slide, "RESTRICCIONES", 0.95, 2.9, 3.0, 0.25, 11, ORANGE, True)
bullet(slide, "Feedback principalmente auditivo", 0.95, 3.35, 5.1)
bullet(slide, "Interfaz visual secundaria", 0.95, 3.95, 5.1, WHITE, 16, GREEN)
bullet(slide, "Deteccion en tiempo real: muchas lecturas por segundo", 0.95, 4.55, 5.2, WHITE, 16, CYAN)
bullet(slide, "Evitar repetir el mismo anuncio", 0.95, 5.15, 5.1, WHITE, 16, RED)
rect(slide, 6.95, 2.6, 5.75, 3.3, PANEL, True, GRID)
text(slide, "SOLUCION", 7.25, 2.9, 3.0, 0.25, 11, GREEN, True)
bullet(slide, "Captura manual: toca 'Capturar' para analizar el encuadre", 7.25, 3.35, 5.0, GREEN)
bullet(slide, "Se anuncia el objeto con mayor confianza", 7.25, 3.95, 5.0, GREEN)
bullet(slide, "Captura congelada: una foto, un anuncio claro", 7.25, 4.55, 5.0, GREEN)
bullet(slide, "Voz, idioma y velocidad configurables", 7.25, 5.15, 5.05, GREEN)

# 3. Pipeline actualizado
slide = base("Del frame a la palabra hablada", "03 / PIPELINE")
text(slide, "Cada frame atraviesa el mismo circuito; solo las ultimas etapas son nuevas.", 0.58, 1.25, 9.5, 0.4, 18, WHITE)
steps = [
    ("01", "capturar", "Frame RGB · CameraX"),
    ("02", "preprocesar", "letterbox 448×448"),
    ("03", "inferir", "EfficientDet-Lite2"),
    ("04", "localizar", "zonas · 6 regiones"),
    ("05", "formar frase", "SpeechFormatter"),
    ("06", "hablar", "TTS es/en"),
]
for i, (num, name, detail) in enumerate(steps):
    x = 0.35 + i * 2.12
    new = "06" in num or "05" in num
    highlight = CYAN if new else GRID
    rect(slide, x, 2.35, 1.82, 1.7, PANEL, True, highlight)
    text(slide, num, x + 0.15, 2.5, 0.4, 0.2, 11, highlight, True, "Courier New")
    text(slide, name, x + 0.15, 3.0, 1.55, 0.3, 18, WHITE, True, "Aptos Display")
    text(slide, detail, x + 0.15, 3.55, 1.55, 0.22, 10, MUTED, False, "Courier New")
    if i < 5:
        text(slide, ">", x + 1.86, 3.0, 0.3, 0.35, 20, GREEN, True, "Courier New", PP_ALIGN.CENTER)
text(slide, "NUEVO", 0.4, 4.35, 1.5, 0.25, 11, GREEN, True)
rect(slide, 0.55, 4.68, 12.2, 0.09, GRID)
text(slide, "AudioModule y SpeechFormatter ahora viven en el flujo:", 0.58, 5.1, 10.5, 0.3, 16, MUTED)
bullet(slide, "Se arma la frase localizada y el TTS la pronuncia al instante", 0.85, 5.6, 9.5, WHITE, 16, GREEN)

# 4. AudioModule
slide = base("AudioModule: wrapper de TextToSpeech", "04 / MODULO DE AUDIO")
rect(slide, 0.62, 1.5, 6.0, 4.6, PANEL, True, GRID)
text(slide, "RESPONSABILIDAD", 0.95, 1.78, 3.0, 0.25, 11, CYAN, True)
bullet(slide, "Inicializacion asincrona con gate 'ready'", 0.95, 2.25, 5.2)
bullet(slide, "Aplica idioma (es/en) y velocidad de voz", 0.95, 2.85, 5.25)
bullet(slide, "speak con QUEUE_FLUSH: interrumpe la voz obsoleta", 0.95, 3.45, 5.3)
bullet(slide, "stop() y shutdown() liberan el motor", 0.95, 4.05, 5.2)
bullet(slide, "Listener de progreso: onDone / onError", 0.95, 4.65, 5.25)
rect(slide, 6.95, 1.5, 5.75, 4.6, PANEL, True, GRID)
text(slide, "DISEÑO", 7.25, 1.78, 3.0, 0.25, 11, GREEN, True)
bullet(slide, "Interfaz mínima: setLanguage · setSpeechRate · speak · stop · shutdown", 7.25, 2.25, 5.15)
bullet(slide, "Guarda configuracion aun sin estar listo el motor", 7.25, 3.05, 5.2)
bullet(slide, "La vista / ViewModel no toca el motor directamente", 7.25, 3.85, 5.2)
bullet(slide, "Lista para intercambiar por Piper (offline) sin tocar llamadores", 7.25, 4.65, 5.2, GREEN, 15, CYAN)
text(slide, "Piper · sherpa-onnx  →  mismo `speak()`, motor distinto", 6.95, 6.35, 5.75, 0.3, 13, MUTED, False, "Courier New")

# 5. Captura manual
slide = base("Una captura, un anuncio claro", "05 / CONTROL DE REPETICION")
text(slide, "La deteccion corre solo cuando el usuario la pide. Sin filtro ni cadenas de\nanuncios repetidos.", 0.58, 1.3, 9.4, 0.75, 18, WHITE)
rect(slide, 0.6, 2.5, 6.05, 2.9, PANEL, True, GRID)
text(slide, "REGLA", 0.9, 2.78, 2.0, 0.2, 11, CYAN, True)
bullet(slide, "Toca 'Capturar'  → el encuadre se congela y se analiza", 0.9, 3.25, 5.4, WHITE, 15, GREEN)
bullet(slide, "Habla el objeto destacado; al terminar, se libera el encuadre", 0.9, 3.95, 5.4, WHITE, 15, GREEN)
bullet(slide, "QUEUE_FLUSH interrumpe la voz obsoleta si vuelves a capturar", 0.9, 4.65, 5.4, WHITE, 15, CYAN)
rect(slide, 7.0, 2.5, 5.75, 2.9, PANEL, True, GRID)
text(slide, "IMPLEMENTACION", 7.3, 2.78, 3.2, 0.2, 11, ORANGE, True)
bullet(slide, "Un solo anuncio activo a la vez (MainViewModel)", 7.3, 3.25, 5.0)
bullet(slide, "Watchdog: nunca se queda pegado si el TTS falla", 7.3, 3.95, 5.1)
bullet(slide, "Boton de captura se deshabilita durante la captura", 7.3, 4.65, 5.0, GREEN, 15, GREEN)
text(slide, "captureFrame() → runPipeline() → speak() → onDone → unfreeze()", 0.6, 5.75, 12.0, 0.35, 14, CYAN, False, "Courier New")

# 6. Salida localizada
slide = base("Cada objeto se dice en tu idioma", "06 / SPEECHFORMATTER")
text(slide, "SpeechFormatter (puro, sin Android) arma la frase; LocalizedLabels resuelve\nlos nombres de clase y de zona por idioma.", 0.58, 1.3, 9.4, 0.75, 18, WHITE)
examples = [
    ("ESP", "silla, frente a ti", "persona, a la derecha", "cama, abajo a la izquierda"),
    ("ENG", "chair, in front of you", "person, to the right", "bed, lower left"),
]
for i, (lang, e1, e2, e3) in enumerate(examples):
    x = 0.6 + i * 6.2
    rect(slide, x, 2.45, 5.9, 2.15, PANEL, True, GRID)
    text(slide, lang, x + 0.28, 2.7, 1.2, 0.25, 12, CYAN, True, "Courier New")
    text(slide, e1, x + 0.28, 3.2, 5.3, 0.35, 19, WHITE, True)
    text(slide, e2, x + 0.28, 3.7, 5.3, 0.35, 17, MUTED)
    text(slide, e3, x + 0.28, 4.2, 5.3, 0.35, 17, MUTED)
rect(slide, 0.6, 5.05, 11.95, 1.15, PANEL, True, GRID)
text(slide, "10 CLASES LOCALIZADAS", 0.95, 5.35, 2.8, 0.22, 11, ORANGE, True)
text(slide, "persona · silla · sofa · cama · mesa · inodoro · television · computadora · lavabo · refrigerador", 3.95, 5.3, 8.3, 0.55, 15, WHITE)
text(slide, "4 pruebas unitarias sobre el armado de frases", 0.6, 6.35, 9.0, 0.3, 14, GREEN)

# 7. Configuracion conectada
slide = base("Las preferencias llegan al audio al instante", "07 / CONFIGURACION")
settings_cards = [
    ("IDIOMA", "es / en", "motor TTS y strings"),
    ("VELOCIDAD", "0.5x – 2.5x", "applySpeechRate"),
    ("VISTA PREVIA", "on / off", "OverlayView"),
    ("CONFIANZA", "0.10 – 0.90", "DetectionModule"),
    ("MAX. OBJETOS", "1 – 3", "top-N anunciados"),
]
for i, (label, value, body) in enumerate(settings_cards):
    x = 0.6 + i * 2.5
    rect(slide, x, 2.2, 2.2, 1.8, PANEL, True, GRID)
    text(slide, label, x + 0.16, 2.42, 1.9, 0.2, 10, CYAN, True, "Courier New")
    text(slide, value, x + 0.16, 2.85, 1.9, 0.35, 20, WHITE, True)
    text(slide, body, x + 0.16, 3.45, 1.9, 0.25, 10, MUTED, False, "Courier New")
rect(slide, 0.6, 4.55, 11.95, 1.6, PANEL, True, GRID)
text(slide, "FLUJO DE PREFERENCIAS", 0.95, 4.85, 3.0, 0.22, 11, ORANGE, True)
text(slide, "DataStore (persistente)  →  SettingsManager  →  MainViewModel (collect)  →  AudioModule", 3.95, 4.82, 8.2, 0.35, 14, WHITE, False, "Courier New")
bullet(slide, "Los cambios se reflejan sin reiniciar la app", 0.9, 5.6, 9.0, WHITE, 15, GREEN)

# 8. Como probar
slide = base("Como probarlo en tu dispositivo", "08 / PRUEBA")
req = [
    ("1", "Android 8.0+ (API 26) con camara"),
    ("2", "Voz es/en instalada: Configuracion > Accesibilidad > Salida de texto a voz"),
    ("3", "Compilar:  ./gradlew assembleDebug"),
    ("4", "Instalar:  adb install app/build/outputs/apk/debug/app-debug.apk"),
]
for i, (num, cmd) in enumerate(req):
    y = 1.7 + i * 0.72
    rect(slide, 0.6, y, 0.5, 0.5, PANEL, True, GRID)
    text(slide, num, 0.6, y + 0.12, 0.5, 0.25, 15, CYAN, True, "Courier New", PP_ALIGN.CENTER)
    text(slide, cmd, 1.35, y + 0.11, 11.0, 0.35, 16, WHITE, True, "Courier New")
text(slide, "En la app:", 0.62, 5.1, 2.0, 0.3, 14, MUTED, True)
bullet(slide, "MANUAL: apunta la camara y toca 'Capturar' — escucharas 'silla, frente a ti'", 0.9, 5.6, 11.0, WHITE, 15, GREEN)
bullet(slide, "Prueba la lente 0.5x: la app elige la camara de menor distancia focal", 0.9, 6.2, 11.0, WHITE, 15, CYAN)
bullet(slide, "Configuracion: cambia idioma, velocidad, confianza y maximo de objetos", 0.9, 6.8, 11.0, WHITE, 15, ORANGE)

# 9. Proximos pasos
slide = base("Estado actual y siguientes pasos", "09 / CIERRE")
rect(slide, 0.62, 1.5, 5.9, 4.4, PANEL, True, GRID)
text(slide, "IMPLEMENTADO", 0.95, 1.78, 3.0, 0.25, 11, GREEN, True)
bullet(slide, "AudioModule (TTS de plataforma) — listo", 0.95, 2.25, 5.2)
bullet(slide, "Captura manual + watchdog de voz — listo", 0.95, 2.95, 5.2)
bullet(slide, "SpeechFormatter + LocalizedLabels — listo", 0.95, 3.65, 5.3)
bullet(slide, "Lente 0.5x con fallback a 1x — listo", 0.95, 4.35, 5.3)
bullet(slide, "SpeechFormatter + LocalizationModule: pruebas unitarias · lint limpio", 0.95, 5.05, 5.3, GREEN, 15, GREEN)
rect(slide, 6.95, 1.5, 5.75, 4.4, PANEL, True, GRID)
text(slide, "PROXIMO", 7.25, 1.78, 3.0, 0.25, 11, ORANGE, True)
bullet(slide, "Piper / sherpa-onnx: TTS offline con la misma interfaz", 7.25, 2.25, 5.15)
bullet(slide, "Modo escaneo de habitacion guiado por voz", 7.25, 3.05, 5.2)
bullet(slide, "Varias fotos con gorilla pod + IMU y mapeo de distancias", 7.25, 3.85, 5.2)
bullet(slide, "Haptic + audio espacial como segunda via", 7.25, 4.65, 5.2, GREEN, 15, CYAN)
text(slide, "La vision del producto: un sistema que 'lee' la habitacion y la describe con voz.", 0.62, 6.2, 12.1, 0.4, 16, MUTED, True, "Aptos Display", PP_ALIGN.CENTER)

prs.save(OUT)
print(f"Saved {OUT}")