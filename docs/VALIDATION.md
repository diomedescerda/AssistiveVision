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

The workflows were parsed locally and the same build/analysis commands passed.
**GitHub Actions itself has not been run for these local changes.** APK download
artifacts appear after the workflow is committed, pushed and completes on GitHub.
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
