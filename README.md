This is a Kotlin Multiplatform project targeting Android, iOS.

Serenity is a free, account-free relaxation sound app. Its feature slices are `home`, `category`,
`favorites`, and `player`; shared catalog and platform primitives live under `core`.

## Included features

- Five all-ages sound categories with 15 tracks: rain, ocean, forest, white noise, and lullabies.
- Hybrid audio delivery: five bundled offline tracks plus ten HTTPS tracks that stream immediately
  and are saved to app-owned storage for later local playback.
- Persistent favorites backed by Kotlin Multiplatform DataStore.
- Browsing, track details, favorites, and active audio playback.
- A `core:media` KMP playback layer built with Media3 ExoPlayer on Android and
  AVFoundation AVPlayer on iOS, including platform media-session integration.
- Public-domain/CC0 audio with a source and checksum manifest in
  [THIRD_PARTY_AUDIO.md](./THIRD_PARTY_AUDIO.md).

The core capability boundary is intentionally incremental: `core:audio-content` owns catalog
metadata, bundled MP3s, remote sources, and local resolution; `core:favorites` owns favorite
persistence; `core:playback` owns app session policy; and `core:media` remains the reusable
platform playback engine.

SQLDelight is intentionally not included: favorites are a small key-value set without relational
queries, partial-row updates, or referential-integrity needs, which makes DataStore the smaller and
more appropriate persistence layer.

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

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
