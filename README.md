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

Each core module owns one capability, contract and implementation together: `core:audio-content`
owns the sound catalog — the manifest, the remote sources, local caching, the repository screens
read and the resolver playback resolves through; `core:preferences` owns what
the app persists, favorites today; `core:playback` owns app session policy; and
`core:playback-engine` remains the reusable platform playback engine. There is no separate
repository layer: a port lives in the module that owns the data behind it.

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
   core:model · core:designsystem · core:navigation
   core:audio-content (catalog, download, cache) · core:preferences (favorites)
   core:playback (session) ─► core:audio-content, core:playback-engine
```

A feature owns its screen, its state, its ViewModel, its use cases and its Koin bindings. It sees
another feature only through that feature's `:navigation` module — a route, its serializers and a
`Navigator` extension, no implementation. Screen-specific orchestration lives with its screen. A
screen action that already has the model it needs injects the capability directly — a use case has
to earn its name by holding a decision.

Four rules hold this in place, and `./gradlew checkArchitecture` fails the build when one breaks:
a core module may not depend on a feature; a feature may depend only on another feature's
`:navigation` module; a use case in a shared core module must serve more than one feature; and a
feature may not reference `AudioContentResolver` or `AudioFileStore` — `core:audio-content` is on
its classpath for the catalog, not for delivery.

### Build configuration

Module build files carry no boilerplate — the convention plugins in
[build-logic](./build-logic/src/main/kotlin) own the KMP targets, the SDK levels, the iOS guard
and the shared dependency sets:

| Plugin | For |
|---|---|
| `xwab.kmp.library` | every KMP library module |
| `xwab.kmp.compose` | the above plus Compose Multiplatform |
| `xwab.kmp.feature` | the above plus the core modules and Compose/Koin surface every screen uses |
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
