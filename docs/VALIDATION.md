# Verified build and experiment checks

The main application branch is **`main`**. The dated validation below records
the specific historical sources checked at that time; merging into `main` does
not turn those results into a new device test.

[Home](../README.md) · [Build instructions](REPRODUCING.md) · [Benchmark](BENCHMARK.md)

Validation date: **5 November 2026**, Linux x86_64. Source basis:
`85c852ddcb543bce1bd69ad0db307f895d597881` with the new documentation, asset
verification, packaging scripts and workflows. Production detection modules
and model match the historical experiment. The fresh-source check used an
independent temporary Git snapshot, without app build outputs or `local.properties`;
that snapshot's commit is a local verification identifier, not a published revision.

| Check | Observed result |
|---|---|
| Build from clean source | Successful, JDK 21, Gradle 9.5.0, SDK Platform 37.1 |
| Local JVM tests | **34 passed**, zero errors, failures or skipped tests |
| Android lint | **0 errors, 28 existing warnings**; passing lint does not mean warning-free |
| Bundled assets | All **362** model, voice and phonemization files matched SHA-256 |
| Packaged APK assets | Every bundled file also matched after extracting from the APK |
| Signature | `apksigner verify --verbose --print-certs` passed; Android debug certificate |
| APK metadata | Min SDK 26, target 36, arm64-v8a and x86_64; no INTERNET permission |
| Package size | 134,267,414 bytes, approximately 128.0 MiB |
| Instrumentation | **Five passed**, including Spanish voice synthesis and frame-store checks; run with emulator airplane mode enabled |
| Fresh dataset replay | **198/198** prediction arrays identical to the archive; non-timing summary fields also equal |

