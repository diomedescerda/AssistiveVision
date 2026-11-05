# Contributing

The main branch is **`main`**. Base new work and pull requests on `main`;
use `git switch main` and `git pull --ff-only origin main` to update an existing clean checkout.

Start with the [build guide](docs/REPRODUCING.md). Keep the app build independent
of the research dataset. Do not commit SDK paths, signing keys, virtual
environments, Gradle outputs or APKs. Publish downloadable APKs through
[GitHub Releases](docs/RELEASES.md) after the verified `main` build.

Before proposing a code change, run `python3 scripts/verify_assets.py` and
`./gradlew :app:testDebugUnitTest :app:lintDebug`. For capture, geometry, model,
frame ownership or speech changes, also run the relevant instrumentation tests
and [Android dataset replay](docs/BENCHMARK.md) on the intended target.

When changing a bundled model/voice, preserve the previous experiment's model
and results in the companion repository. Review its upstream terms, intentionally
update `docs/assets.sha256.json`, and record the source/version and affected
measurements. Never change hashes merely to silence an unexpected mismatch.

Report issues with the commit, build type, Android version/ABI, device or emulator,
steps to reproduce, expected/actual behavior and relevant logs. Camera photos
are optional; avoid sharing private scenes when a minimal example is sufficient.

Keep ad hoc diagnostics, draft generators and local work under `development/`.
Generated auxiliary outputs belong in ignored `development/runs/`; required
app tests stay in `app/src/test` and `app/src/androidTest`.
