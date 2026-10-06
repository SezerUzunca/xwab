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

The graph also builds every part of the engine and the platform resources those parts use.

- **Shared clock and schedulers.** The sleep-timer clock is one binding that both the facade and
  the countdown read. The tick schedulers get their main-looper `Handler` (Android) or main
  dispatcher (iOS) from the graph.
- **Android connection.** The timer IPC client, the controller's session token and its executor
  come from the graph.
- **iOS player.** The user agent, a provider of `AVQueuePlayer` (a media-services reset needs a
  new player) and the system's shared audio session, notification, remote-command and
  now-playing centers come from the graph.
- **Parts that call back into the facade.** The store, the countdown, the Android controller
  connection, and the iOS player and media session are `@AssistedInject` classes. The facade
  passes its callbacks to the factory, and Metro supplies the rest.
- **Creation order.** Factories and providers create those parts, and with them every native
  resource, only after the facade's main-thread check.
- **Unscoped schedulers.** This is deliberate. Scheduling replaces the pending tick, so each owner
  needs its own scheduler.

Android builds `PlaybackService`, so the service builds `PlaybackServiceGraph` in `onCreate`. That
graph provides the player, its source chain with the application's user agent, the media session
and the sleep timer. The service keeps these and releases them in `onDestroy`. The timer counts
with the same clock and scheduler bindings as the engine's countdown. `AndroidPlaybackBindings`, a
binding container that both Android graphs include, holds those bindings.

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
