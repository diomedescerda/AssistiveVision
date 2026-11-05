# AssistiveVision — Architecture (Proposal Phase 2)

The current application is maintained on **`main`**. Fixed commits in the
benchmark guide identify historical experimental versions.

This document supplies the offline-architecture specification, bundled-asset
inventory, initialization/recovery design, device-compatibility and positioning
guidance, audio grammar, instrumentation points, and release reproducibility
required by Phase 2 of the project proposal (integrated architecture, capture
interface, DNN adaptation/integration, audio descriptor module, Android prototype).

This is the implementation/design record. For current, verified build commands and evidence, use [REPRODUCING.md](REPRODUCING.md), [BENCHMARK.md](BENCHMARK.md) and [VALIDATION.md](VALIDATION.md).

---

## 1. Offline architecture (hard guarantees)

```
┌────────────────────────── single Activity, Navigation ──────────────────────────┐
│ MainActivity (edge-to-edge, async locale, permission launcher)                   │
│  ├─ CameraFragment ── CameraFragment → MainViewModel (activity-scoped VM)        │
│  └─ SettingsFragment → SettingsViewModel (DataStore passthrough)                 │
└──────────────────────────────────────────────────────────────────────────────────┘

MainViewModel pipeline (manual capture only):
  CaptureModule (Camera2, TextureView preview, rear ultra-wide preference)
    → tap Capture → JPEG still decoded, rotated and frozen
    → Dispatchers.Default (pipelineMutex single-flight):
        PreprocessingModule (letterbox 448×448, LetterboxMapper geometry)
        DetectionModule (LiteRT EfficientDet-Lite2, synchronized close)
        inverse letterbox mapping (same LetterboxMapper)
        LocalizationModule (6 zones, pure)
        SpeechFormatter (pure sentence assembly, localized via LocalizedLabels)
        AudioModule (bundled sherpa-onnx offline TTS → AudioTrack)
    → unfreeze (onDone / onError / empty result / 20 s watchdog)
```

**Offline guarantees, enforced structurally:**

1. The source manifest requests `android.permission.CAMERA`. The merged debug
   manifest also contains library-added permissions (including `ACCESS_NETWORK_STATE`),
   but no `android.permission.INTERNET`; the app has no network socket permission.
2. The detection model (`efficientdet-lite2-detection-metadata.tflite`, 7.2 MB) and
   both TTS voices are **bundled in the APK** under `assets/`; no downloads exist.
3. `android.speech.tts.TextToSpeech` is **not used**; speech is synthesized in-process
   via the bundled sherpa-onnx native runtime (`com.github.k2-fsa.sherpa-onnx` AAR,
   JitPack, v1.13.6) with `abiFilters arm64-v8a` (devices) and `x86_64` (emulator).
4. Speech initialization performs a one-time copy of the shared `espeak-ng-data`
   phonemization tree from assets to `filesDir/tts/espeak-ng-data`, guarded by a
   version marker (`tts/espeak-version.txt`); bumping `TtsVoiceCatalog.ESPEAK_DATA_VERSION`
   forces a re-copy after upgrades.

## 2. Module inventory

