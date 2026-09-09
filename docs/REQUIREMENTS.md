# AssistiveVision — Requirements & Conceptual Design (Proposal Phase 1)

This document supplies the measurable requirements, safety boundaries, privacy policy,
target-user definition, object-class mapping, model-selection evidence, offline-TTS
decision, and risk/ethics planning required by Phase 1 of the project proposal
(literature/problem analysis, requirements & conceptual design, DNN selection,
priority-object definition, audio descriptor design).

Status: **implemented baseline** — every requirement below is either implemented in
this repository or explicitly listed as a documented limitation / user-side activity.

---

## 1. Problem statement and target users

**Problem.** Blind and low-vision users need on-demand awareness of nearby objects
(people, furniture, appliances) in indoor spaces. Commercial solutions are expensive,
cloud-dependent, or language-limited.

**Primary users.** Adults with visual impairment (blind or severe low vision) who own
or can borrow an Android 8.0+ smartphone, and who communicate in Spanish or English.
Spanish is the default language of the app (new installs start in Spanish).

**Secondary users.** Researchers/caregivers running the Phase 3 evaluation, and
developers extending the class set or voices.

**Accessibility requirements (A-1…A-6).**

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| A-1 | All primary information must be **spoken**, not only displayed | Every detection result is announced through the bundled offline TTS; the app never depends on on-screen text alone |
| A-2 | UI text must meet WCAG AA contrast | Automated lint + manual audit; all text ≥ 4.5:1 (normal) or ≥ 3:1 (large); implemented via `#B3FFFFFF` minimums, darker overlay tag `#14785E`, header scrim |
| A-3 | Controls must be usable with TalkBack | Real button roles, `labelFor` on all labeled controls, decorative images excluded (`importantForAccessibility=no`), status row is a polite live region |
| A-4 | Text must scale with system font size | Overlay label text is specified in `sp`; no fixed `px` text sizes in the overlay |
| A-5 | Touch targets ≥ 48 dp | All interactive controls ≥ 48 dp (back button enlarged from 46 → 48 dp) |
| A-6 | Both UI languages fully translated | `values/` and `values-es/` string tables have identical key sets; per-app locale switching recreates activities so no mixed-language screens |

---

## 2. Functional requirements

Detection is **on-demand (manual capture)**: the user aims the phone and taps
Capture; the current frame is frozen, analyzed, announced once, and released. This is
a deliberate product decision — one capture, one clear announcement — chosen for
reliability and comprehensibility over continuous streaming (see §9 safety note and
the proposal-revision note).

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| FR-1 | Freeze-and-capture control | Tap Capture freezes the live preview, runs the pipeline once, and disables re-entry while a capture is in flight |
| FR-2 | Object detection on-device | EfficientDet-Lite2 TFLite (448×448, RGB) runs via LiteRT on the device; no network call exists in the code path |
| FR-3 | Target classes | Exactly the 10 classes in §4 are announced; other COCO classes are ignored |
| FR-4 | Confidence filter | Only detections with score ≥ the user threshold (DataStore default `0.30f`, configurable 0.10–0.90) are announced |
| FR-5 | Announcement cap | At most `maxObjects` (default 2, range 1–3) objects are spoken, ranked by confidence |
| FR-6 | Spatial localization | Each announced object includes one of six zones (left/center/right × top/bottom) computed from the bbox center in frame coordinates |
| FR-7 | Localized speech | Object names and zones are spoken in the selected language (Spanish default); sentence assembly is pure (`SpeechFormatter`) and unit-tested |
| FR-8 | Overlay | Bounding boxes + label tags are drawn over the frozen frame using the same letterbox geometry forward and inverse (round-trip tested) |
| FR-9 | Camera permission gating | The camera never binds before `CAMERA` is granted; denial is explained in-app in the UI language |
| FR-10 | Lens selection | The back camera with the lowest focal length is chosen (0.5× ultra-wide when present), so close objects fill the frame |
| FR-11 | Settings persistence | Language, speech rate (0.1–3.0 applied, UI 0.5–2.5), confidence, max objects, preview toggle persist via DataStore |
| FR-12 | Per-app language switch | EN/ES switch takes effect immediately via `AppCompatDelegate.setApplicationLocales` with `localeConfig`; spoken labels switch on the next capture |
| FR-13 | Graceful TTS failure | If the bundled voice cannot initialize, the UI shows a localized "voice unavailable" status; captures are never stuck (watchdog + error unfreeze) |
| FR-14 | Remote shutter trigger | A paired Bluetooth camera remote (e.g. a GorillaPod grip button) triggers capture on the camera screen: volume up/down, camera, headset, and enter keycodes map to capture; volume keys keep their default behavior outside the camera screen |

