# AssistiveVision

An Android application that provides real-time object detection assistance for visually impaired users. The app uses the device camera to detect objects in the environment and provides spatial localization with multi-language support.

## About

AssistiveVision leverages on-device machine learning to help visually impaired individuals navigate their surroundings independently. Using EfficientDet-Lite2 for real-time object detection, the app identifies everyday objects like people, chairs, tables, and more, then displays bounding boxes and spoken feedback about their location. The system is designed for immersive full-screen use, capturing the user's natural field of view as they move through indoor environments.

## Features

- **Real-time object detection** using EfficientDet-Lite2 TFLite model
- **Camera integration** with CameraX for live preview and frame analysis
- **10 target object classes**: person, chair, couch, bed, table, toilet, tv, computer, sink, refrigerator
- **Detection overlay** with teal-colored bounding boxes and label tags
- **AUTO / MANUAL modes** — continuous detection or on-demand capture
- **Spatial localization** — zones like "center bottom", "left top" etc.
- **Multi-language support** — English and Spanish via Android string resources
- **Immersive full-screen** — system bars hidden, edge-to-edge layout
- **Pipeline mutex** — skips frames if the pipeline is busy, matching CameraX KEEP_ONLY_LATEST strategy
- **Optimized inference pipeline** with 9 FPS cap and bitmap recycling to reduce GC pressure

## Architecture

The app follows a fragment-based MVVM architecture:

```
CameraX Capture → Preprocessing → TFLite Detection → Localization → Overlay / Audio
```

### Modules

| Module | Description |
|--------|-------------|
| `CaptureModule` | CameraX integration with configurable frame rate (9 FPS) |
| `PreprocessingModule` | Letterbox resize to 448x448 with RGB tensor conversion and bitmap recycling |
| `DetectionModule` | TFLite EfficientDet-Lite2 inference with COCO class mapping and confidence filtering |
| `LocalizationModule` | Spatial zone computation (left/center/right × top/bottom) |

### UI

| Component | Description |
|-----------|-------------|
| `MainActivity` | Edge-to-edge immersive host with Navigation Component |
| `CameraFragment` | Camera preview, overlay, mode toggle, status bar, and action buttons |
| `SettingsFragment` | Language selection via Spinner |
| `OverlayView` | Custom View drawing teal bounding boxes and label tags over detections |

### Data Flow

1. **CaptureModule** captures frames at 9 FPS using CameraX ImageAnalysis
2. **PreprocessingModule** converts frames to 448x448 uint8 tensors with letterbox padding (recycles intermediate bitmaps)
3. **DetectionModule** runs EfficientDet-Lite2 inference, filters to target COCO classes
4. **LocalizationModule** assigns spatial zones to each detection
5. **MainViewModel** exposes detections, zone labels, FPS, and detection count via LiveData
6. **CameraFragment** observes LiveData and updates the OverlayView and status UI

## Tech Stack

- **Language**: Kotlin
- **Camera**: CameraX 1.6.1
- **ML Framework**: TensorFlow Lite 2.16.1
- **Model**: EfficientDet-Lite2 (efficientdet-lite2-detection-metadata.tflite)
- **Navigation**: Navigation Component 2.7.7
- **Architecture**: MVVM with ViewModel and LiveData
- **UI**: Fragment-based with View Binding
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Compile SDK**: 37

## Performance

Based on benchmark testing on Snapdragon 685:

| Metric | Value |
|--------|-------|
| Model | EfficientDet-Lite2 |
| Input Size | 448x448 |
| Inference Latency | ~137.8 ms |
| FPS | ~8.55 |
| Target FPS | 9 (optimized for throughput) |

## Project Structure

```
app/src/main/java/com/unimagdalena/assistivevision/
├── modules/
│   ├── CaptureModule.kt          # Camera capture and frame delivery
│   ├── PreprocessingModule.kt    # Image preprocessing, tensor conversion, bitmap recycling
│   ├── DetectionModule.kt        # TFLite inference, COCO class mapping, confidence filtering
│   ├── LocalizationModule.kt     # Spatial zone computation
│   ├── AudioModule.kt            # (placeholder) Text-to-speech output
│   └── PriorityFilterModule.kt   # (placeholder) Detection prioritization
├── viewmodel/
│   └── MainViewModel.kt          # Pipeline orchestration, LiveData, mutex concurrency
├── model/
│   └── Detection.kt              # Detection data class (classId, className, score, bbox, zone)
└── ui/
    ├── MainActivity.kt           # Immersive edge-to-edge host, permission handling
    ├── CameraFragment.kt         # Camera preview, overlay, mode toggle, status UI
    ├── SettingsFragment.kt       # Language selection
    └── OverlayView.kt            # Custom View drawing detection boxes and labels
```

## Getting Started

### Prerequisites

- Android Studio with Kotlin support
- Android device or emulator with API 26+
- Camera permission (required for object detection)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/AssistiveVision.git
   ```

2. Open the project in Android Studio

3. Sync Gradle and build the project

4. Install on a physical Android device (recommended for camera access)

### Usage

1. Grant camera permission when prompted
2. The app starts in AUTO mode — point the camera at objects and detection boxes appear in real time
3. Switch to MANUAL mode to capture frames on demand using the Speak button
4. Access Settings to change the language

## Configuration

### Detection Parameters

In `DetectionModule.kt`:

```kotlin
const val CONFIDENCE_THRESHOLD = 0.30f  // Minimum detection confidence
const val MAX_DETECTIONS = 15           // Maximum objects to track
```

### Frame Rate

In `CaptureModule.kt`:

```kotlin
const val maxFps = 9  // Frames per second (matched to model throughput)
```

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

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- TensorFlow Lite team for EfficientDet-Lite2 model
- Android CameraX library for camera integration
- COCO dataset for object detection training data
