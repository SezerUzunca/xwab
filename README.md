This is a Kotlin Multiplatform project targeting Android, iOS.

Serenity is a free, account-free relaxation sound app. Its feature slices are `category`, `player`
and `story`; shared catalog and platform primitives live under `core`. Favouriting is a shared data
capability used from several screens rather than a screen of its own.

**The home screen is not a slice.** It lives in `shared`, the composition root, under
`com.xwab.app.home` — the arrangement Now in Android has, where the app module names `forYouEntry`
and `ForYouNavKey` directly. The trade is stated plainly in [Architecture rules](#architecture)
below and in `HomeEntry.kt`: the rules that stop every other screen from reaching the network, the
player engine or the shipped manifest cannot see a screen that lives here.

`home` and `story` are separate because they share no data: one deals in `Music`,
categories, favorites and a local cache, the other in `Story`, an author and a narrator, and a
stream that is never written to disk. The one thing they do share — the playback session — is
already content-independent, so joining the two screens would put back at the feature layer the
coupling `PlaybackItemId` removed from the core.

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

They are not equally visible, though. `core:designsystem`, `core:navigation` and `core:testing` are
things a screen uses; `core:network` is not, and rule 4 below refuses a feature that declares it. A
screen reads content through a repository — it does not make requests.

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
            ├─► shared (composition root: Koin + AppShell + the home screen)
iosApp ─────┘        │
        ┌────────────┼────────────────┬────────────────┐
        │            ▼                ▼                ▼
        │      feature:category  feature:player  feature:story
        │            │                │                │        (each with a :navigation module)
        │            │                │                └──► core:story:catalog
        └────────────┴────────────────┴──► core:sound:catalog · core:sound:favorites
                                      │
                    all four ─────────┴──► core:playback:session
                                                     │
                          ┌──────────────────────────┼─────────────────────┐
                          ▼                          ▼                     ▼
                  core:sound:delivery        core:story:manifest   core:playback:engine
                          │                          │
                          ▼                          ▼
                  core:sound:manifest         core:story:catalog
                          │                   (streams; never cached)
                          ▼
                  core:sound:catalog

  crosscutting: core:network · core:designsystem · core:navigation · core:testing
```

`feature:story` reads two capabilities where a sound screen reads three: there is no favorites port
for stories.

## Navigation

`AppShell` is a navigation bar over one back stack per tab, and it **names no feature and no route**
— not even the start destination, which it reads off the first `TopLevelDestination` in the list
`AppFeatures` builds. A slice appears on screen by publishing one on its `FeatureEntry`:

```kotlin
val storyFeature = FeatureEntry(
    koinModule = storyModule,
    entries = { storiesEntry(it) },
    serializers = storyNavigationSerializers,
    topLevel = TopLevelDestination(
        route = StoriesRoute,
        order = 1,
        label = { stringResource(Res.string.tab_stories) },
        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
    ),
)
```

`label` and `icon` are slots rather than a `StringResource` and an `ImageVector`, so each feature
fills them from its own Compose resources and `core:navigation` stays free of the design system.
A feature that leaves `topLevel` null — `player`, `category` — is navigated *into* and is not a
place to switch to.

The bar is `(listOf(homeTopLevel) + features.mapNotNull { it.topLevel }).sortedBy { it.order }`.
Adding a slice is one line in `AppFeatures`, and no line at all in the features that already exist.
Home is the one hard-coded term: it carries no `FeatureEntry`, so `AppFeatures` names its
destination, its entry, its serializers and its Koin module one by one.

Each feature also fills in `entries`, its own slice of the navigation graph, in an `XEntry.kt`
beside its screen — home does the same, in `shared`:

```kotlin
internal fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeRoute> {
        HomeScreenRoute(
            onCategoryClick = navigator::navigateToCategory,
            onMusicClick = { trackId -> navigator.navigateToPlayer(trackId.value) },
            viewModel = koinViewModel(),
        )
    }
}
```

This is the shape Now in Android uses for `forYouEntry`. The navigator is an argument rather than a
composition local, so a screen's callbacks are wired where the entry is declared and nothing depends
on the shell having provided something. Registration used to sit in each feature's Koin module,
which put the navigation graph inside the object graph.

For the three features the shell is still inverted relative to NIA: `NiaApp` lists every entry
function by name, while `AppShell` calls `appEntryProvider(navigator)` and lets the feature list
supply them. Home is the exception, and follows NIA exactly — it is named.

### What hosting home in `shared` costs

`checkArchitecture` reads Gradle paths. Rules 1–4 fire only on modules under `:core:` or
`:feature:`, and `:shared` is neither. `:shared` also declares `core:network`,
`core:sound:delivery`, `core:playback:engine` and `core:sound:manifest`, because binding them is
what a composition root does.

So the home screen **can** issue an HTTP call, resolve a track to a URI or drive the player
directly, and the build will report success. Every other screen in this app is refused all four at
compile time. This is not an oversight to be fixed by widening the rules: a rule cannot separate the
screen's code from the root's when both live in the same module, and the source-scanning version of
rule 4 that could have tried is the one this build deleted for rotting silently.

`com.xwab.app.home` is therefore held to the boundary by review rather than by the build. It reads
`MusicCatalogRepository`, `FavoritesRepository` and `PlaybackCoordinator`, and nothing else.

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
feature may not declare `core:network`, `core:sound:delivery`, `core:playback:engine`,
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
