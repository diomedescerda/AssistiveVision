# Reproduce the app's experimental results

The current application and build guides are maintained on **`main`**. The
reference commit below is a historical experiment snapshot; its explicit checkout
is required for that replay and does not change the main branch.

[Home](../README.md) · [Download APK](https://github.com/diomedescerda/AssistiveVision/releases/latest/download/AssistiveVision-1.0-debug.apk) · [Build the APK](REPRODUCING.md)

The data and analysis live in
[indoor-detect](https://github.com/diomedescerda/indoor-detect). They are kept out
of this app repository so the app build does not require research photographs.
The experiment's reference app commit is
`85c852ddcb543bce1bd69ad0db307f895d597881`. Its exact detector and production
module hashes are preserved with the data.

## Recompute tables and figures without Android

```sh
git clone https://github.com/diomedescerda/indoor-detect.git
cd indoor-detect
python3.12 -m venv .venv
.venv/bin/python -m pip install -r requirements-analysis.lock.txt
.venv/bin/python scripts/reproduce.py --overlays
```

The runner verifies the archive, validates GT, runs the analysis tests,
regenerates mount/lens, scene, occlusion, distance and digital-brightness analyses,
and compares the full numerical tables with the archived results. Outputs go
to `development/runs/saved-results/`; existing outputs are never overwritten.
On Windows use `.venv\Scripts\python.exe`. See the companion repository's
[full reproduction guide](https://github.com/diomedescerda/indoor-detect/blob/main/docs/REPRODUCIR.md)
for the expected outputs and interpretation limits.

## Replay the photographs through Android

Use a dedicated test target. A published APK and a locally built APK may use
different debug certificates, so a fresh emulator avoids replacing a daily-use
installation or its settings. The runner stops at a signing conflict.

Create sibling checkouts, prepare the Python environment above, and install the
Android SDK/JDK described in [REPRODUCING.md](REPRODUCING.md). For the historical
experiment, use a separate app checkout pinned to the reference commit:

```sh
git clone --branch main https://github.com/diomedescerda/AssistiveVision.git AssistiveVision-reference
git -C AssistiveVision-reference checkout 85c852ddcb543bce1bd69ad0db307f895d597881
cd indoor-detect
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME=/path/to/jdk-21
"$ANDROID_HOME/platform-tools/adb" devices
.venv/bin/python scripts/run_android_benchmark.py --app ../AssistiveVision-reference --sdk "$ANDROID_HOME" --serial emulator-5554
```

Replace `emulator-5554` with the serial of the running emulator or authorized
phone. A target must already be booted; the runner does not create one. Android
Studio Device Manager can create an API 37 x86_64 emulator. The original run used
the Pixel 8 emulator profile and a 16 KB system image; the exact saved fingerprint
is in `Build/Testing/provenance/android_run_metadata.json`.

The runner checks the five production-source hashes and model against the
archive, copies the exact instrumentation test if absent, builds and installs
both debug APKs, stages the 198 photos and GT in app-private storage, runs the
test, checks frame identities/input hashes and evaluates the new predictions.
It uses the Python interpreter that launched it. New runs are isolated under
`development/runs/android-<UTC timestamp>/` with `android.json`, instrumentation log, `comparison_to_archive.json`
and `metrics/`. It does not erase archived results or uninstall an app when
signatures conflict. To investigate changed source/model, explicitly pass
`--allow-different-source`; that run is exploratory, not an unchanged reproduction.

## What to compare

At confidence 0.30 and IoU 0.50, detector TP counts in order
head 1x, head 0.5x, chest 1x, chest 0.5x, selfie stick with tripod 1x, selfie stick with tripod 0.5x are
**56, 70, 65, 77, 75, 82**. Chest 1x has F1 **0.6190476190**;
selfie stick with tripod 0.5x has F1 **0.5714285714**. The latter leads count, not F1.

A fresh emulator replay on 5 November 2026 returned identical prediction arrays
for all 198 frames. Its timings changed, as expected. Do not require milliseconds
to match: host load, emulator, delegate, phone and build configuration affect them.
The benchmark replays iPhone JPEGs through preprocessing/detection. It excludes
camera acquisition, localization, speech synthesis/playback and listener clarity.

The older six-model comparison is a separate historical experiment; see its
[availability record](https://github.com/diomedescerda/indoor-detect/blob/main/Build/models_comparison/README.md).

The six historical model binaries were recovered from Git history. A separate
[recovery/replay guide](https://github.com/diomedescerda/indoor-detect/blob/main/docs/HISTORICO.md)
records an updated-runtime replay and its differences from the published historical tables.
The exact 198-photo Android reproduction is unaffected.

The original benchmark uses the legacy key `gorillapod` for the selfie stick
with folding tripod legs. This is the same mount; the archived identifier and
numerical results are preserved. See the [mount-name mapping](https://github.com/diomedescerda/indoor-detect/blob/main/docs/NOMBRES_DE_MONTAJES.md).