| Module | Responsibility | Key facts |
|--------|----------------|-----------|
| `CaptureModule` | Camera2 preview and JPEG still capture | Separate TextureView preview; rear ultra-wide preference with main-camera fallback; physical/logical camera paths, image rotation and frame ownership |
| `CameraLensPolicy` | Historical pure lens helper | Retained with its unit tests; current `CaptureModule` resolves physical/logical Camera2 choices directly |
| `PreprocessingModule` | Letterbox + tensor packing | 448×448, gray-114 padding, RGB uint8 `ByteBuffer` (allocateDirect), geometry from `LetterboxMapper`; caller-serialized via `pipelineMutex` |
| `LetterboxMapper` | Pure geometry | Forward `Geometry(scale, dx, dy, dstW, dstH)` + exact inverse `toFrameX/Y`; floored forward / exact float inverse; round-trip tested |
| `DetectionModule` | LiteRT inference | `TARGET_LABELS` = 10 COCO classes (ids 1,62,63,65,67,70,72,73,81,82); output tensor shape validated `[1,N,4]`; `detect()`/`close()` synchronized by `inferenceLock` + `isClosed` flag; `MAX_DETECTIONS=15` |
| `LocalizationModule` | Zones | Pure: x<0.35 left, x>0.65 right, y≥0.5 bottom; zero-size frames guarded |
| `AudioModule` | Bundled offline TTS | sherpa-onnx `OfflineTts` + `AudioTrack` streaming; single-thread executor; language-switchable; failure surfaced to UI |
| `SpeechFormatter` / `LocalizedLabels` | Announcement text | Pure formatter + string-resource resolver (EN/ES complete) |
| `SettingsManager` | DataStore Preferences | 5 keys: language ("es" default), confidence (0.30f), maxObjects (2), showPreview (true), speechRate (1.0f) |
| `TtsVoiceCatalog` | Declarative voice map | Pure Kotlin; `specFor("es"|"en")`; es default; espeak asset dir + version constant |

Remote shutter input: `MainActivity.dispatchKeyEvent` maps volume/camera/headset/enter keycodes (`ShutterKeyPolicy`) to `MainViewModel.captureFrame()` while the camera destination is active — Bluetooth remotes pair at the OS level, no extra permission or connectivity code.

## 3. Bundled asset inventory (Phase 2 deliverable: assets + checksums)

All assets live under `app/src/main/assets/`. Total TTS tree: **361 files,
54,956,056 bytes (52.4 MB)**.

| Asset | Size | SHA-256 | License |
|-------|------|---------|---------|
| `efficientdet-lite2-detection-metadata.tflite` | 7,557,887 B | `6fd32c84ab1eb0f7e7f3a7a20a20d7df1530daa8378728f7c79571096286bd52` | Per TensorFlow model garden (Apache-2.0) |
| `tts/vits-piper-es_MX-claude-high-int8/es_MX-claude-high.onnx` | 18.3 MB | `6aa528726d93cf32ee95dd214e2f9708dc956b3e8d3929a9f6fb9ea4a41ed743` | Apache-2.0 (voice MODEL_CARD) |
| `tts/vits-piper-es_MX-claude-high-int8/tokens.txt` | — | `ef3a7e4a8d1af0c9d4dc45aaae1a6242ebe24a7ed6f3d025a49eb29682784c6d` | same as voice |
| `tts/vits-piper-en_US-amy-low-int8/en_US-amy-low.onnx` | 18.7 MB | `f1718264d28bf04916d67b388d4954647e0fd56a2093b1a409eae2a287ec8820` | mimic3-voices (repo top-level CC-BY-SA-4.0) — **confirm per-voice terms before commercial redistribution** |
| `tts/vits-piper-en_US-amy-low-int8/tokens.txt` | — | `42d1a69ed2b91a51928a711aa228ed9f3dc021c6d359a3e9c4f37eb1d20f80bd` | same as voice |
| `tts/espeak-ng-data/` (shared, 355 files) | 17.2 MB | per-file (version-pinned via `ESPEAK_DATA_VERSION`) | espeak-ng derived (GPL-3.0) |

Each voice directory also ships its `MODEL_CARD` (language, sample rate, dataset URL,
license) so provenance travels with the APK. `.onnx.json` runtime configs are
intentionally not bundled (defaults are compiled in). Voice model sources:
`vits-piper-es_MX-claude-high-int8` and `vits-piper-en_US-amy-low-int8` from the
sherpa-onnx `tts-models` release tag.

**Resulting binaries:** `app-debug.apk ≈ 128 MB`, `app-release.apk ≈ 118 MB`
(R8 + resource shrinking; natives for two ABIs ≈ 64 MB uncompressed).

## 4. TTS subsystem design (`AudioModule`)

**Public contract** (unchanged from the platform-TTS era, so `MainViewModel` wiring
and tests stay stable): `Listener { onReady, onDone, onError }`, `speak(text): Boolean`,
`setLanguage(tag)`, `setSpeechRate(rate)`, `stop()`, `shutdown()`.