---

## 3. Non-functional requirements

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| NFR-1 | **Fully offline** | Manifest declares only `android.permission.CAMERA` — **no INTERNET permission**. All inference and speech synthesis happen on-device from bundled assets. Verified by airplane-mode test (see Evaluation Plan §5) |
| NFR-2 | End-to-end response time | Capture → speech completion completes within ~2.5 s p95 on the reference device (inference ~138 ms + short utterance synthesis); measured in Phase 3 |
| NFR-3 | No stuck captures | A capture always resolves: success, empty result, TTS error, or 20 s watchdog timeout releases the UI |
| NFR-4 | Crash safety | Interpreter close is synchronized with in-flight inference; ImageProxy close is in `finally`; bitmap access is `@Volatile`/copied |
| NFR-5 | Privacy | No image, audio, or usage data leaves the device; nothing is written to shared storage; no analytics, ads, or third-party SDKs that phone home |
| NFR-6 | Install size budget | Voice + model assets ≤ 60 MB; release APK ≤ 120 MB (actual: assets 52.4 MB, release APK ≈ 118 MB) |
| NFR-7 | Compatibility | minSdk 26; ABIs arm64-v8a (physical) + x86_64 (emulator); runs on Android 8.0+ |
| NFR-8 | Release hardening | R8 minification + resource shrinking; reproducible signing via `keystore.properties` with debug fallback |
| NFR-9 | Automated quality gates | Unit tests, lint (0 errors), debug + release builds all green in CI/local verification |

---

## 4. Priority objects and class mapping (proposal activity: "priority-object definition")

The proposal cites doors, chairs and desks as example priority objects. Available
open models constrain what is detectable: the shipped EfficientDet-Lite2 is trained
on MS COCO, and only these COCO classes are enabled (IDs are the 1-indexed COCO
labels used by the model output):

| App class | COCO ID | Proposal example? | Notes |
|-----------|---------|-------------------|-------|
| person | 1 | (implied) | Highest priority: people around the user |
| chair | 62 | ✅ chairs | Direct match |
| couch | 63 | — | Indoor furniture |
| bed | 65 | — | Indoor furniture |
| table | 67 | ≈ desks | COCO has no separate "desk"; dining/work tables are the nearest supported class — **documented limitation** |
| toilet | 70 | — | Orientation landmark |
| tv | 72 | — | Appliances/screens |
| computer | 73 | — | Laptops/desktops |
| sink | 81 | — | Kitchen/bathroom landmark |
| refrigerator | 82 | — | Appliances |

**Not covered (explicit limitation).** Doors are not a COCO class and are therefore
not announced. This is stated as a known gap for the final report; closing it would
require either a different open model with door/cabinet classes, fine-tuning on a
custom annotated dataset (out of scope of the current phases), or accepting the
nearest-class behavior documented here.

---

## 5. DNN selection evidence (proposal activity: "DNN selection/evaluation")

**Choice:** EfficientDet-Lite2 (TensorFlow Lite / LiteRT), input 448×448.

Rationale against the alternatives considered during Phase 1:

| Option | Why / why not |
|--------|---------------|
| EfficientDet-Lite0/Lite1 | Smaller/faster but lower accuracy on small indoor objects; Lite2 keeps ~138 ms single-shot latency on the reference device while staying on-device |
| EfficientDet-Lite3/Lite4 | Better mAP but noticeably slower on mid-range phones and larger APK; exceeds the NFR-2 budget on low-end hardware |
| YOLO-family TFLite exports | Faster but the available metadata-compatible export pipeline and COCO label conventions of the chosen artifact were cleaner for this project; can be revisited |
| SSD MobileNet | Weaker accuracy/size tradeoff at 448-class granularity |

**Selection protocol (reproducible).** Model choice is justified by (a) the official
model card / published mAP, (b) on-device single-inference latency on the reference
device (see README Performance — to be re-verified in Phase 3 per the Evaluation
Plan, not treated as final), and (c) asset size budget NFR-6. The shipped artifact is
`app/src/main/assets/efficientdet-lite2-detection-metadata.tflite`
(SHA-256 `6fd32c84…6bd52`, full hash in `docs/ARCHITECTURE.md` §3).

**Confidence threshold selection.** Default 0.30 balances missed objects vs. false
announcements for assistive use; Phase 3 sweeps 0.10–0.90 to plot the precision/recall
trade-off and validate (or revise) the default with data.

---

## 6. Offline TTS decision (proposal activity: "audio descriptor design")

