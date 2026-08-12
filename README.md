This is a Kotlin Multiplatform project targeting Android, iOS.

Xwab is a free, account-free relaxation sound app. Its feature slices are `browse`, `favorites`,
`category`, `sounds`, and `story`; shared catalog and platform primitives live under `core`.
`sounds` is the player: one track, its transport, and its sleep timer.

**`browse` is the app's start feature.** It owns only category browsing and reads only the sound
catalog. **`favorites` is a top-level feature** that owns the user's saved-sounds list and its inline
playback behavior. The shared `core:sound:favorites` module remains the persistence capability used
by `favorites`, `category`, and `sounds`; it is not UI.

`browse`/`favorites` and `story` remain separate because they share no content model: sound features
deal in `Music` and `Category`; stories deal in `Story`, author, narrator, and a stream that is never
written to disk. They share the content-independent playback session, not a feature.

## Included features

- Five all-ages sound categories with 20 tracks: rain, ocean, forest, white noise, and lullabies.
- On-demand audio delivery: no audio ships in the app. A picked track streams over HTTPS
  immediately and is saved to app-owned storage, so later plays of it are local and offline.
- Persistent favorites backed by Kotlin Multiplatform DataStore.
- Browsing, track details, favorites, and active audio playback.
- Five public-domain sleep stories, streamed rather than cached, played from the story list. No
  position or seek yet: the playback engine does not publish one, so a story is played and paused
  like a sound is.
- A `core:playback:engine` KMP playback layer built with Media3 ExoPlayer on Android and
  AVFoundation AVPlayer on iOS, including platform media-session integration.
- Public-domain/CC0 sound and story recordings with an auditable source and license register in
  [THIRD_PARTY_AUDIO.md](./THIRD_PARTY_AUDIO.md).

Each core module owns one capability, contract and implementation together, and is named for the
question it answers rather than for the mechanism behind it. They are grouped by the content they
serve, so a module's Gradle path says which half of the app it belongs to:

```
core/
├── network
├── sound/       catalog · manifest · delivery · favorites
├── story/       catalog · manifest
├── playback/    session · engine
├── designsystem
├── navigation
└── testing
```

`sound`, `story` and `playback` are directories, not modules: they have no build file, and the
container projects Gradle makes for them are never declared on. `core/sound/manifest` is
`:core:sound:manifest`, and the architecture rules read exactly that path.

| Module | Answers |
|---|---|
| `core:sound:catalog` | *what can be played?* — `Music`, `Category`, `MusicCatalogRepository` |
| `core:sound:manifest` | *what ships, and where does each track's audio live?* — the manifest and its two ports |
| `core:sound:delivery` | *where do this track's bytes come from now?* — resolution, download, cache |
| `core:sound:favorites` | *what did the listener mark?* — persisted through DataStore |
| `core:story:catalog` | *what can be listened to?* — `Story`, `StoryId`, `StoryCatalogRepository` |
| `core:story:manifest` | *which stories exist, and where does each one stream from?* — the list and its two ports |
| `core:playback:session` | *how is the one session steered?* — `PlaybackCoordinator`, `PlaybackSummary` |
| `core:playback:engine` | *how does this platform play audio?* — the reusable Media3/AVFoundation engine |
| `core:network` | *how does shared code reach a server?* — one Ktor client for text and streamed bytes |

Crosscutting modules — `core:network`, `core:designsystem`, `core:navigation`, `core:testing` — are tied to no
content type, so they are grouped under none of them and stay directly under `core/`.

They are not equally visible, though. Screens use `core:designsystem`, feature tests use
`core:testing`, and only the app shell uses `core:navigation`. `core:network` is also hidden from
features, and rule 4 below refuses one that declares it. A screen reads content through a repository
— it does not make requests.

Kotlin packages did not move with the directories: `:core:sound:manifest` still declares
`com.xwab.app.core.catalogmanifest`. Renaming a package touches every import in the build, which is
its own migration and not this one.

There is no separate repository layer and no shared model module: a port lives in the module that
owns the data behind it, and so does the type it publishes.

SQLDelight is intentionally not included: favorites are a small key-value set without relational
queries, partial-row updates, or referential-integrity needs, which makes DataStore the smaller and
more appropriate persistence layer.

## Architecture

