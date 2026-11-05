# Publish and reproduce downloadable APKs

The application is maintained on **`main`**. Users download the APK directly from
[the latest release](https://github.com/diomedescerda/AssistiveVision/releases/latest);
[the direct APK link](https://github.com/diomedescerda/AssistiveVision/releases/latest/download/AssistiveVision-1.0-debug.apk) skips the release page.
Releases retain their files until a maintainer removes them. CI artifacts remain
secondary build evidence with a 30-day retention period.

## Rebuild the published v1.0.0

Download `build-manifest.json` from the same version page as the APK and read
`source_commit`. The first release `v1.0.0` was built from
`d78ffa7127a5e133970a50f2f941b6b3bdc20ce6`, an ancestor of `main`:

```sh
git clone --branch main https://github.com/diomedescerda/AssistiveVision.git AssistiveVision-release
cd AssistiveVision-release
git checkout d78ffa7127a5e133970a50f2f941b6b3bdc20ce6
```

Keep this checkout. Apply only the JDK/SDK environment setup and build command
for your operating system from the [current build guide](REPRODUCING.md); skip
that guide's clone and `cd AssistiveVision` commands. For another version, use **its** manifest
commit. Checking out a fixed commit leaves Git in detached-HEAD mode; that is
expected for this separate reproduction copy. Keep ongoing development on `main`.
The current `main` README can evolve after a released binary was built.

## Publish a new version from main

1. Merge the intended app changes into `main`; update the app version and
   [release notes](RELEASE_NOTES.md) when appropriate.
2. Open **Actions → Build and verify Android APK → Run workflow**, select
   **Branch: main**, and enter a new release version such as `v1.0.1` in
   **release_tag**. Leave it empty for a verification build without publication.
3. The workflow validates the version, builds from clean sources, runs tests and
   lint, and verifies assets, APK signature and metadata. Only a successful build
   can publish the release. It uploads the APK and verification/license files
   individually and updates GitHub's latest-release link.

Equivalent command for an authenticated maintainer:

```sh
gh workflow run android.yml --ref main -f release_tag=v1.0.1
```

Use a new version for each publication. Existing releases and their binaries are
not overwritten. The workflow creates the tag at its exact `main` commit. The
public APK filename remains `AssistiveVision-1.0-debug.apk` for the current 1.0
application; if that filename changes, update both repositories' direct links.

## Verify or rebuild a specific download

Each release includes `SHA256SUMS`, `build-manifest.json`, `apk-signature.txt`,
`apk-metadata.txt`, `LICENSE` and `THIRD_PARTY.md`. The manifest's `source_commit`
is the source of the binary. The release tag is a fixed snapshot; `main` and the
latest-download link can advance. Follow [REPRODUCING.md](REPRODUCING.md) to build
the current app, or check out that exact source commit in a separate clone to
rebuild a selected release. Compare the published APK with its own checksum.

The distributed APK is debug-signed for research. A fresh GitHub runner may use
a different debug certificate for another release; an update across certificates
can be rejected by Android. Preserve app data before changing installations.
This procedure does not claim byte-identical rebuilds or a production signing key.
