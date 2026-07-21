# AssistiveVision

An Android application that provides real-time object detection assistance for visually impaired users. The app uses the device camera to detect objects in the environment and provides audio feedback about what's in front of the user.

## About

AssistiveVision leverages on-device machine learning to help visually impaired individuals navigate their surroundings independently. Using EfficientDet-Lite2 for real-time object detection, the app identifies everyday objects like people, chairs, tables, and more, then provides spoken feedback about their location and proximity. The system is designed for chest-mounted use in landscape orientation, capturing the user's natural field of view as they move through indoor environments.

## Features

- **Real-time object detection** using EfficientDet-Lite2 TFLite model
- **Camera integration** with CameraX for live preview and frame analysis
- **10 target object classes**: person, chair, couch, bed, table, toilet, tv, computer, sink, refrigerator
- **Optimized inference pipeline** with 9 FPS cap to match model throughput
- **Modular architecture** with dedicated modules for capture, preprocessing, and detection

## Architecture

The app follows a modular pipeline architecture:

```
CameraX Capture → Preprocessing → TFLite Detection → Audio Output
```

### Modules

| Module | Description |
|--------|-------------|
| `CaptureModule` | CameraX integration with configurable frame rate (9 FPS) |
| `PreprocessingModule` | Letterbox resize to 448x448 with RGB tensor conversion |
| `DetectionModule` | TFLite EfficientDet-Lite2 inference with COCO class mapping |

### Data Flow

1. **CaptureModule** captures frames at 9 FPS using CameraX ImageAnalysis
2. **PreprocessingModule** converts frames to 448x448 uint8 tensors with letterbox padding
3. **DetectionModule** runs EfficientDet-Lite2 inference, outputs detections with class labels and confidence scores
4. **MainViewModel** exposes detections via Kotlin StateFlow to the UI layer

## Tech Stack

- **Language**: Kotlin
- **Camera**: CameraX 1.6.1
- **ML Framework**: TensorFlow Lite 2.16.1
- **Model**: EfficientDet-Lite2 (efficientdet-lite2-detection-metadata.tflite)
- **Architecture**: MVVM with ViewModel and StateFlow
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
│   ├── PreprocessingModule.kt    # Image preprocessing and tensor conversion
│   └── DetectionModule.kt        # TFLite inference and object detection
├── viewmodel/
│   └── MainViewModel.kt          # Business logic and state management
├── model/
│   └── Detection.kt              # Detection data class
└── ui/
    └── MainActivity.kt           # Main activity with camera preview
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
2. Point the camera at objects in your environment
3. The app will detect objects and display them in the debug log

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