**Requirement.** The user requires *all* functionality offline, explicitly including
speech synthesis. Spanish and English voices are required; Spanish is default.

**Decision.** Platform `android.speech.tts.TextToSpeech` is **not used**. It is only
an interface to whatever engine/voice the device vendor ships, cannot guarantee that
voice data is present or offline, and varies per OEM — unacceptable for a
guaranteed-offline assistive app.

**Implemented architecture.** The app bundles the **sherpa-onnx** offline TTS runtime
(Apache-2.0) with two **Piper VITS** voices shipped inside the APK:

| Language (default) | Voice package | Speaker / quality | Sample rate | License |
|--------------------|---------------|-------------------|-------------|---------|
| `es` (default) | `vits-piper-es_MX-claude-high-int8` | male, Latin-American Spanish, high | 22 050 Hz | Apache-2.0 (MODEL_CARD) |
| `en` | `vits-piper-en_US-amy-low-int8` | female, US English, low | 16 000 Hz | mimic3-voices repo — top-level CC-BY-SA-4.0; per-voice attribution to be confirmed before commercial redistribution |

Phonemization data (`espeak-ng-data`, GPL-3.0-derived) is shared by both voices and
copied once from assets to internal storage (versioned marker file forces re-copy on
upgrade). Speech is synthesized locally through JNI and streamed to an
`AudioTrack` (accessibility usage, transient-may-duck focus). There is **no network
and no platform-TTS fallback**: if the bundled engine or voice fails to initialize,
the UI reports "voice unavailable" and the app degrades visibly (FR-13).

Audio descriptor grammar (what is spoken): natural sentences with connectors and
articles, e.g. `"Hay una persona donde estás apuntando, también hay una silla
abajo a la izquierda de donde estás apuntando y una computadora arriba a la
derecha de donde estás apuntando."` (English: `"There is a person where you're
pointing, there is also a chair to the lower left of where you're pointing and a
computer to the upper right of where you're pointing."`), up to `maxObjects`
items. Full spec in `docs/ARCHITECTURE.md` §8.

---

## 7. Privacy, data handling, and network policy

- **Permissions:** `CAMERA` only. No INTERNET, no microphone, no location, no storage.
- **Camera frames** exist only in process memory (latest-frame slot + one frozen
  copy during a capture); they are never written to disk and never leave the device.
- **Settings** (language, rate, confidence, max objects, preview) are stored locally
  in DataStore Preferences. They are not backed up off-device by any custom policy
  (platform auto-backup remains default Android behavior; the backup rule files are
  placeholders — acceptable for this data class, revisit before store release).
- **Telemetry:** none. No analytics/crash-reporting SDKs are integrated.
- **Third-party runtimes** (LiteRT, sherpa-onnx) run fully in-process with no network
  use; this is enforced structurally by the absence of the INTERNET permission.

## 8. Risks and mitigation

| Risk | Mitigation |
|------|------------|
| User relies on the app for safety-critical navigation | §9 non-goals; spoken disclaimers in user-facing docs; evaluation includes negative cases |
| Voice assets fail to load (corrupt APK, low storage) | Startup surfaces a localized error state; asset checklist + checksums in Architecture doc |
| Latin-American Spanish voice accent mismatch for some users | Default voice is es-MX per the target population (Universidad del Magdalena, Colombia); alternate voices can be swapped via the pure `TtsVoiceCatalog` |
| APK size blocks low-storage devices | int8-quantized voices (~18 MB each), shared espeak data, R8 + resource shrinking |
| Obstacle occlusion / poor lighting degrade detection | Explicitly part of the Phase 3 scenario matrix rather than hidden |
| Ethical: testing with visually impaired participants | Recruitment/consent protocol required before any human study (Evaluation Plan §8); no data collection in this repo |

## 9. Scope boundaries (non-goals)

AssistiveVision is **not**: a navigation system, a collision-avoidance device, a
white-cane replacement, a continuous-scene streamer, or a cloud service. It provides
on-demand, one-shot scene descriptions. It must never be presented as safe for
walking while distracted; the final report must state this explicitly.

## 10. Traceability to proposal Phase 1

| Proposal activity | Where satisfied |
|---|---|
| Literature/problem analysis | `docs/REQUIREMENTS.md` §1 (this file) + final report (user-owned) |
| Requirements & conceptual design | §1–§3, §7–§9 |
| DNN selection/evaluation | §5 |
| Priority-object definition + datasets (COCO / Open Images) | §4 |
| Audio descriptor design | §6 + `docs/ARCHITECTURE.md` §8 |
