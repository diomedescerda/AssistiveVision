# AssistiveVision

The main branch is **`main`**. Use it for current app builds and documentation.
The experimental replay uses the historical commit documented in [BENCHMARK.md](docs/BENCHMARK.md).

An Android research prototype that describes ten types of everyday objects in
Spanish or English. Point the camera, press **Capture**, and hear an announcement
with the objects and their positions. Detection and bundled speech run on the
phone without an internet connection.

**[Download APK for Android](https://github.com/diomedescerda/AssistiveVision/releases/latest/download/AssistiveVision-1.0-debug.apk)** · [Versions and checksums](https://github.com/diomedescerda/AssistiveVision/releases/latest)

**Español:** [instalación y uso paso a paso](docs/USO.md).

## Choose your starting point

| I want to… | Start here |
|---|---|
| Install and try the app | [Download APK](https://github.com/diomedescerda/AssistiveVision/releases/latest/download/AssistiveVision-1.0-debug.apk) · [Installation guide](docs/USO.md#instalar-la-aplicación) |
| Build the same app from source | [Reproducible build guide](docs/REPRODUCING.md) |
| Reproduce the experimental results | [Android replay and saved-result analysis](docs/BENCHMARK.md) |
| Understand the implementation | [Architecture and modules](docs/ARCHITECTURE.md) |
| Check model/voice files and attribution | [Asset checksums](docs/assets.sha256.json) · [Third-party components](docs/THIRD_PARTY.md) |
| Read the thesis, dataset and evidence | [indoor-detect](https://github.com/diomedescerda/indoor-detect) |

## Build an installable APK

Install **JDK 21**, Python 3.12+ and the Android SDK, then install Platform 37.1,
Build-Tools 36.0.0 and Platform-Tools in Android Studio's SDK Manager. See the
[full guide](docs/REPRODUCING.md) for SDK setup, Windows commands and troubleshooting.
The first build needs internet to download Gradle and Maven dependencies.

```sh
git clone --branch main https://github.com/diomedescerda/AssistiveVision.git
cd AssistiveVision
export ANDROID_HOME="$HOME/Android/Sdk"  # replace with your SDK location
export JAVA_HOME=/path/to/jdk-21
python3 scripts/build_apk.py
```

The build starts clean, runs unit tests and Android lint, builds the app and test
APKs, and checks every bundled model/voice asset. The distributable is
`dist/AssistiveVision-1.0-debug.apk`, accompanied by `SHA256SUMS`,
`build-manifest.json` and attribution. This is a **debug-signed research build**.
Rebuilding on another computer uses a different debug signing key, so APK bytes
need not match; the manifest identifies the source and exact bundled assets.

Published APKs are available as individual downloads on [GitHub Releases](https://github.com/diomedescerda/AssistiveVision/releases/latest).
Each version includes the APK, checksum, build manifest, signature/metadata reports
and licenses. The **Download APK** link above opens the current APK directly.
[Publishing instructions](docs/RELEASES.md) explain how maintainers create a new
version from a verified `main` build.

## Try a capture

1. Install on an **arm64 Android 8.0+ phone**, open AssistiveVision and grant camera access.
2. Point the rear camera at a supported object, then tap **Capture / Capturar fotograma**. The frame freezes,
   boxes show the detections, and a sentence describes their positions.
3. Wait for the announcement to finish before capturing again.
4. Open **Settings / Configuración** (gear icon) to change language, voice speed, confidence, object limit or preview.

If **Capture** stays disabled after the first camera permission grant on `v1.0.0`,
force-close and reopen the app once; the [installation guide](docs/USO.md#primera-captura)
includes the verified recovery.

Defaults: Spanish, confidence **0.30**, at most **two objects** and speech rate **1.0**.
An external Bluetooth shutter can trigger capture after pairing in Android settings.
On `main`, Camera2 capture prefers a usable rear ultra-wide camera and falls
back to the main rear camera. It supports direct physical cameras and logical
multi-camera paths; availability depends on the phone. Detection uses a JPEG
still while the live preview remains separate.

Supported COCO classes: person, chair, couch, bed, dining table, toilet, TV, laptop,
sink and refrigerator. The displayed “computer” class corresponds to COCO laptop,
not every kind of computer. Positions are six image-relative zones, rather than
measured distances or a navigation route. The app can miss objects or announce
incorrect detections; the study evaluates these errors.

## Evidence and reproduction

The companion repository preserves **198 photos, 889 annotated instances and
33 six-view capture groups**, the exact EfficientDet-Lite2 model, Android
instrumentation and predictions. The reference app commit is
`85c852ddcb543bce1bd69ad0db307f895d597881`.

[Reproduce the study](docs/BENCHMARK.md) either by regenerating all analysis from
saved predictions without Android, or by replaying the photos through the app on
an emulator/device. These are separate checks. The benchmark evaluates the
production preprocessing/detector modules; camera capture and speech comprehension
are outside that dataset replay. Emulator timing is not phone performance.

## Repository structure

`app/` contains the application, bundled assets and required tests. `docs/` holds
the user/build/experiment guides; `scripts/` holds the supported build commands.
Intermediate logs and presentation tooling are grouped in
[`development/`](development/README.md), outside the app build. New auxiliary
outputs go to ignored `development/runs/`; APK packages remain in `dist/`.

## Development

Kotlin, Camera2 capture, LiteRT EfficientDet-Lite2, MVVM, DataStore and bundled
sherpa-onnx/Piper speech. Android min SDK 26, target 36, compile 37.1;
Gradle 9.5.0 and AGP 9.3.1 are pinned in the project.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
```

[Requirements](docs/REQUIREMENTS.md) · [Architecture](docs/ARCHITECTURE.md) ·
[Validation record](docs/VALIDATION.md) · [Contributing](CONTRIBUTING.md)

App code: [Apache-2.0](LICENSE). Bundled assets have
[separate upstream terms](docs/THIRD_PARTY.md).