**Threading.** A single-thread executor (`AudioModule-TTS`) serializes engine init,
voice swap, utterance generation, and teardown — no native use-after-free is possible
because `shutdown()` executes teardown *on the same executor* after in-flight work.

**Initialization.** `init {}` enqueues `ensureVoice(language)`: release old engine →
build `OfflineTtsConfig(vits = OfflineTtsVitsModelConfig(model = "<voiceDir>/<model>",
tokens = "<voiceDir>/tokens.txt", dataDir = <filesDir>/tts/espeak-ng-data),
numThreads = 2)` → `OfflineTts(assets, config)`. Success → `Listener.onReady()`
(and `speechUnavailable=false` in the ViewModel); any failure → engine released +
`Listener.onError()` (localized "voice unavailable" status).

**Playback.** Per utterance: create `AudioTrack` (USAGE_ASSISTANCE_ACCESSIBILITY,
CONTENT_TYPE_SPEECH, PCM float, mono, ≥16 KB buffer, generated session), request
transient-may-duck audio focus, then `generateWithCallback(text, sid=0, speed, cb)`
streaming float samples with `WRITE_BLOCKING`; the callback checks a per-utterance
`AtomicBoolean` so `stop()` aborts promptly. After generation, playback is drained
(poll `playbackHeadPosition` every 30 ms, cap 15 s) so the tail is not clipped, then
focus is abandoned and the track released. `onDone` is posted only when not aborted —
preventing the next capture from being unfrozen by a preempted utterance.

**Language switch.** `setLanguage` looks up `TtsVoiceCatalog.specFor` (case/whitespace
insensitive; only "es"/"en" are bundled). Switching rebuilds the engine on the
executor; a failed init can be retried by any later `setLanguage`.

**Failure states (FR-13).** `speak()` returns false only when the executor was shut
down; while uninitialized/failed it queues nothing and the UI keeps showing the
unavailable state until a retry succeeds. The 20 s watchdog in `MainViewModel`
guarantees a stuck engine can never freeze the capture flow permanently.

**Speed.** `speechRate` is clamped to 0.1–3.0 and passed as the synthesis speed
factor (DataStore default 1.0; UI exposes 0.5–2.5).

## 5. Initialization, recovery, and lifecycle

- Camera binds **only after** `CAMERA` is granted (`CameraFragment` permission
  launcher); denial disables capture with a localized explanation.
- `DetectionModule` validates the output tensor shape at construction and throws with
  a clear message on mismatch (fail fast, not silent corruption).
