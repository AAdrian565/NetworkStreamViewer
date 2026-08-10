# Repository Guidelines

## Project Structure & Module Organization

Network Stream Viewer is a single-module Android app (`:app`) built with Kotlin, Jetpack Compose, Camera2, C++17, JNI, MediaCodec, and the licensed NDI Advanced SDK.

- `app/src/main/java/com/adriant/networkstreamviewer/domain/`: models and repository contracts; keep this layer independent of Android/native implementations.
- `app/src/main/java/com/adriant/networkstreamviewer/data/ndi/`: NDI SDK boundary, JNI declarations/mappings, discovery repository, receiver/player, audio, and PTZ control.
- `app/src/main/java/com/adriant/networkstreamviewer/data/camera/`: Camera2 capture, YUV-to-NV12 conversion, and sender lifecycle.
- `app/src/main/java/com/adriant/networkstreamviewer/data/settings/` and `data/update/`: persisted settings and GitHub update integration.
- `app/src/main/java/com/adriant/networkstreamviewer/presentation/`: `NdiViewModel`, UI state, permissions/orientation, and Compose screens under `sources/`, `player/`, `camera/`, and `settings/`.
- `MainActivity.kt`: Compose entry point only. `ui/theme/` contains theming.
- `app/src/main/cpp/`: JNI bridge, Full NDI rendering, NDI HX H.264/H.265 MediaCodec decoding, audio capture, and CMake setup.
- `app/src/main/res/`: manifest resources and launcher artwork. `Icon.png` is the source for `make icon`.
- `app/src/test/`: local JUnit tests. `app/src/androidTest/`: AndroidX/Compose device tests.

Do not edit generated output in `build/`, `app/build/`, `.cxx/`, or `.externalNativeBuild/`.

## Setup, Build, Test, and Development Commands

Install Android SDK API 37, the NDK, and CMake. Install the licensed NDI Android SDK separately. In ignored `local.properties`, configure `sdk.dir` and `ndi.sdk.dir`; the NDI directory must provide `include/Processing.NDI.Lib.h` and `lib/<ABI>/libndi_advanced.so`. Gradle configuration fails without `ndi.sdk.dir`.

- `./gradlew assembleDebug` or `make debug`: compile Kotlin and native code and create `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew testDebugUnitTest`: run local JUnit tests.
- `./gradlew connectedDebugAndroidTest`: run AndroidX/Compose tests on a connected device.
- `make lint`: run `ktlintCheck` and strict `lintDebug`; lint reports are under `app/build/reports/`.
- `make format`: apply ktlint formatting to Kotlin sources and Kotlin Gradle scripts.
- `./gradlew installDebug`: install the debug APK with ADB.
- `make devices`: list connected ADB devices.
- `make install [ADB_SERIAL=<id>]`: build, install, and launch a debug APK; provide a serial when multiple devices are connected.
- `make install-release` or `make install BUILD_TYPE=release`: build, install, and launch the signed release.
- `make release`: remove stale release names and build `app/build/outputs/apk/release/NetworkStreamViewer-v<VERSION_NAME>.apk`.
- `make icon`: regenerate adaptive launcher artwork from `Icon.png` with ImageMagick.
- `make clean`: remove Gradle build output.

Device behavior requires a physical Android device on the same LAN as an active NDI sender or receiver; multicast filtering, client isolation, and separate VLANs can break discovery.

## Architecture & Integration Constraints

Dependencies point inward: presentation uses domain contracts, while data implements them. Presentation code must not call `NdiNative` directly. Expose immutable `StateFlow` from `NdiViewModel`, keep selection there for rotation, and run blocking SDK/native lifecycle operations on `Dispatchers.IO`. Treat `CancellationException` separately from failures.

Discovery is bounded to five one-second attempts. Publish source names before bounded concurrent detail probes; always stop refresh state, show an empty/error state, and allow pull-down/button refresh. Playback must expose connecting, keyframe-wait, disconnect, codec, decoder, and bandwidth states. Preserve Automatic, Highest, and Preview/Low modes; Automatic starts high and falls back when video stalls.

Playback is sensor-landscape and immersive; source/settings screens are portrait. Restore system bars on exit and fit-scale the stream aspect ratio without stretching or silent cropping. Keep receiver/audio/PTZ lifecycle serialized and stop PTZ movement on release, navigation, or surface loss.

`NdiNative.kt` and JNI symbols in `app/src/main/cpp/ndi_bridge.cpp` are a paired API: update both sides together. Full NDI uses the Advanced SDK decompressor; HX packets go to MediaCodec. Decoder work stays on the receive thread, waits for a keyframe before configuration, validates packet sizes, and releases codec resources before the playback surface. Pair every NDI object/frame, native thread, listener global reference, and `ANativeWindow` acquisition with deterministic cleanup. C++ builds with `-Wall -Wextra -Werror`.

Camera publishing is video-only NV12, up to 1080p, with selectable lens/resolution/frame rate. Require camera and local-network permissions, keep the camera open only while its screen is active, and stop the sender before changing settings, renaming, or leaving.

## Coding Style & Naming Conventions

Follow `.editorconfig`: UTF-8, LF endings, final newline, trimmed trailing whitespace, four-space indentation, and a 120-column Kotlin limit. ktlint uses its official style and permits PascalCase names for `@Composable` functions. Use `PascalCase` for types/composables, `camelCase` for functions/properties, and `UPPER_SNAKE_CASE` for constants. Prefer one primary type per file. Run `make format`, then `make lint`, after Kotlin or Gradle-script changes. Android Lint treats warnings as errors.

## Testing Guidelines

Name local tests `*Test.kt` and device tests `*InstrumentedTest.kt`. Add focused regression tests for fixes. Unit-test models, mappings, conversion, and state transitions without Android dependencies. Use device tests for JNI/native integration, permissions, refresh timeout/recovery, rotation, immersive playback, aspect ratio, PTZ/audio controls, and surface destruction. Native playback and discovery changes still require physical-device testing with real NDI traffic.

## Versions, Commits, and Pull Requests

Release versions live in tracked root `gradle.properties`: update SemVer `VERSION_NAME` and increasing integer `VERSION_CODE` together. Recent history mixes terse descriptive subjects and Conventional Commits; use a short imperative subject, optionally with a prefix such as `fix:` or `feat:`. Pull requests should describe behavior, list verification commands, link relevant issues, and include screenshots for UI changes.

## Security & Configuration

Keep `local.properties`, licensed NDI binaries/headers, SDK licenses, machine paths, keystores, and signing credentials out of Git. Release signing properties belong in ignored `gradle/gradle.properties` or user-level `~/.gradle/gradle.properties`; when configured, the same private key signs debug and release builds. Preserve NDI attribution and Android API 37 local-network permission behavior. Releases are built locally; no repository CI/CD workflow is configured.
