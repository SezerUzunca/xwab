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
├── navigation
├── sound/       catalog · manifest · delivery · favorites
├── story/       catalog · manifest
├── playback/    session · engine
├── designsystem
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

Crosscutting modules — `core:network`, `core:navigation`, `core:designsystem` and `core:testing` —
are tied to no content type, so they are grouped under none of them and stay directly under
`core/`. `core:navigation` holds one thing: `componentScope()`, every feature component's
Decompose-lifecycle-scoped replacement for `viewModelScope`.

They are not equally visible, though. Screens use `core:designsystem`, feature tests use
`core:testing`, and `core:network` is hidden from features. Rule 4 below refuses a feature that
declares it. Navigation state and destination policy live with their only consumer, the app shell,
under `shared/navigation`. A screen reads content through a repository — it does not make requests.

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
            ├─► shared (App entry + application shell + Koin composition root)
iosApp ─────┘        │
                     ├─► feature:browse:{api,impl}    (catalog)
                     ├─► feature:favorites:{api,impl} (catalog + favorites + playback session)
                     ├─► feature:category:{api,impl}  (catalog + favorites + playback session)
                     ├─► feature:sounds:{api,impl}    (catalog + favorites + playback session)
                     └─► feature:story:{api,impl}     (story catalog + playback session)

Every feature follows Now in Android's two-module layout. `:api` publishes only the serializable
`Config` the composition root places in the navigation tree; Compose, Koin and Decompose's
navigation types do not travel onto its consumers' classpaths. `:impl` owns UI, state, the
Decompose component, use cases, DI and resources. Feature modules have no dependencies on other
features. `shared` constructs each feature's component directly and assembles the core adapters;
it never hosts a feature screen.
```

`feature:story` reads two capabilities where sound features read three: there is no favorites port
for stories.

## Navigation

`shared/.../App.kt` is the multiplatform entry used by Android and iOS. It renders from a single
[Decompose](https://github.com/arkivanov/Decompose) component tree, `AppComponent`, built once at
the platform edge (`MainActivity`/`MainViewController`) and retained across Android configuration
changes via `retainedComponent` — every descendant component, and the coroutine scope and hot flow
it holds, survives with it. Application policy is explicit in `shared/.../navigation/`: `AppTab`
chooses the start tab and the tab bar's items, and each tab's own `*TabConfig.kt` (`BrowseTabConfig`,
`FavoritesTabConfig`, `StoriesTabConfig`) is the closed set of screens that tab can show. Feature
components are constructed separately in `shared/.../composition/AppComponent.kt`, so the
navigation package sees feature `Config`s but never a feature implementation.

```kotlin
internal sealed interface BrowseTabConfig {
    @Serializable data object Root : BrowseTabConfig
    @Serializable data class Category(val config: CategoryConfig) : BrowseTabConfig
    @Serializable data class Player(val config: PlayerConfig) : BrowseTabConfig
}

private fun browseChild(config: BrowseTabConfig, componentContext: ComponentContext): BrowseTabChild =
    when (config) {
        is BrowseTabConfig.Root -> BrowseTabChild.Root(
            DefaultBrowseComponent(
                componentContext = componentContext,
                musicCatalog = koin.get(),
                onCategoryClick = { categoryId ->
                    browseNavigation.pushToFront(BrowseTabConfig.Category(CategoryConfig(categoryId.value)))
                },
            ),
        )
        is BrowseTabConfig.Category -> BrowseTabChild.Category(/* ... */)
        is BrowseTabConfig.Player -> BrowseTabChild.Player(/* ... */)
    }