```
androidApp ─┐
            ├─► shared (App entry + Koin composition root + XwabApp shell)
iosApp ─────┘        │
                     ├─► feature:browse:{api,impl}    (catalog)
                     ├─► feature:favorites:{api,impl} (catalog + favorites + playback session)
                     ├─► feature:category:{api,impl}  (catalog + favorites + playback session)
                     ├─► feature:sounds:{api,impl}    (catalog + favorites + playback session)
                     └─► feature:story:{api,impl}     (story catalog + playback session)

Every feature follows Now in Android's two-module layout. `:api` publishes only its navigation
contract and serializers; Compose and Koin do not travel onto its consumers' classpaths. `:impl`
owns UI, state, ViewModel, use cases, DI, resources and entry wiring. Feature modules have no
dependencies on other features. `shared` binds intent callbacks to destination APIs and assembles
the implementations and core adapters; it never hosts a feature screen.
```

`feature:story` reads two capabilities where sound features read three: there is no favorites port
for stories.

## Navigation

`shared/.../App.kt` is the thin multiplatform entry used by Android and iOS. It delegates to
`XwabApp` under `shared/.../ui`, the app-level UI equivalent of Now in Android's `NiaApp`.
`XwabApp` owns the scaffold and one back stack per tab; `AppNavigationBar` owns the navigation
chrome. As in NiA, application policy is explicit in
`shared/.../navigation/AppNavigation.kt`: the app chooses its start route and top-level items,
calls every implementation entry provider, and includes every API serializer.

```kotlin
val TOP_LEVEL_DESTINATIONS = listOf(
    TopLevelDestination(
        route = BrowseRoute,
        label = { stringResource(Res.string.tab_browse) },
        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
    ),
    TopLevelDestination(
        route = FavoritesRoute,
        label = { stringResource(Res.string.tab_favorites) },
        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
    ),
)

fun appEntryProvider(navigator: Navigator) = entryProvider {
    browseEntry(onCategoryClick = { navigator.navigate(CategoryRoute(it)) })
    favoritesEntry(onMusicClick = { navigator.navigate(PlayerRoute(it.value)) })
    categoryEntry(
        onMusicClick = { navigator.navigate(PlayerRoute(it.value)) },
        onBack = navigator::goBack,
    )
    playerEntry(onBack = navigator::goBack)
    storiesEntry()
}
```

The first item is deliberately the start route. `sounds` and `category` are absent from that list:
they are navigated *into*, not places to switch to. Their entry providers and serializers are still
registered explicitly. Koin modules are assembled separately in `AppModules.kt`; no DI type leaks
through `core:navigation` or a feature API.

The local equivalent of Now in Android's `feature:foryou:api` and `feature:foryou:impl` split is:

```text
feature:browse:api      api/navigation/BrowseNavigation.kt (public contract)
feature:browse:impl     impl/navigation/BrowseEntry.kt + UI + state + ViewModel + use case + DI
feature:favorites:api   api/navigation/FavoritesNavigation.kt (public contract)
feature:favorites:impl  impl/navigation/FavoritesEntry.kt + UI + state + ViewModel + use case + DI
shared                  App entry, XwabApp shell, top-level UI and application composition root
```

`BrowseEntry`, `FavoritesEntry` and `CategoryEntry` publish only intent callbacks. `shared` decides
that those intents open `category:api` or `sounds:api`; therefore no feature compiles against any
other feature module. Because every start-screen source now lives beneath `:feature:`,
`checkArchitecture` also enforces the same dependency boundaries on the start screen as on every
other feature.

### One back stack per tab

`NavigationState` holds a `Map<NavKey, MutableList<NavKey>>` and which tab is selected, following
the multiple-back-stacks recipe in the Navigation 3 documentation. Handing `NavDisplay` a different
back stack on each tab switch would not do: entries that leave the display have their
`ViewModelStore` and saved state cleared by the decorators, so a tab would come back scrolled to the
top with its ViewModels rebuilt. Each stack is decorated separately with
`rememberDecoratedNavEntries` and the ones in use are flattened into the single list `NavDisplay`
renders.

The start tab is always in that list, so back from another tab's root lands on it rather than
leaving the app. A tab's own root is never popped: an emptied stack has nothing to render, and the
tab would vanish instead of resetting.

`NavigationState` and `Navigator` hold no composition — only a `MutableState` — so `NavigatorTest`
drives every one of those rules without a UI.

One deviation from the documented recipe: the selected tab is persisted as an index rather than
through `NavKeySerializer`, which `navigation3-runtime` 1.1.1 publishes for Android only and this
build is Kotlin Multiplatform.

A feature owns its screen, its state, its ViewModel, its use cases and its Koin bindings. It exposes
outgoing user intents as callbacks and knows no destination feature. The composition root consumes
the target feature's route API and connects the callback. Screen-specific orchestration lives with
its screen. A screen action that already has the model it needs injects the capability directly —
a use case has to earn its name by holding a decision.