- Capture lifecycle: `captureFrame()` → frozen frame posted (bitmap **copied**, so the
  analyzer's next frame cannot mutate it) → pipeline on `Dispatchers.Default` under
  `pipelineMutex.tryLock` → `finally` resets `_isCapturing` → release happens on
  `onDone`/`onError`/empty-result/**20 s watchdog**.
- `onCleared`: audio teardown + interpreter close (interpreter close is lock-protected
  against in-flight inference; audio teardown serializes on its executor).
- Bitmap hygiene: previous live frame recycled when replaced; frozen copy owned by
  the pipeline; preprocessing recycles intermediates but never the caller's bitmap.

## 6. Device compatibility (Phase 2 deliverable: prototype assumptions)

| Item | Requirement |
|------|-------------|
| OS | Android 8.0 (API 26) or newer; target SDK 36, compile SDK 37.1 |
| ABIs | arm64-v8a (physical devices), x86_64 (emulator) — both bundled |
| Camera | Rear camera required (`uses-feature android.required=true`); 0.5× ultra-wide optional (better close-range framing) |
| RAM | ≥ 3 GB recommended (model 7.2 MB mmap + ONNX voice ~18 MB + AudioTrack buffers) |
| Storage | Debug APK about 129 MiB, including native libraries and assets; additional internal espeak copy on first run |
| Latency | ~138 ms single inference measured on Snapdragon 685 reference device (to be re-verified in Phase 3) |

## 7. Camera positioning and operating guidance (proposal: "phone positioning")

- The phone is held with the **rear camera facing the scene**, portrait, roughly
  perpendicular to the object of interest; the ultra-wide lens gives a wider
  field of view for close furniture.
- Usable range for the 448×448 model: roughly 0.5–3 m depending on object size;
  farther objects still announce but boxes are small and confidence drops.
- Hold the phone steady for ~1 s before Capture — the announced frame is the latest
  captured one; motion blur reduces confidence.
- The frozen frame stays on screen with boxes until the announcement finishes, so a
  companion can see what was announced.

## 8. Audio descriptor grammar (proposal: "audio descriptor module")

Natural sentences with connectors (localized EN/ES in `speech_*` string resources;
assembly in `SpeechFormatter.announce`):

| Situation | Spoken output |
|-----------|---------------|
| One object | ES: `"Hay una persona donde estás apuntando."` / EN: `"There is a person where you're pointing."` |
| 2 objects | ES: `"Hay una persona donde estás apuntando y una silla a la izquierda de donde estás apuntando."` |
| 3+ objects | ES: `"Hay …, también hay una silla abajo a la izquierda de donde estás apuntando y una computadora arriba a la derecha de donde estás apuntando."` — middle items repeat `también hay`, last joins with `y` |
| Nothing detected | No speech; UI shows 0 objects and unfreezes immediately |
| Grammar | Each item = article + noun (`speech_class_*`, e.g. `una silla` / `a chair`) + zone phrase (`zone_*`, relative to "where you're pointing") |
| Order / cap | Confidence ranking, top-`maxObjects` (1–3) |
| Rate | User-configured 0.5×–2.5× (synthesis speed clamped 0.1–3.0) |

Prioritization = confidence ranking (top-`maxObjects`); repetition = none needed
because each capture speaks exactly once (QUEUE-style stacking is impossible by
design; a new capture interrupts nothing since capture is locked while speaking).

## 9. Performance / battery / thermal instrumentation (feeds Phase 3)

Implemented hooks and where to measure:

- **Pipeline latency:** time `runPipeline` preprocess → detect → localize sections in
  `MainViewModel` (log per stage) — needed for p50/p95 breakdown (Evaluation Plan §6).
- **TTS synthesis:** measure `generateWithCallback` wall time vs. audio duration per
  utterance (`AudioModule`) — reported as real-time factor.
- **End-to-end:** timestamp capture tap → `onDone`.
- **Battery/thermal/memory:** use Android Studio Profiler / `dumpsys batterystats`,
  `dumpsys thermalservice`, `adb shell dumpsys meminfo` during a scripted capture
  session (Evaluation Plan §7); no in-app instrumentation required.

## 10. Build and release reproducibility

- Toolchain: Gradle wrapper 9.5.0 (SHA-pinned), AGP 9.3.1, JDK 21 (Android Studio JBR).
- Dependencies pinned in `gradle/libs.versions.toml` (CameraX 1.6.1, LiteRT 1.4.2,
  sherpa-onnx v1.13.6 via JitPack, Navigation 2.7.7, DataStore 1.1.1).
- Release: R8 minification + resource shrinking; `proguard-rules.pro` keeps
  `org.tensorflow.lite.**` and sherpa-onnx JNI surface.
- Signing: `keystore.properties` (storeFile/storePassword/keyAlias/keyPassword) at
  repo root; debug-signing fallback when absent so `assembleRelease` always builds.
  **User-owned:** create the real keystore before distributing.
- Asset verification: compare SHA-256 hashes in §3 after any voice/model update;
  bump `TtsVoiceCatalog.ESPEAK_DATA_VERSION` when espeak data changes.
- Quality gates: `:app:testDebugUnitTest` (31 tests), `lintDebug` (0 errors),
  `assembleDebug`, `assembleRelease`; instrumented suite includes
  `TtsOfflineInstrumentedTest` (assets present, offline init + speak completes,
  espeak copy on disk) — run via `connectedDebugAndroidTest` on a device/emulator.