```

`Category` and `Player` are absent from `AppTab`: they are navigated *into*, not places to switch
to, and are pushed onto whichever tab's own `StackNavigation` initiated them — a `Config` that
carries no data beyond "this feature's screen" needs no feature `api` module at all, which is why
`browse`, `favorites` and `story` publish none: `AppTab`'s label/icon and each `*TabConfig.Root`
case already say everything those screens need as navigation input. Koin modules are assembled
separately in `AppModules.kt`, but a component is never Koin-managed — it is constructed directly,
by whichever `composition/` factory places it in the tree, from an explicit `Koin` instance handed
down from the platform edge (`GlobalContext.get()`), not resolved through Compose.

The local equivalent of Now in Android's `feature:foryou:api` and `feature:foryou:impl` split is:

```text
feature:category:api    api/navigation/CategoryConfig.kt (public contract)
feature:category:impl   CategoryComponent.kt + UI + state + use case + DI
feature:favorites:api   (none — Favorites carries no navigation input of its own)
feature:favorites:impl  FavoritesComponent.kt + UI + state + use case + DI
shared                  App entry, tab/back-stack policy, top-level UI and composition root
```

`DefaultBrowseComponent`, `DefaultFavoritesComponent` and `DefaultCategoryComponent` expose only
intent callbacks (`onCategoryClick`, `onMusicClick`, …). `shared` decides that those intents push a
`CategoryConfig` or `PlayerConfig`; therefore no feature compiles against any other feature module.
Because every start-screen source lives beneath `:feature:`, `checkArchitecture` also enforces the
same dependency boundaries on the start screen as on every other feature.

### One back stack per tab

`DefaultAppComponent` owns one `StackNavigation`/`childStack` pair per tab, each created once as a
property of the single retained root rather than lazily on first selection — so a tab's components
survive switching away from it unconditionally, not only while composed. The tab-switching rules
themselves — reselecting a tab clears its sub-stack, switching tabs never pushes one tab onto
another's history, back pops within the selected tab before falling through to the start tab, a
tab's own root is never popped — live in `TabBackStackPolicy`, a small class with no Decompose
stack types in its own signature, so `TabBackStackPolicyTest` drives every one of those rules with
two fake tabs instead of the app's real three. Back-button handling is centralized: every
`childStack` is created with `handleBackButton = false`, and `AppComponent` registers exactly one
`BackCallback`, so which tab's stack responds to a back press is always this policy's decision, never
essenty's callback-dispatch order across three independently-registered handlers.

A feature owns its screen, its state, its Decompose component, its use cases and its Koin bindings.
It exposes outgoing user intents as callbacks and knows no destination feature. The composition root
constructs the target feature's component and connects the callback. Screen-specific orchestration
lives with its screen. A screen action that already has the model it needs injects the capability
directly — a use case has to earn its name by holding a decision.

A feature also declares the capabilities it reads, in its own build file. `xwab.kmp.feature` hands
out the design system, `:core:navigation` (`componentScope()`, every component's replacement for
`viewModelScope`), Decompose and the Compose/Koin surface, and nothing else. Delivery, the engine
and the shipped manifest are declared by the modules that assemble a session and by the composition
root, and nowhere else — so no screen can name a delivery type, the engine's own state model, or
the URL behind a track.

Five rules hold this in place, and `./gradlew checkArchitecture` fails the build when one breaks:
a core module may not depend on a feature; feature modules may not depend on other features; a use
case in a shared core module must serve more than one feature; and a feature may not declare
`shared`, `core:network`, `core:sound:delivery`, `core:playback:engine`,
`core:sound:manifest` or `core:story:manifest`. Finally, the app navigation package may import
feature APIs but not feature implementations; component construction belongs to the composition
root.

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

That writes `api` and `impl`, their build files and a Config/screen/component/Koin skeleton. Gradle
finds both modules on its own because `settings.gradle.kts` scans `feature/`. The script prints the
remaining explicit app-composition steps:

1. add the API and implementation dependencies to `shared/build.gradle.kts`;
2. add the implementation module to `AppModules.kt`;
3. make it reachable: either give it its own tab (a new `*TabConfig.kt`, an `AppTab` entry and a
   `childStack` in `DefaultAppComponent`), or add its `Config` as a case in the pushing tab's
   `*TabConfig.kt` and a matching branch in that tab's child factory in
   `shared/.../composition/AppComponent.kt`.

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

The Android host suite runs Koin's `verify()` over every feature's use-case bindings, so a missing
dependency fails there instead of the first time a screen is opened.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
