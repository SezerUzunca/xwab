# Playback engine

Standalone Kotlin Multiplatform audio playback library.

## Scope

- Common, UI-independent playback contract and observable state.
- Android playback with Jetpack Media3 ExoPlayer.
- iOS playback with AVFoundation `AVQueuePlayer`, `AVPlayerLooper`, and `AVAudioSession`.
- One active audio source and optional single-track looping.

The module does not own the application graph, playlists, persistence, downloads, analytics, or
UI. It contributes its platform adapter to Metro and owns the platform playback session,
background controls, and playback metadata required by its player.

## Platform creation

Metro selects the internal platform adapter for `PlaybackEnginePort`. Android receives the
application `Context` from the app graph; iOS needs no caller-supplied platform dependency.

The application-specific adapter in `core:playback:session` depends on `PlaybackEnginePort`: it
observes `state` / `sleepTimerState` and drives playback through the single
`submit(PlaybackCommand...)` entry point. That module and the composition root are the only two
that declare this one — a feature may not, which `checkArchitecture` enforces as a
dependency edge, so screens reach playback through `PlaybackPort` and never see this
engine's state model. Use one app-scoped
controller instance and call it only from the main thread; DI owns the engine's `release()`. Every
`AudioSource` needs a stable non-blank ID and a non-blank playable URI.

## Host-level background integration

For Android background playback, the module encapsulates `PlaybackService`, its
foreground-service permissions, manifest declaration, Media3 notification, and
metadata management. Android manifest merging adds these declarations to the
consuming application automatically.

The iOS application target must still enable the Audio background mode. Lock
Screen, Control Center, and remote commands are managed by `AppleMediaSession`.
