This is a Kotlin Multiplatform project targeting Android, iOS.

Serenity is a free, account-free relaxation sound app. Its feature slices are `home`, `category`
and `player`; shared catalog and platform primitives live under `core`. Favouriting is a
shared data capability used from several screens rather than a screen of its own.

## Included features

- Five all-ages sound categories with 15 tracks: rain, ocean, forest, white noise, and lullabies.
- On-demand audio delivery: no audio ships in the app. A picked track streams over HTTPS
  immediately and is saved to app-owned storage, so later plays of it are local and offline.
- Persistent favorites backed by Kotlin Multiplatform DataStore.
- Browsing, track details, favorites, and active audio playback.
- A `core:playback-engine` KMP playback layer built with Media3 ExoPlayer on Android and
  AVFoundation AVPlayer on iOS, including platform media-session integration.
- Public-domain/CC0 audio with a source and checksum manifest in
  [THIRD_PARTY_AUDIO.md](./THIRD_PARTY_AUDIO.md).

Each core module owns one capability, contract and implementation together, and is named for the
question it answers rather than for the mechanism behind it:

| Module | Answers |
|---|---|
| `core:catalog` | *what can be played?* — `Music`, `Category`, `MusicCatalogRepository` |
| `core:catalog-manifest` | *what ships, and where does each track's audio live?* — the manifest and its two ports |
| `core:audio-delivery` | *where do this track's bytes come from now?* — resolution, download, cache |
| `core:favorites` | *what did the listener mark?* — persisted through DataStore |
| `core:playback-session` | *how is the one session steered?* — `PlaybackCoordinator`, `PlaybackSummary` |
| `core:playback-engine` | *how does this platform play audio?* — the reusable Media3/AVFoundation engine |

There is no separate repository layer and no shared model module: a port lives in the module that
owns the data behind it, and so does the type it publishes.

SQLDelight is intentionally not included: favorites are a small key-value set without relational
queries, partial-row updates, or referential-integrity needs, which makes DataStore the smaller and
more appropriate persistence layer.

## Architecture

```
androidApp ─┐
            ├─► shared (composition root: Koin + NavDisplay)
iosApp ─────┘        │
        ┌────────────┼────────────────┐
        ▼            ▼                ▼
  feature:home  feature:category  feature:player   (each with a :navigation API module)
        │            │                │
        └────────────┴───────┬────────┘
                             ▼
   core:catalog      core:favorites      core:playback-session
        ▲                                        │
        │                        ┌───────────────┴───────────────┐
        │                        ▼                               ▼
        │                core:audio-delivery            core:playback-engine
        │                        │
        │                        ▼
        └──────────────  core:catalog-manifest

  crosscutting: core:designsystem · core:navigation · core:testing
```

A feature owns its screen, its state, its ViewModel, its use cases and its Koin bindings. It sees
another feature only through that feature's `:navigation` module — a route, its serializers and a
`Navigator` extension, no implementation. Screen-specific orchestration lives with its screen. A
screen action that already has the model it needs injects the capability directly — a use case has
to earn its name by holding a decision.

A feature also declares the capabilities it reads, in its own build file. `xwab.kmp.feature` hands
out the design system, navigation and the Compose/Koin surface and nothing else. Delivery, the
engine and the shipped manifest are declared by the modules that assemble a session and by the
composition root, and nowhere else — so no screen can name a delivery type, the engine's own state
model, or the URL behind a track.

Four rules hold this in place, and `./gradlew checkArchitecture` fails the build when one breaks:
a core module may not depend on a feature; a feature may depend only on another feature's
`:navigation` module; a use case in a shared core module must serve more than one feature; and a
feature may not declare `core:audio-delivery`, `core:playback-engine` or `core:catalog-manifest`.

That fourth rule used to scan feature sources for the strings `AudioContentResolver` and
`AudioFileStore`, because the catalog and delivery shared one module that every feature needed for
the catalog's sake — the graph could not tell a legitimate dependency from a reach through it.
Splitting capabilities apart until each boundary was a real edge is what turned it into a
dependency check, which survives a rename in a way a quoted class name did not. The rule also fails
loudly if a module it names stops existing, so it cannot quietly end up protecting nothing.

The rules live in `FeatureFirstRules` as plain functions over the dependency graph, and
`FeatureFirstRulesTest` drives each of them from both sides — a graph that must pass and one that
must fail. A rule only ever run against a repository that satisfies it has never been shown to
reject anything, which is the failure mode the old version would have had. `./gradlew check` runs
those tests; `./gradlew checkArchitecture` runs the rules against this repository.

## Playback

One session, steered through `PlaybackCoordinator`. It publishes a `PlaybackSummary` carrying both
`playIntent` — whether playback is *wanted* — and `isPlaying` — whether sound is audible. A
play/pause control renders the first, and a tap branches on the first; they differ while a source is
being resolved or buffered, and mixing them up is how a tap during buffering used to pause a sound
the screen was showing as stopped.

`play(trackId)` claims the session before it resolves anything, so a second tap on the same sound
finds something to pause. `pause()` abandons a lookup still in flight. A lookup that came back
`NotFound` or `Unavailable` reaches the screen as a `PlaybackFailure` instead of a tap that did
nothing at all.

It takes a `TrackId`, not a `String` and not a `Music`: the metadata the media session publishes is
read from the catalog beside the source it is paired with, so a screen cannot hand over a stale
title, and it cannot hand over a category id either.

### Build configuration

Module build files carry no boilerplate — the convention plugins in
[build-logic](./build-logic/src/main/kotlin) own the KMP targets, the SDK levels, the iOS guard
and the shared dependency sets:

| Plugin | For |
|---|---|
| `xwab.kmp.library` | every KMP library module |
| `xwab.kmp.compose` | the above plus Compose Multiplatform |
| `xwab.kmp.feature` | the above plus the design system, navigation and the Compose/Koin surface every screen uses — capability modules are declared per feature |
| `xwab.kmp.feature.navigation` | a feature's navigation API module |

`shared` is the exception and configures itself: it is the only module producing an iOS framework.

They are `Plugin<Project>` classes rather than precompiled `.gradle.kts` script plugins on
purpose: compiling those needs Gradle's `kotlin-dsl` plugin, which is published only to
plugins.gradle.org. This build resolves everything from Google's Maven and Maven Central, and
`build-logic/settings.gradle.kts` declares both repository blocks explicitly to keep it that way.

### Adding a feature

```powershell
./tools/new-feature.ps1 sleep-timer
```

That writes both modules, their build files and a working route/screen/ViewModel/Koin skeleton.
Gradle finds them on its own — `settings.gradle.kts` scans `feature/`. Two lines are left, both
in `shared`: the module dependency in `shared/build.gradle.kts`, and the feature's `FeatureEntry`
in [AppFeatures.kt](./shared/src/commonMain/kotlin/com/xwab/app/di/AppFeatures.kt), which is what
carries its Koin bindings and route serializers into the app.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`
- Architecture rules: `./gradlew checkArchitecture`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
