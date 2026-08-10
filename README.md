# Network Stream Viewer

An Android 17 Jetpack Compose application that discovers NDI sources on the local network, renders selected video streams, and publishes the device camera through the native NDI Android SDK.

The camera sender publishes up to 1080p video with selectable lens, resolution, and frame rate. Choose its advertised stream name before starting; stop it to change settings or rename and restart. Camera publishing remains video-only. The player supports synchronized NDI audio playback, mute/volume control, and stereo peak/RMS metering.

## Architecture

The app uses a small layered architecture:

- `domain/` contains application models and repository contracts.
- `data/ndi/` owns the licensed SDK boundary, JNI declarations, repository implementation, and player controller.
- `data/camera/` owns Camera2 capture, NV12 conversion, and sender lifecycle.
- `presentation/` contains lifecycle state, permissions, and separate source-list, player, and camera-sender screens.
- `MainActivity.kt` only creates the Compose application.

Dependencies point inward: presentation uses domain contracts, while the data layer implements them. Blocking SDK discovery work is isolated on `Dispatchers.IO`.

## Local setup

Install Android SDK API 37, CMake, and the NDK from Android Studio. Install the licensed NDI SDK separately, then add its machine-local path to the ignored `local.properties` file:

```properties
sdk.dir=/home/you/Android/Sdk
ndi.sdk.dir=/absolute/path/to/NDI SDK for Android
```

The NDI SDK directory must contain `include/Processing.NDI.Lib.h` and ABI-specific libraries such as `lib/arm64-v8a/libndi.so`. Proprietary NDI SDK files are intentionally not copied into this project.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Code quality

The project uses [ktlint](https://pinterest.github.io/ktlint/) for Kotlin formatting and Android Lint for platform-specific analysis. Both checks fail the build on violations.

```bash
make lint                    # Run ktlint and Android Lint
make format                  # Apply safe ktlint formatting fixes
./gradlew testDebugUnitTest  # Run local unit tests
```

Formatting rules live in `.editorconfig`; Android Lint policy is configured in `app/build.gradle.kts`.

Convenience targets are also available:

```bash
make devices                  # List connected devices and their serials
make install                  # Auto-detect one device, then build and install
make install ADB_SERIAL=ID    # Select a device when several are connected
make install-release           # Build, install, and open the signed release app
make install BUILD_TYPE=release # Equivalent to make install-release
make release                  # Build the release APK
make lint                     # Run Kotlin and Android lint checks
make format                   # Format Kotlin sources and build scripts
```

The version is controlled in `gradle.properties` with `VERSION_NAME` (SemVer) and `VERSION_CODE` (an increasing integer). Update both values before building a release.

### Release signing

The release APK is signed with your own keystore for GitHub distribution. Keep the keystore and passwords out of Git. You can use the untracked `gradle/gradle.properties` file or your user-level `~/.gradle/gradle.properties` file:

```properties
NETWORKSTREAMVIEWER_STORE_FILE=/absolute/path/to/networkstreamviewer-release.jks
NETWORKSTREAMVIEWER_STORE_PASSWORD=your-keystore-password
NETWORKSTREAMVIEWER_KEY_ALIAS=networkstreamviewer
NETWORKSTREAMVIEWER_KEY_PASSWORD=your-key-password
```

Build the signed release with:

```bash
make release
```

The signed APK is written to `app/build/outputs/apk/release/NetworkStreamViewer-v1.0.0.apk` (with the current version in the filename). Back up the keystore: future updates must use the same signing key.

When the signing properties are present, both debug and release APKs use this same keystore. This allows a debug APK to be updated directly by a release APK without uninstalling it. Keep the keystore private because debug builds now have the same signing identity as releases.

## Device testing

Use a physical Android device on the same LAN as an active NDI sender or receiver. Allow local-network and camera permissions when prompted. Wi-Fi client isolation, blocked mDNS traffic, or separate VLANs can prevent discovery and camera publishing.

The source screen searches for up to five seconds, then presents an empty state with pull-down and button refresh actions. The player enters immersive landscape mode, overlays a floating Back button, and fit-scales the NDI display aspect ratio without stretching. Supported PTZ sources expose press-and-hold pan, tilt, zoom, and focus controls, presets, and autofocus; movement stops when controls are released or playback ends. The player renders video and synchronized NDI audio, with media-volume routing, mute/volume controls, stereo metering, and non-blocking audio diagnostics. NDI discovery uses a Wi-Fi multicast lock only while the application is active.

NDI® is a registered trademark of Vizrt NDI AB.
