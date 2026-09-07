# Xwab

Xwab is a Kotlin Multiplatform sleep-sound app for Android and iOS. It is account-free, streams
audio on first play, caches sounds for later playback, and streams public-domain sleep stories.

## Architecture

The project uses Navigation 3, Metro compile-time dependency injection, and a strict ports-only
core boundary.

```text
androidApp ─┐
            ├── shared (app shell, Navigation 3 policy, Metro graph)
iosApp ─────┘        │
                     ├── feature:browse
                     ├── feature:favorites
                     ├── feature:category
                     ├── feature:sounds
                     └── feature:story
                              │
                              └── public core ports
                                      │
                                      └── internal Metro adapters
```

Every feature is one Gradle module. Routes, entry providers, UI, state, ViewModels, use cases and
the feature's Metro dependency bag live together; there is no feature API/implementation module
split. Features never depend on one another. The app shell connects outgoing feature intents to
destination routes.

`designsystem` and `testing` are top-level support modules. They are deliberately outside `core`:
core reserves its public surface for ports, while UI components and reusable test fakes are not
application capability ports.

## Core boundary

Every type crossing a core-module boundary lives in a `.port` package and is explicitly `public`.
Production declarations outside `.port` packages are `internal` or `private`. Core modules may
import another core module only through that module's `.port` package.

There is no shared repository abstraction. A feature consumes the narrow capability it needs:

| Capability module | Public port |
|---|---|
| `:core:sound:catalog` | `SoundCatalogPort` and sound model types |
| `:core:sound:manifest` | `SoundSourcePort` and `TrackSource` |
| `:core:sound:delivery` | `SoundContentPort` and its resolution result |
| `:core:sound:favorites` | `FavoritesPort` |
| `:core:story:catalog` | `StoryCatalogPort` and story model types |
| `:core:story:manifest` | `StorySourcePort` and `StoryStreamSource` |
| `:core:playback:session` | `PlaybackPort` and session model types |
| `:core:playback:engine` | `PlaybackEnginePort` and engine command/state types |
| `:core:network` | `NetworkPort` and transport-neutral response/error types |

Implementations such as manifest, DataStore, Ktor, cache and platform playback adapters stay
internal. Metro discovers them through `@ContributesBinding(AppScope::class)`; `@Inject` constructs
them and `@SingleIn(AppScope::class)` owns their lifetime. Android and iOS graphs are generated at
compile time, so missing or ambiguous bindings fail compilation.

```text
core/
├── network
├── sound/
│   ├── catalog
│   ├── manifest
│   ├── delivery
│   └── favorites
├── story/
│   ├── catalog
│   └── manifest
└── playback/
    ├── session
    └── engine

designsystem/
testing/
feature/
├── browse
├── category
├── favorites
├── sounds
└── story
```

The grouping directories under `core` are not Gradle modules; only directories containing a
`build.gradle.kts` are included.

## Navigation 3

`shared` owns the app-level navigation policy and one back stack per top-level destination.
Feature route keys and entry providers live in each feature's `.navigation` package. Only
the app shell's navigation-composition boundary may import those packages; feature code publishes
intent callbacks and does not name destination features.

`BrowseRoute` is the initial destination. Browse and Favorites are top-level destinations;
Category and Player are nested destinations. Story remains a separate content feature while
sharing the content-neutral playback port.

## Playback and delivery

`PlaybackPort` controls the single app-wide session. It accepts a `PlaybackItemId` containing a
kind and value, keeping sound and story identifiers distinct. Its internal adapter resolves
metadata and content through ports, then drives `PlaybackEnginePort`.

Sound playback asks `SoundContentPort` for a playable source. Cached files are preferred; otherwise
the HTTPS source is returned immediately and a single background download fills app-owned cache.
Stories use `StorySourcePort` and stream without being cached.

Android playback uses Media3; iOS playback uses AVFoundation. Platform implementations are
internal Metro contributions behind `PlaybackEnginePort`.

## Architecture enforcement

`checkArchitecture` fails when any of these rules is broken:

1. A core module depends on a feature, or one feature depends on another feature.
2. A feature is not exactly one `:feature:<name>` module, or an `api`/`impl` directory appears under `core` or `feature`.
3. A feature reaches an adapter-only core module, directly or through an exported dependency.
4. A feature-specific use case leaks into `core`.
5. Feature navigation packages are imported outside the app navigation composition boundary.
6. A production core declaration outside an exact capability `.port` package is public.
7. A port declaration or member is not explicitly `public`, or a public contract interface does not end in `Port`.
8. A cross-core import, wildcard import, or fully qualified reference bypasses an exact `.port` package.
9. A `Repository` or DI-style `Provider` abstraction appears in `core`.
10. A Koin import or dependency is reintroduced anywhere in the project.

The Metro convention additionally treats non-public contribution problems as errors and generates
providers that allow internal contributed adapters to remain hidden across modules.

## Adding a feature

```powershell
./tools/new-feature.ps1 sleep-timer
```

The script creates one `:feature:sleep-timer` module. Then wire its dependency bag, entry provider
and serializer into `shared`, and make the route reachable from either a top-level destination or
an existing feature intent.

## Build and checks

```powershell
./gradlew :androidApp:assembleDebug
./gradlew compileAndroidHostTest
./gradlew testAndroidHostTest
./gradlew checkArchitecture
```

Open `iosApp` in Xcode to run the iOS application.

Audio provenance and licenses are recorded in
[THIRD_PARTY_AUDIO.md](./THIRD_PARTY_AUDIO.md).
