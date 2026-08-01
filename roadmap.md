# Network Stream Viewer Roadmap

This roadmap prioritizes improvements that make source discovery and playback more reliable before expanding the app with advanced viewing and publishing features.

## Phase 1: Faster Discovery and Reliable Playback

- [x] Show discovered source names immediately and load resolution, frame rate, and codec details asynchronously.
- [x] Replace indefinite black screens with clear states: connecting, waiting for keyframe, disconnected, unsupported codec, decoder failure, and insufficient network bandwidth.
- [x] Allow Highest, Preview/Low, and Automatic bandwidth selection.

## Phase 3: Monitoring and Audio

- [ ] Add synchronized NDI audio playback through Android `AudioTrack`.
- [ ] Provide mute and volume controls in the floating playback interface.
- [ ] Add an optional stream-information overlay showing resolution, frame rate, Full NDI/HX codec, bitrate, dropped frames, latency, and connection status.
- [ ] Record useful playback diagnostics for troubleshooting without exposing private network information.

## Phase 2: NDI PTZ Camera Control

- [ ] Detect PTZ capability after the receiver connects and show controls only for supported sources.
- [ ] Support recalling and storing numbered camera presets, with confirmation before overwriting a preset.
- [ ] Add press-and-hold pan and tilt controls with adjustable movement speed and immediate stop on release, navigation, or surface loss.
- [ ] Add zoom-in and zoom-out controls with safe speed limits and immediate stop behavior.
- [ ] Add autofocus and manual focus controls, followed by optional exposure and white-balance controls where supported.
- [ ] Report rejected or unavailable PTZ commands without interrupting video playback.
- [ ] Add native and device tests for capability changes, command ranges, stop commands, presets, and receiver cleanup.

## Phase 4: Android Viewing Experience

- [ ] Add Android picture-in-picture playback.
- [ ] Remember per-source preferences such as bandwidth, mute state, and information-overlay visibility.
- [ ] Add configurable gestures for showing controls, muting, and changing volume or brightness.
- [ ] Improve accessibility with content descriptions, larger touch targets, and screen-reader-friendly connection states.

## Phase 5: Camera Publishing

- [ ] Add microphone audio publishing with audio/video synchronization.
- [ ] Add bitrate, focus, exposure, torch, and orientation controls.
- [ ] Support hardware-encoded NDI HX publishing where the device and SDK license permit it.
- [ ] Display publishing statistics including connected receivers, bitrate, dropped frames, and device temperature warnings.

## Phase 6: Performance and Quality

- [ ] Move Full NDI color conversion from the CPU to OpenGL ES or Vulkan.
- [ ] Profile battery, memory, decoder latency, and thermal behavior at 1080p and 4K.
- [ ] Add device tests for reconnection, rotation, immersive mode, decoder errors, source-detail timeouts, and surface recreation.
- [ ] Test H.264, HEVC, Full NDI, multiple frame rates, and changing stream resolutions across representative Android devices.
