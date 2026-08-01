# Network Stream Viewer

A vibe coded android 17 Jetpack Compose application that discovers NDI sources on the local network, renders a selected video stream, and publishes the device camera through the native NDI Android SDK.

The camera sender publishes up to 1080p video with selectable lens, resolution, and frame rate. Choose its advertised stream name before starting; stop it to change settings or rename and restart. Camera audio is not included yet.

## Architecture

The app uses a small layered architecture:

- `domain/` contains the `NdiSource` model and repository contract.
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

Convenience targets are also available:

```bash
make devices                  # List connected devices and their serials
make install                  # Auto-detect one device, then build and install
make install ADB_SERIAL=ID    # Select a device when several are connected
make release                  # Build the release APK
```

The current release build is unsigned until a release signing configuration is added.

## Device testing

Use a physical Android device on the same LAN as an active NDI sender or receiver. Allow local-network and camera permissions when prompted. Wi-Fi client isolation, blocked mDNS traffic, or separate VLANs can prevent discovery and camera publishing.

The source screen searches for up to five seconds, then presents an empty state with pull-down and button refresh actions. The player enters immersive landscape mode, overlays a floating Back button, and fit-scales the NDI display aspect ratio without stretching. This milestone renders video without audio. NDI discovery uses a Wi-Fi multicast lock only while the application is active.

NDI® is a registered trademark of Vizrt NDI AB.
