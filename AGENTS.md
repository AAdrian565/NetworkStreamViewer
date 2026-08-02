# Repository Guidelines

## Project Structure & Architecture

This Android app uses Kotlin, Compose, C++, and JNI. Preserve these layers:

- `domain/` contains models and repository contracts.
- `data/ndi/` owns `NdiNative`, SDK adapters, discovery, player control, and JNI sender calls.
- `data/camera/` owns Camera2 capture, YUV conversion, and sender lifecycle.
- `presentation/` contains `NdiViewModel`, UI state, permissions, orientation, source list, player, and camera sender UI.
- `MainActivity.kt` is only the Compose entry point.
- `app/src/main/cpp/` contains JNI rendering, NDI HX MediaCodec decoding, and CMake configuration.

Place JVM tests in `app/src/test/` and device tests in `app/src/androidTest/`. Never edit `build/`, `app/build/`, or `.cxx/` output.

## Build, Test, and Development Commands

- `./gradlew assembleDebug` — build Kotlin/native code and produce the debug APK.
- `./gradlew testDebugUnitTest` — run local JUnit tests.
- `./gradlew connectedDebugAndroidTest` — run AndroidX tests on a connected device.
- `./gradlew lintDebug` — run lint; reports appear under `app/build/reports/`.
- `./gradlew installDebug` — install through ADB.
- `make install` — build, install, and open the debug APK through ADB.
- `make install-release` — build, install, and open the signed release APK.
- `make install BUILD_TYPE=release` — equivalent to `make install-release`.
- `make release` — remove stale release APK names, build the signed release, and print its versioned path.

Test on a physical device on the same LAN as an active NDI sender.

Release versions are configured in the tracked root `gradle.properties` using `VERSION_NAME` (SemVer) and an increasing integer `VERSION_CODE`. The release artifact is named `NetworkStreamViewer-v<version>.apk`, for example `NetworkStreamViewer-v1.0.0.apk`, under `app/build/outputs/apk/release/`. Update both version properties before a release.

## Coding Style & Behavioral Invariants

Use four-space indentation, `PascalCase` for types/composables, `camelCase` for functions/properties, and `UPPER_SNAKE_CASE` for constants. Prefer one primary type per file.

Expose `StateFlow` from `NdiViewModel`. Keep blocking SDK calls on `Dispatchers.IO`; presentation must not call native functions directly. Discovery is bounded to five one-second attempts. It must stop its indicator, show an empty state, and restart from pull-down or button refresh. Handle cancellation separately from failures.

Keep selection in `NdiViewModel` so rotation preserves playback. The list is portrait; playback is sensor-landscape and immersive. Restore system bars on exit. Fit-scale the NDI display aspect ratio; never stretch or crop silently.

Receive regular NDI through the Advanced SDK's decompressor and pass NDI HX H.264/H.265 packets to Android MediaCodec. Keep decoder work on the receive thread, wait for a keyframe before configuration, validate compressed packet sizes, and release codec resources before the playback surface.

Publish discovered source names before starting bounded detail probes. Playback must expose connecting, keyframe wait, disconnect, codec, decoder, and bandwidth states instead of leaving a silent black surface. Preserve Automatic, Highest, and Preview/Low receiver modes; Automatic starts high and falls back to preview when video stalls.

Camera publishing is video-only NV12 at up to 1080p with selectable lens and frame rate. Require camera and local-network permissions, keep the camera open only while its screen exists, and stop the NDI sender before changing settings, renaming, or leaving.

Compile C++ with `-Wall -Wextra -Werror`. When changing `NdiNative`, update matching JNI symbols. Pair every NDI, thread, frame, listener global reference, and `ANativeWindow` acquisition with deterministic cleanup.

## Testing Guidelines

Name local tests `*Test.kt` and device tests `*InstrumentedTest.kt`. Unit-test mapping and state. Device-test refresh timeout, permissions, rotation, immersive playback, aspect ratio, and surface destruction. Fixes should include focused regression tests.

## Commits & Pull Requests

No history is available to infer a convention. Use short imperative subjects, optionally Conventional Commits, such as `fix: bound source discovery`. Pull requests should describe behavior, list verification commands, link issues, and include screenshots for UI changes.

## Security & Configuration

Keep `local.properties` untracked and set `ndi.sdk.dir` to the licensed SDK. Never commit NDI binaries, proprietary headers, licenses, signing keys, or machine paths. Preserve NDI attribution and API-37 local-network permission behavior.

Release signing properties belong in the ignored `gradle/gradle.properties` or user-level `~/.gradle/gradle.properties`. When present, the same private keystore signs both debug and release builds. Keep the keystore and all four signing properties private. GitHub Actions CI/CD is not configured; release APKs are built locally and manually uploaded to GitHub Releases.
