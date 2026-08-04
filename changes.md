# Suggested Improvements

## High priority

- [x] Make the camera optional in the manifest. The viewer should install on devices without cameras because camera publishing is an optional feature. Change `android:required="true"` to `android:required="false"` in `app/src/main/AndroidManifest.xml`.
- [x] Move native player lifecycle calls off the main thread. `startReceiver()`, `stopReceiver()`, and `shutdown()` can block while creating or joining native threads. Serialize these operations on a dedicated I/O or native lifecycle executor.
- [x] Fix compressed-packet overflow validation in `app/src/main/cpp/media_codec_decoder.cpp`. Check `extra_size > available_size` before comparing `payload_size` against the remaining bytes, instead of evaluating `payload_size + extra_size` directly.
- [x] Always free captured NDI video frames in `app/src/main/cpp/ndi_bridge.cpp`, including frames with invalid dimensions or payloads.
- [x] Make `Yuv420ToNv12Converter` account for `Image.cropRect`, row stride, and chroma pixel stride. Add tests for cropped images and different device plane layouts.

## Performance and power

- Cache `ANativeWindow_setBuffersGeometry()` results instead of calling it for every Full NDI frame.
- Move Full NDI color conversion from the CPU to OpenGL ES or Vulkan when profiling shows the receive thread is a bottleneck.
- Hold the Wi-Fi multicast lock only while source discovery is active. Do not hold it during playback, camera preview, settings, or other screens.

### Multicast-lock behavior

NDI discovery uses multicast traffic, typically mDNS/UDP multicast, to find sources on the local network. Android may filter or suspend multicast delivery while Wi-Fi power saving is active. `WifiManager.MulticastLock` temporarily asks Android to deliver that traffic to the app.

The current implementation acquires the lock whenever local-network permission is granted and releases it only when the composable leaves composition. Because `NdiApp` remains composed for the whole application, the lock can remain held during playback and camera publishing. That can increase battery usage without helping an already-connected receiver or sender.

The lock should instead follow discovery state:

1. Acquire it immediately before a discovery attempt begins.
2. Keep it across all five bounded discovery attempts, then release it once source names have been published. Detail probes normally connect directly to the discovered source and do not need the multicast lock.
3. Release it in a `finally` block when discovery completes, fails, or is cancelled.
4. Release it when entering playback, camera publishing, settings, or when the app goes into the background.

The important part is to preserve the lock across all five one-second discovery attempts. Releasing it between attempts could make discovery unreliable. A reference-counted lock or a small discovery-session owner can prevent overlapping refreshes from releasing a lock still needed by another refresh.

Also re-check local-network permission when the activity resumes, so a permission changed in Android Settings is reflected before acquiring the lock.

## Testing and product quality

- Add ViewModel tests for refresh cancellation, five-attempt timeout, empty-state recovery, and source-detail updates.
- Add device tests for permissions, rotation, surface recreation, reconnection, decoder errors, and immersive playback.
- Add camera tests for supported-size selection, frame-rate fallback, and NV12 conversion.
- Add audio playback, picture-in-picture, playback statistics, per-source preferences, and richer accessibility support as later features.
