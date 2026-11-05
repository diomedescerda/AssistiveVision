# Build AssistiveVision from a fresh clone

The supported branch for the current application is **`main`**. All clone commands
below select it explicitly. For an existing clean checkout, run `git switch main`
and `git pull --ff-only origin main` before building. A fixed historical checkout
for experiment replay is documented separately in [BENCHMARK.md](BENCHMARK.md).

[Home](../README.md) · [Uso en español](USO.md) · [Reproduce results](BENCHMARK.md)

## What you need

| Requirement | Version / purpose |
|---|---|
| Git | Download the source and all bundled assets; Git LFS and submodules are not used |
| JDK | 21; set `JAVA_HOME` to its directory |
| Python | 3.12+; build/check scripts use only the standard library |
| Android SDK | Platform `android-37.1`, Build-Tools `36.0.0`, Platform-Tools |
| Gradle / AGP | Wrapper 9.5.0 / Android Gradle Plugin 9.3.1; do not install a separate Gradle |
| Test target | arm64-v8a phone or x86_64 emulator, API 26+ |
| Network / storage | Internet for first Gradle/Maven download; several GB for SDK, dependencies and build outputs |

Android Studio is convenient but not required after the SDK is installed. In
**Settings → Languages & Frameworks → Android SDK**, note the SDK location.
Enable **Show Package Details**, install Android API 37.1 and Build-Tools 36.0.0,
plus Platform-Tools. Set the IDE's Gradle JDK to 21 if building inside the IDE.

With the Android Command-Line Tools installed, the equivalent package command is:

```sh
sdkmanager 'platform-tools' 'platforms;android-37.1' 'build-tools;36.0.0'
sdkmanager --licenses
```

Accept the SDK licenses interactively. `sdkmanager` is supplied by the SDK tools;
it is not a repository script. See [Android SDK package management](https://developer.android.com/tools/sdkmanager).

## Linux and macOS

```sh
git clone --branch main https://github.com/diomedescerda/AssistiveVision.git
cd AssistiveVision
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME="$HOME/Android/Sdk"  # macOS commonly uses $HOME/Library/Android/sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
java -version
python3 scripts/verify_assets.py
python3 scripts/build_apk.py
```

Replace the example JDK and SDK paths with the directories on your computer.
`ANDROID_HOME` makes the command-line build independent of the author's
`local.properties`, which is intentionally excluded from Git.

## Windows PowerShell

```powershell
git clone --branch main https://github.com/diomedescerda/AssistiveVision.git
cd AssistiveVision
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
py -3.12 scripts/verify_assets.py
py -3.12 scripts/build_apk.py
```

To install a published build, use **[Download APK](https://github.com/diomedescerda/AssistiveVision/releases/latest/download/AssistiveVision-1.0-debug.apk)**.
The [release page](https://github.com/diomedescerda/AssistiveVision/releases/latest) contains its checksum, source manifest and license files.
Maintainers publish new versions from successful `main` builds using the
[release procedure](RELEASES.md).

## Expected output and checks

The asset check reports **362 files**. The build script runs `clean`, debug unit
tests, lint, `assembleDebug` and `assembleDebugAndroidTest`. A Gradle failure stops
packaging; an APK from an earlier build is never used as a fallback.

| File | Purpose |
|---|---|
| `dist/AssistiveVision-1.0-debug.apk` | Installable debug app with both voices and detector |
| `dist/SHA256SUMS` | SHA-256 for verifying this specific downloaded APK |
| `dist/build-manifest.json` | Commit, source file hashes, dirty-tree flag, Java, build command and APK hash |
| `dist/LICENSE`, `dist/THIRD_PARTY.md` | App license and bundled-component attribution |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | Separate instrumentation APK; not the user-facing app |
| `app/build/reports/tests/testDebugUnitTest/` | Unit-test report |
| `app/build/reports/lint-results-debug.html` | Lint findings, including existing warnings |

The output directory must be new. For another build, keep the previous package
and run `python3 scripts/build_apk.py --output dist/second-build`.

To verify a downloaded package on Linux, run `sha256sum -c SHA256SUMS` inside
its directory. On macOS use `shasum -a 256 -c SHA256SUMS`; in PowerShell compare
`Get-FileHash .\AssistiveVision-1.0-debug.apk -Algorithm SHA256` with the supplied file.

The APK contains arm64-v8a and x86_64 native libraries. It does not support
32-bit ARM-only phones. Android 8.0 (API 26) is the declared minimum; the recorded
validation uses the API 37 x86_64 emulator, not every Android version or phone.

## Install and run

```sh
adb devices
adb -s YOUR_DEVICE_SERIAL install -r dist/AssistiveVision-1.0-debug.apk
adb -s YOUR_DEVICE_SERIAL shell am start -n com.unimagdalena.assistivevision/.ui.MainActivity
```

Enable USB debugging and accept the device authorization prompt. Replace
`YOUR_DEVICE_SERIAL` with an actual serial printed by `adb devices`. Grant the
camera permission in the app. Alternatively, copy the APK to the phone and open
it using the system package installer; allow installation from that source.

## Device tests

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.unimagdalena.assistivevision.tts.TtsOfflineInstrumentedTest,com.unimagdalena.assistivevision.model.FrameStoreInstrumentedTest
```

Use one authorized target for this command. These five tests check bundled
voice assets, Spanish synthesis completion and frame copying/concurrency.
They do not measure perceived clarity or English pronunciation. The separate
198-photo replay is described in [BENCHMARK.md](BENCHMARK.md); do not run it
without staging its dataset first.

## Troubleshooting

| Symptom | Check / action |
|---|---|
| `SDK location not found` | Set `ANDROID_HOME` to the SDK directory, not Platform-Tools. Android Studio can also create local `local.properties`. |
| Missing platform 37.1 | Install the exact minor API in SDK Manager. Installing 36 or 37.0 alone is insufficient. |
| Java/Gradle startup failure | Run `java -version`, set JDK 21 and retry. Gradle's daemon toolchain criteria also request 21. |
| Gradle/JitPack download failure | The first build needs access to Google Maven, Maven Central, Gradle and JitPack. Retry after restoring connectivity; bundled model files do not remove this dependency. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | The installed app uses another signing key. Preserve its data and use another target or that original key. The scripts do not uninstall it automatically. |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | Use an arm64-v8a device or x86_64 emulator. |
| Camera denied / black preview | Grant camera access in Android settings; check that the target exposes a rear camera. A physical phone is preferable for live capture. |
| No speech / long first capture | Raise media volume, wait for first voice initialization, check the visible error/status and collect `adb logcat`. No online TTS download is expected. |
| Asset checksum mismatch | Check that the clone contains the binary files and restore from the intended commit. Do not replace the manifest just to hide a changed model. |

## Rebuilding versus reproducing bytes

This workflow reproduces the source, assets and behavior checks. It does not
promise byte-identical signed APKs across machines: debug keys, tool caches and
packaging timestamps may differ. Preserve the commit, `build-manifest.json` and
`SHA256SUMS` with a distributed build. The default package needs no signing
secret. An optimized `assembleRelease` uses the project's existing signing
configuration; without `keystore.properties` it falls back to the debug key.
Do not label that fallback a production-signed release.

Official reference: [Android command-line builds and APK installation](https://developer.android.com/build/building-cmdline).
