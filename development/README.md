# Intermediate development material

[Project home](../README.md)

This directory groups auxiliary work outside the app's main source/build path.

| Directory | Content |
|---|---|
| `validation/` | Local build, signature and instrumentation logs supporting the validation summary |
| `presentations/` | Optional voice-feedback slide generator |
| `runs/`, `scratch/` | Generated development outputs, ignored by Git |

`relocations.json` records the original paths and file hashes before the move.
The app source, bundled assets, Gradle files and required unit/instrumentation
tests stay in their standard directories. `dist/` remains the APK distribution
directory and is ignored by Git. Gradle and Android Studio manage their own
ignored caches; those are not part of the source delivery.

The optional slide generator writes to `development/runs/presentations/`.
It is not required to build, install or run the app. Use the root README and
`docs/REPRODUCING.md` for the supported build workflow.