A feature also declares the capabilities it reads, in its own build file. `xwab.kmp.feature` hands
out the design system, navigation and the Compose/Koin surface and nothing else. Delivery, the
engine and the shipped manifest are declared by the modules that assemble a session and by the
composition root, and nowhere else — so no screen can name a delivery type, the engine's own state
model, or the URL behind a track.

Four rules hold this in place, and `./gradlew checkArchitecture` fails the build when one breaks:
a core module may not depend on a feature; feature modules may not depend on other features; a use
case in a shared core module must serve more than one feature; and a feature may not declare
`core:navigation`, `core:network`, `core:sound:delivery`, `core:playback:engine`,
`core:sound:manifest` or `core:story:manifest`.

That fourth rule reads what a screen can *reach*, not only what it names. An `api` dependency
re-exports its own `api` dependencies onto every consumer's compile classpath, so one edge added to
a module every feature already declares — `core:testing` would be enough — would have put delivery
in front of every screen without anyone declaring it. The rule follows those edges and names the
path it took.

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

One session, steered through `PlaybackCoordinator`, which must be driven from the main thread — the
platform engines behind it are main-thread-only and check it.

Its `PlaybackSummary` keeps apart four things that disagree during a switch, when the listener has
asked for B while A is still audible: `requestedItemId` (what was asked for), `activeItemId` (what
the engine holds), `playIntent` (whether playback is wanted) and `isPlaying` (whether sound is
coming out). A screen highlights and acts on the first and third. Mixing them up is how a tap during
buffering used to pause a sound the screen was showing as stopped.

`play(itemId)` claims the session before it resolves anything, so a second tap on the same sound
finds something to pause, and it releases that claim even when the lookup is cancelled. `pause()`
abandons a lookup still in flight. A lookup that came back `NotFound` or `Unavailable` reaches the
screen as a `PlaybackFailure` — which names the item it is about, because by then the session has
already fallen back to whatever came before.

It takes a `PlaybackItemId` — a kind and a raw value — not a `Music` and not a bare string. Not a
`Music`, because the metadata the media session publishes is read beside the source it is paired
with, so a screen cannot hand over a stale title. Not a bare string, because the session holds one
playback for the whole app and a sound is not the only thing that can occupy it: a sound and a story
may share a raw id, and the kind is what keeps them two items rather than one. Inside the session,
one internal resolver per kind turns an item into a URI, a title and a loop policy — which is why
adding stories to playback will not touch a screen.

### Build configuration

Module build files carry no boilerplate — the convention plugins in
[build-logic](./build-logic/src/main/kotlin) own the KMP targets, the SDK levels, the iOS guard
and the shared dependency sets:

| Plugin | For |
|---|---|
| `xwab.kmp.library` | every KMP library module |
| `xwab.kmp.compose` | the above plus Compose Multiplatform |
| `xwab.kmp.feature` | the above plus the design system, navigation and the Compose/Koin surface every screen uses — capability modules are declared per feature |
| `xwab.kmp.feature.api` | a Compose-free feature API containing public routes and serializers |

`shared` is the exception and configures itself: it is the only module producing an iOS framework.

They are `Plugin<Project>` classes rather than precompiled `.gradle.kts` script plugins on
purpose: compiling those needs Gradle's `kotlin-dsl` plugin, which is published only to
plugins.gradle.org. This build resolves everything from Google's Maven and Maven Central, and
`build-logic/settings.gradle.kts` declares both repository blocks explicitly to keep it that way.

### Adding a feature

```powershell
./tools/new-feature.ps1 sleep-timer
```

That writes `api` and `impl`, their build files and a route/screen/ViewModel/Koin skeleton. Gradle
finds both modules on its own because `settings.gradle.kts` scans `feature/`. The script prints the
remaining explicit app-composition steps:

1. add the API and implementation dependencies to `shared/build.gradle.kts`;
2. add the implementation module to `AppModules.kt`;
3. add its entry and serializer to `AppNavigation.kt`;
4. make it reachable: either add it to `TOP_LEVEL_DESTINATIONS`, or connect an existing feature's
   intent callback to its route in `AppNavigation.kt`.

Registration alone never claims that a nested feature is visible. Architecture checks reject every
cross-feature dependency, including `impl → api`, and reject an `:api` module that depends on its
own implementation.

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

The Android host suite runs Koin's `verify()` over every feature definition, including route
parameters, so a missing use-case or ViewModel dependency fails before a screen is opened.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
