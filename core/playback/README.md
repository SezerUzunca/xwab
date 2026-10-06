# Playback engine

Standalone Kotlin Multiplatform audio playback library.

## Scope

- Common, UI-independent playback contract and observable state.
- Android playback with Jetpack Media3 ExoPlayer.
- iOS playback with AVFoundation `AVQueuePlayer`, `AVPlayerLooper`, and `AVAudioSession`.
- One active audio source and optional single-track looping.

The module does not own the application graph, playlists, persistence, downloads, analytics, or
UI. It binds its platform implementation in its own graph, contributes only `PlaybackEnginePort` to
the application graph, and owns the platform playback session, background controls, and playback
metadata required by its player.

## Platform creation

Each platform has its own graph, `AndroidPlaybackGraph` or `IosPlaybackGraph`, built once by that
platform's graph adapter. On Android the adapter takes the application `Context` from the app graph
and passes it on; iOS needs no platform input.

The graph builds the engine's parts as well: the sleep-timer clock both the facade and the countdown
read, the tick schedulers, the Android timer IPC client and the user agent the iOS player presents.
A part that calls back into the facade — the countdown, the Android controller connection, the
iOS player — is an `@AssistedInject` class: the facade passes its callbacks to the factory and
Metro supplies the rest. Factories and providers create those parts only after the facade's
main-thread check. The facade still builds two parts itself, the `PlaybackStore` and on iOS the
`AppleMediaSession`, because they take nothing but its own callbacks. Schedulers are unscoped on
purpose: scheduling replaces the pending tick, so the Android countdown and load timeout each need
their own.

Android builds `PlaybackService`, so the service builds `PlaybackServiceGraph` in `onCreate`: it
provides the player, its source chain with the application's user agent and the media session.
The service keeps the player and the session and releases them in `onDestroy`.

The application-specific adapter in `core:session` depends on `PlaybackEnginePort`: it
observes `state` / `sleepTimerState` and drives playback through the single
`submit(PlaybackCommand...)` entry point. That module and the composition root are the only two
that declare this one — a feature may not, which `checkArchitecture` enforces as a
dependency edge, so screens reach playback through `PlaybackPort` and never see this
engine's state model. Use one app-scoped
controller instance and call it only from the main thread. The engine lives as long as the
application process: nothing in the app calls `release()`, and the playback service keeps running
on its own; `release()` is for a host that tears the engine down itself, such as a test. Every
`AudioSource` needs a stable non-blank ID and a non-blank playable URI.

## Host-level background integration

For Android background playback, the module encapsulates `PlaybackService`, its
foreground-service permissions, manifest declaration, Media3 notification, and
metadata management. Android manifest merging adds these declarations to the
consuming application automatically.

The iOS application target must still enable the Audio background mode. Lock
Screen, Control Center, and remote commands are managed by `AppleMediaSession`.
