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

Every type crossing a core-module boundary lives in a `.port` package and is public — Kotlin's own
default when no modifier is written, or `public` written out; either is fine, only a keyword saying
otherwise is not. Hand-written production declarations outside `.port` packages are `internal` or
`private`. Core modules may import another core module only through that module's `.port` package.

There is no shared repository abstraction. A feature consumes the narrow capability it needs:

| Capability module | Public port |
|---|---|
| `:core:sound` | `SoundPort`, sound models and `TrackSource` |
| `:core:delivery` | `DeliveryPort`, `DeliveryRequest`, `CacheKey` and `DeliveryResult` |
| `:core:favorites` | `FavoritesPort` |
| `:core:story` | `StoryPort`, story models and `StoryStreamSource` |
| `:core:session` | `PlaybackPort` and session model types |
| `:core:playback` | `PlaybackEnginePort` and engine command/state types |
| `:core:network` | `NetworkPort` and transport-neutral response/error types |

Implementations such as manifest, DataStore, Ktor, cache and platform playback adapters stay
internal. Metro discovers them through `@ContributesBinding(AppScope::class)`; `@Inject` constructs
them and `@SingleIn(AppScope::class)` owns their lifetime. Android and iOS graphs are generated at
compile time, so missing or ambiguous bindings fail compilation.

Metro's generated public contribution providers return ports, keeping the concrete adapter types
hidden. Feature `*Dependencies` classes are public DI contracts containing ports with internal
properties; `shared.di` exposes these bags so the composition root can pass them to feature entries.

```text
core/
├── network
├── sound
├── story
├── delivery
├── favorites
├── session
└── playback

designsystem/
testing/
feature/
├── browse
├── category
├── favorites
├── sounds
└── story
```

Each directory directly under `core` is one Gradle module. Sound and story each own their models,
one port, manifest data and an internal implementation in a separate file. `SoundPort` and
`StoryPort` combine metadata queries with source lookup. Contracts and models live in each
module's `port` package; `SoundPortImpl.kt` and `StoryPortImpl.kt` live in the module package.
Delivery and favorites are separate modules. `checkArchitecture` enforces one port per content module.
Favorites uses caller-owned namespaces and string IDs; delivery accepts source URLs and namespaced
cache requests. Neither depends on sound or story. Only delivery depends on the network module.

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

Sound playback resolves its source through `SoundPort`, then gives a request to `DeliveryPort`.
The session owns the sound namespace, MPEG policy and current cache inventory. Cached files are preferred; otherwise
the HTTPS source is returned immediately and a single background download fills app-owned cache.
Stories use `StoryPort` and stream without being cached.

Android playback uses Media3; iOS playback uses AVFoundation. Platform implementations are
internal Metro contributions behind `PlaybackEnginePort`.

## Architecture enforcement

`checkArchitecture` fails when any of these rules is broken:

1. A core module depends on a feature, or one feature depends on another feature.
2. A feature is not exactly one `:feature:<name>` module, or an `api`/`impl` directory appears under `core` or `feature`.
3. A feature reaches an adapter-only core module, directly or through an exported dependency.
4. A feature-specific use case leaks into `core`.
5. Any shared production source set references a feature outside the allowed boundaries: navigation/composition may use feature navigation contracts, and DI may use feature dependency bags.
6. A production core declaration outside an exact capability `.port` package is public.
7. A port declaration or member is written as non-public, or a public contract interface does not end in `Port`.
8. A cross-core import, wildcard import, or fully qualified reference bypasses an exact `.port` package.
9. A `Repository` or DI-style `Provider` abstraction appears in `core`.
10. A Koin import or dependency is reintroduced anywhere in the project.
11. A feature exposes a declaration outside its navigation package or a DI `*Dependencies` class.
12. Sound or story exposes more than one port interface or lacks its `SoundPort` / `StoryPort` contract.
13. Favorites depends on another project, or delivery depends on a project other than itself or `:core:network`.

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
./gradlew :check
```

`:check` runs the architecture check and the build-logic regression tests. Android CI runs it
alongside the Android host tests before assembling the APK.

Open `iosApp` in Xcode to run the iOS application.

Audio provenance and licenses are recorded in
[THIRD_PARTY_AUDIO.md](./THIRD_PARTY_AUDIO.md).
