# AssistiveVision

An Android application that provides object detection assistance for visually impaired users. The app uses the device camera to capture a frame on demand, detects objects with on-device machine learning, and reports what it sees out loud with spatial localization in English or Spanish.

## About

AssistiveVision leverages on-device machine learning to help visually impaired individuals understand their surroundings independently. Using EfficientDet-Lite2, the app identifies everyday objects like people, chairs, and tables, draws bounding boxes over the frozen frame, and speaks a localized announcement such as "silla, inferior izquierda" ("chair, lower left"). The system is designed for immersive full-screen use, capturing the user's natural field of view.

## Features

- **Manual capture workflow** — tap "Capture" to freeze the current frame; one tap, one clear announcement
- **Object detection** using the EfficientDet-Lite2 TFLite model (LiteRT)
- **Smart lens selection** — binds the 0.5x ultra-wide camera when the phone has one, otherwise the lowest-focal-length (1x) back camera
- **Camera permission gating** — the camera only starts after permission is granted; denial is shown in-app, localized
- **10 target object classes**: person, chair, couch, bed, table, toilet, tv, computer, sink, refrigerator
- **Detection overlay** with teal bounding boxes, a dark visibility halo, and label tags (font-scale aware)
- **Spatial localization** — zones like "bottom center", "top left", etc.
- **Spoken announcements** — Android text-to-speech reports detected objects with localized names and zones (e.g. "silla, inferior izquierda"), localized for English and Spanish
- **Settings** — language, speech rate (0.5x–2.5x), confidence threshold (0.10–0.90), max announced objects (1–3), preview visibility
- **Per-app language switching** — instant EN/ES switch via `AppCompatDelegate` with platform `localeConfig`
- **Accessibility** — WCAG-AA contrast, button roles and labels for TalkBack, live-region status row
- **Robust lifecycle** — interpreter close race guarded, image proxies closed in `finally`, TTS watchdog prevents stuck captures
- **Release hardening** — R8 minification + resource shrinking, configurable release signing

## Architecture

The app follows a fragment-based MVVM architecture:

```
CameraX Capture → Preprocessing (letterbox) → TFLite Detection → Inverse mapping → Localization → SpeechFormatter → TTS
```

### Modules

| Module | Description |
|--------|-------------|
| `CaptureModule` | CameraX integration: preview + ImageAnalysis, 9 FPS frame throttle, lens selection |
| `CameraLensPolicy` | Pure lens-choice logic: lowest focal length among back cameras (0.5x if present) |
| `PreprocessingModule` | Letterbox resize to 448x448 with RGB uint8 tensor conversion; geometry from `LetterboxMapper` |
| `DetectionModule` | LiteRT EfficientDet-Lite2 inference with COCO class mapping, confidence filtering, synchronized close |
| `LocalizationModule` | Spatial zone computation (left/center/right × top/bottom) |
| `AudioModule` | Android TextToSpeech wrapper (ready gating, language/speech-rate, QUEUE_FLUSH, failure listener) |

### UI

| Component | Description |
|-----------|-------------|
| `MainActivity` | Edge-to-edge immersive host with Navigation Component; applies the saved locale asynchronously |
| `CameraFragment` | Permission-gated camera preview, overlay, status bar, capture/settings actions |
| `SettingsFragment` | Language, speech rate, confidence, max objects, preview visibility |
| `OverlayView` | Custom View drawing bounding boxes and label tags over the frozen frame |

### Data Flow

1. **CaptureModule** keeps the latest frame available (throttled to 9 FPS via ImageAnalysis)
2. The user taps **Capture**; the frame is frozen and copied for the pipeline
3. **PreprocessingModule** letterboxes the frame to 448x448 using shared `LetterboxMapper` geometry (gray 114 padding)
4. **DetectionModule** runs EfficientDet-Lite2 inference, filters to target COCO classes by confidence
5. Detections are mapped back to frame coordinates with the same `LetterboxMapper` (exact forward/inverse consistency)
6. **LocalizationModule** assigns spatial zones to each detection
7. **SpeechFormatter** assembles the localized sentence (pure, unit-testable)
8. **AudioModule** speaks it; when the utterance finishes (or a watchdog fires), the frozen frame is released
9. **MainViewModel** exposes detections, zone labels, detection count, and capture state via LiveData; **CameraFragment** updates the OverlayView and status UI

Spoken object names and zone labels are resolved through localized string resources
(`LocalizedLabels`), keeping the sentence assembly in `SpeechFormatter` pure and unit-testable.

## Tech Stack

- **Language**: Kotlin
- **Camera**: CameraX 1.6.1
- **ML Framework**: LiteRT (com.google.ai.edge.litert 1.4.2, TFLite `Interpreter` API)
- **Model**: EfficientDet-Lite2 (efficientdet-lite2-detection-metadata.tflite)
- **Navigation**: Navigation Component 2.7.7
- **Preferences**: Jetpack DataStore 1.1.1
- **Architecture**: MVVM with ViewModel and LiveData
- **UI**: Fragment-based with View Binding
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Compile SDK**: 37.1