Evidence: [build check summary](../development/validation/build-checks.json),
[clean build log](../development/validation/clean-build.log),
[signature verification](../development/validation/apk-signature.txt),
[offline instrumentation log](../development/validation/offline-instrumentation.log).
The [companion validation record](https://github.com/diomedescerda/indoor-detect/blob/main/docs/VALIDACION.md)
preserves the new raw Android replay and the complete analysis comparison report.

The target was the Pixel 8 **emulator profile**, API 37, x86_64, 16 KB pages.
This is not physical Pixel 8 performance or validation of every supported phone.
The instrumentation confirms execution/completion of Spanish synthesis; it does
not measure intelligibility with listeners or establish English pronunciation quality.
Live camera quality and perception tests require their own experimental evidence.

The original validation above used locally parsed workflows and local commands.
Subsequent GitHub Actions checks are recorded separately below. APK download
artifacts appear only after a workflow completes successfully on GitHub.
The local APK is in `dist/`, excluded from Git, with its current checksum, build
manifest and signature report. Different computers can produce different signed
APK bytes even when source and model checks pass.

## Integration on main

On 5 November 2026, the reproduction/documentation changes were merged into
`main`, preserving the Camera2 changes from `7b33992`. The merged sources passed
34 JVM tests, Android lint (zero errors, 30 warnings), `assembleDebug`,
`assembleDebugAndroidTest`, and all 362 asset checksums. See the
[main integration record](../development/validation/main-integration.json) and
[build log](../development/validation/main-integration-build.log). This check
did not execute instrumented tests or test physical camera selection.

## GitHub Actions SDK setup correction

The first two GitHub runs stopped in SDK setup before compilation because the
setup action default requested the retired `tools` package. Commit `d78ffa7`
sets its package list to `platform-tools` and retains the pinned platform and
build-tools installation. The full build and artifact upload pass in the
[corrected main run](https://github.com/diomedescerda/AssistiveVision/actions/runs/42096695975).
A separate clean local build of that commit completed all 87 Gradle tasks and
passed packaged-asset, signature and APK metadata checks; its APK checksum is
`23580339d68d1662206eef95d15864569ba896207ae9442d2918728b3a54a453`.
See the [SDK correction record](../development/validation/ci-sdk-fix.json).

## Downloadable research release

The public APK is provided through [GitHub Releases](https://github.com/diomedescerda/AssistiveVision/releases/latest), with an individual [APK download](https://github.com/diomedescerda/AssistiveVision/releases/latest/download/AssistiveVision-1.0-debug.apk). Version `v1.0.0` preserves the successful CI build of `main` commit `d78ffa7127a5e133970a50f2f941b6b3bdc20ce6`. Its APK contains 134,267,426 bytes and has SHA-256 `883078e73596d2861029449debd796e366de4388fafe7a838c78fadef52f0820`. The downloaded CI package was checked against its manifest and checksum; all 362 embedded assets and its APK signature were verified before publication.

See [release publication and verification](RELEASES.md) for the source snapshot, published metadata and future version procedure. The release build did not add new device or listener experiments.

## README reader journeys checked on 5 November 2026

The [combined evidence record](../development/validation/readme-e2e-20261105.json)
covers all 16 README/LEEME files across the app and research repositories,
including explicitly classified archives. A clean independent `main` checkout
completed the full 87-task build, 34 JVM tests, lint with zero errors and 30
warnings, and all 362 asset checks. Five voice/frame tests passed in a dedicated
API 37.2 x86_64 16 KB emulator with airplane mode enabled.

The public APK was downloaded completely without authentication, matched its
published checksum, installed and launched, and detected the virtual-scene TV
with Spanish speech synthesis. Language and object limit persisted across a cold
restart. A local APK signed with another debug certificate was correctly rejected
without erasing that installation; device tests used a separate emulator.

**Known v1.0.0 first-launch issue:** after granting camera permission, Capture can
remain disabled despite a working preview. Force-stopping and relaunching once restored
capture in this test. The [use guide](USO.md#primera-captura) documents the recovery;
this documentation change does not fix the production app itself.

The companion fresh-clone runs matched 27 CSV/two JSON files and all 198 Android
prediction arrays. The historical replay completed with the documented 16 metric
differences. The review-page export/restore and 73-page editorial reconstruction
also passed. Current navigation, setup, safe output paths and release rebuilding
instructions were clarified from these checks.

These are Linux/emulator checks with installed SDK and dependency caches. They
are not Windows/macOS execution, a phone browser's package-installer flow, physical
camera/Bluetooth validation or listener clarity measurements. Publication guidance
was reviewed without creating an unnecessary new public release.

## Reader workflows rechecked on 9 November 2026

The current `main` source `b99e41971913ceaac2e5a2928c1e4053997dffba`
was cloned from GitHub and built without pre-existing build outputs or
`local.properties`, using JDK 21 and SDK 37.1. The build passed **34 JVM tests**,
lint with **zero errors and 30 warnings**, app/test assembly, all **362** source
and packaged asset hashes, signature verification and APK metadata checks.

The public `v1.0.0` APK was downloaded without authentication and matched its
published SHA-256. Its ZIP contents, including runtime bytecode and resources,
match the clean local `main` build; the signed APK files differ. A new dedicated
API 37.2 x86_64 emulator with 16 KB pages completed these checks with airplane
mode enabled and Wi-Fi/mobile data disabled:

| Reader action | Observed result |
|---|---|
| Install, open and grant camera access | Successful; Capture was enabled after permission in this run |
| Capture with no selected detections | Returned to ready without an object announcement |
| Detect and announce the virtual-scene TV | English and Spanish PCM generated; positive scene checks used confidence 0.10 and speech rate 2.0 |
| Hide preview, force-stop, reopen and capture | Capture and English speech still worked |
| Change language, rate, confidence, object cap and preview | All five settings survived a cold restart |
| Fullscreen and hardware shutter key | Controls toggled; injected camera keycode 27 triggered Spanish capture |
| Deny camera permission | Capture disabled with the localized permission explanation |
| Install a differently signed local APK over the public APK | Rejected with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; public installation preserved |

The virtual TV scored below the default 0.30 in these captures. Lowering the
threshold exercised the positive speech path; it is not a new detection-accuracy
study or evidence that every object is recognized. The first-permission issue
recorded on 5 November was not reproduced in this run; its recovery instructions
remain available for affected installations.

On a separate dedicated emulator, the documented command passed **five offline
voice/frame-store instrumentation tests**. A separate checkout of historical
commit `85c852ddcb543bce1bd69ad0db307f895d597881` replayed all **198** study
photographs: every prediction array and all non-timing summary values matched
the reference. Saved-result analysis matched **27 CSV and two JSON files**.

The [complete evidence record](../development/validation/readme-e2e-20261109.json),
[build log](../development/validation/readme-e2e-20261109/app-build.log),
[offline test log](../development/validation/readme-e2e-20261109/offline-instrumentation.log)
and [live app log](../development/validation/readme-e2e-20261109/public-app-logcat.txt)
preserve the checks. SDK, dependency downloads and research Git objects were
cached; the research clone was dissociated and all environments/results were new.

The author confirms successful use on their Redmi Note 13. No physical phone was
connected during this audit, so that report is distinct from these emulator
checks. Physical Bluetooth pairing, Windows/macOS, listener comprehension and
Word desktop Zotero refresh were not executed here. App production code and the
published release were not changed by this documentation verification.
