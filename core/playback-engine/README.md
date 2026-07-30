# Audio player

Standalone Kotlin Multiplatform audio playback library.

## Scope

- Common, UI-independent playback contract and observable state.
- Android playback with Jetpack Media3 ExoPlayer.
- iOS playback with AVFoundation `AVQueuePlayer`, `AVPlayerLooper`, and `AVAudioSession`.
- One active audio source and optional single-track looping.

The module intentionally does not own application DI, playlists, persistence,
downloads, analytics, or UI. It does own the platform playback session,
background controls, and playback metadata required by its player.

## Platform creation

DI creates the platform `PlaybackController` (`createAndroidPlaybackController(context)` /
`createIosPlaybackController()`).

The application-specific adapter in `core:playback` depends on `PlaybackController`: it observes
`state` / `sleepTimerState` and drives playback through the single
`submit(PlaybackCommand...)` entry point. Feature and domain code use `PlaybackCoordinator`
instead of depending on this engine directly. Use one app-scoped controller instance and call it
only from the main thread; DI owns the engine's `release()`. Every `AudioSource` needs a stable
non-blank ID and a non-blank playable URI.

## Host-level background integration

For Android background playback, the module encapsulates `PlaybackService`, its
foreground-service permissions, manifest declaration, Media3 notification, and
metadata management. Android manifest merging adds these declarations to the
consuming application automatically.

The iOS application target must still enable the Audio background mode. Lock
Screen, Control Center, and remote commands are managed by `AppleMediaSession`.