## Performance

Based on benchmark testing on Snapdragon 685:

| Metric | Value |
|--------|-------|
| Model | EfficientDet-Lite2 |
| Input Size | 448x448 |
| Inference Latency | ~137.8 ms |
| FPS | ~8.55 |
| Frame throttle | 9 (matched to model throughput) |

## Project Structure

```
app/src/main/java/com/unimagdalena/assistivevision/
├── modules/
│   ├── CaptureModule.kt          # Camera capture, lens selection, frame delivery
│   ├── CameraLensPolicy.kt       # Pure 0.5x / lowest-focal-length lens decision
│   ├── PreprocessingModule.kt    # Letterbox preprocessing, tensor conversion
│   ├── DetectionModule.kt        # LiteRT inference, COCO mapping, synchronized close
│   ├── LocalizationModule.kt     # Spatial zone computation
│   └── AudioModule.kt            # Text-to-speech wrapper (language, rate, QUEUE_FLUSH)
├── viewmodel/
│   ├── MainViewModel.kt          # Pipeline orchestration, frozen-frame state, watchdog
│   └── SettingsViewModel.kt      # Settings passthrough to the UI
├── model/
│   ├── Detection.kt              # Detection data class (classId, className, score, bbox, zone)
│   ├── LetterboxMapper.kt        # Pure forward/inverse letterbox geometry (tested)
│   ├── SpeechFormatter.kt        # Pure, testable announcement sentence assembly
│   └── LocalizedLabels.kt        # Class-name and zone labels from string resources
├── data/
│   └── SettingsManager.kt        # DataStore-backed preferences
└── ui/
    ├── MainActivity.kt           # Immersive edge-to-edge host, async locale
    ├── CameraFragment.kt         # Permission-gated preview, capture flow
    ├── SettingsFragment.kt       # Settings UI
    └── OverlayView.kt            # Custom View drawing detection boxes and labels
```

## Getting Started

### Prerequisites

- Android Studio (Gradle 9.5 / AGP 9.3.1, JDK 21 — Android Studio's bundled JBR works)
- Android device or emulator with API 26+
- Camera permission (requested on first launch; required for object detection)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/AssistiveVision.git
   ```

2. Open the project in Android Studio

3. Sync Gradle and build the project

4. Install on a physical Android device (recommended for camera access)

### Usage

1. Grant camera permission when prompted (denial is explained in-app; re-request happens on next launch of the screen)
2. Point the camera at an object and tap **Capture** — the frame freezes, the most confident object is announced (e.g. "silla, inferior izquierda"), and boxes are drawn over the frozen frame
3. Open **Settings** to change the language, speech rate, confidence threshold, maximum announced objects, or preview visibility

### Release signing

Release builds use R8 minification. Signing reads `keystore.properties` at the repo root:

```properties
storeFile=/absolute/or/relative/path/to/keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

When the file is absent, release builds fall back to the debug signing config so `assembleRelease` always works locally.

## Tests

`.\gradlew :app:testDebugUnitTest` (or `./gradlew ...`) runs the local unit test suite:

- `SpeechFormatterTest` — announcement sentence assembly
- `LocalizationModuleTest` — all six zones, threshold boundaries, degenerate frame sizes
- `LetterboxMapperTest` — forward geometry, inverse round-trip, padding edges
- `CameraLensPolicyTest` — ultra-wide preference, fallbacks, no-data handling

Instrumented tests run on device via `connectedDebugAndroidTest`.

## Configuration

### Detection Parameters

In `DetectionModule.kt`:

```kotlin
const val MAX_DETECTIONS = 15  // Maximum detections kept per inference
```

The confidence threshold is a runtime setting (DataStore default `0.30f`, configurable 0.10–0.90 in Settings) and is passed to `detect()` from `MainViewModel`.

### Frame Rate

In `CaptureModule.kt`:

```kotlin
private val maxFps: Int = 9  // Frames per second (matched to model throughput)
```

### Lens Selection

`CaptureModule` binds the back camera with the lowest available focal length via `CameraLensPolicy` — the 0.5x ultra-wide lens on phones that have one, otherwise the 1x main lens. Without focal-length data it falls back to the default back camera.

## Target Object Classes

The app detects the following 10 object classes from MS COCO dataset:

| Class | COCO ID | Description |
|-------|---------|-------------|
| Person | 1 | Human beings |
| Chair | 62 | Seating furniture |
| Couch | 63 | Sofas and lounges |
| Bed | 65 | Sleeping furniture |
| Table | 67 | Dining and work tables |
| Toilet | 70 | Bathroom fixtures |
| TV | 72 | Television screens |
| Computer | 73 | Laptops and desktops |
| Sink | 81 | Kitchen/bathroom sinks |
| Refrigerator | 82 | Cooling appliances |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- The EfficientDet-Lite2 model and the LiteRT team
- Android CameraX library for camera integration
- COCO dataset for object detection training data
