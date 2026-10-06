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
                     ├── feature:sound
                     ├── feature:story
                     └── feature:nowplaying
                              │
                              └── public core ports
                                      │
                                      └── internal Metro adapters
```

Every feature is one Gradle module. Routes, entry providers, UI, state, ViewModels and use cases
live together; there is no feature API/implementation module split. Features never depend on one
another. The app shell connects outgoing feature intents to destination routes.

`designsystem` and the `testing` modules are top-level support modules. They are deliberately
outside `core`: core reserves its public surface for ports, while UI components and reusable test
fakes are not application capability ports.

Test fakes are split by the port they stand in for — `:testing:session` (`FakePlaybackPort`),
`:testing:favorites` (`FakeFavorites`) and `:testing:sound` (`FakeSoundCatalog`, `track()`,
`category()` and the sound-namespace `FakeFavorites(Set<TrackId>)` builder). A feature's tests
declare only the ones it reads, so the story list and the now-playing bar compile their tests
against no sound catalog, and removing a capability breaks only the tests that used it. The
`testing/` directories are discovered like `core/` and `feature/`.

`designsystem` owns Material theme integration and stateless visual controls.
Each feature owns its screen state; core capabilities manage their own operation state.
Designsystem is independent of application projects; core cannot depend on it or on the app shell.

## Core boundary

Every type crossing a core-module boundary lives in a `.port` package and is public. Kotlin's
implicit public visibility and an explicit `public` modifier are both valid, and the port code uses
both. Hand-written production declarations outside `.port` packages are
`internal` or `private`. Core modules may import another core module only through that module's
`.port` package.

There is no shared repository abstraction. A feature consumes the narrow capability it needs:

| Capability module | Public port |
|---|---|
| `:core:sound` | `SoundPort`, sound metadata models, `SOUND_FAVORITES_NAMESPACE`, `SOUND_PLAYBACK_KIND` and `SOUND_CACHE_NAMESPACE` |
| `:core:delivery` | `DeliveryPort`, `DeliveryRequest`, `CacheKey` and `DeliveryResult` |
| `:core:favorites` | `FavoritesPort` |
| `:core:story` | `StoryPort`, story metadata models and `STORY_PLAYBACK_KIND` |
| `:core:session` | `PlaybackPort` and session models; `PlaybackItemResolver`, `ItemResolution` and `PlaybackPolicy` for content adapters, behind the `PlaybackResolverApi` opt-in |
| `:core:playback` | `PlaybackEnginePort` and engine command/state types |
| `:core:network` | `NetworkPort` and transport-neutral response/error types |

Every core module wires its own implementation in its own internal Metro graph (`NetworkGraph`,
`DeliveryGraph`, `FavoritesGraph`, `SoundGraph`, `StoryGraph`, `SessionGraph`, and per platform
`AndroidPlaybackGraph` / `IosPlaybackGraph`), scoped to that module (`@SingleIn(DeliveryScope::class)`
and so on). Library objects are provided there with `@Provides` — the `HttpClient`, the DataStore —
and internal services bind their internal interfaces there, so the delivery cache store and the
prefetcher share one store because the module graph scopes it.

Only ports leave a module. A small `*GraphAdapter` contributes the module graph's port to `AppScope`
with `@ContributesBinding` (or, for a content resolver, `@ContributesIntoMap` under its playback
kind); a module that hands out two things builds its graph once in a `*GraphHolder` both adapters
share. What a module needs from another — `NetworkPort` for delivery, `DeliveryPort` for sounds, the
engine and the resolver map for the session — the adapter takes from `AppScope` and passes to its
graph's factory. The resolver map therefore stays a multibinding of the application graph, and
content modules still plug in and out there. A platform value — the favorites file, the cache
directory — is built by that platform's adapter (`AndroidDeliveryGraphAdapter`,
`IosFavoritesGraphAdapter`, …), on Android from the application `Context` it takes, so it never
becomes an application binding either; delivery, favorites and playback have one adapter per platform for that
reason. Beyond ports, a core module adds only its `*GraphHolder`, where it has one, to the
application graph. Android and iOS application graphs and every module graph are generated at
compile time, so a missing binding inside a module fails that module's own compilation.

Each module's tests build its module graph (`DeliveryGraphTest`, `SoundGraphTest`,
`StoryGraphTest`, `SessionGraphTest`, `NetworkGraphTest`, the favorites graph test) with test
doubles for its inputs; playback's platform graphs are built on a device and on the simulator
(`AndroidPlaybackGraphTest`, `IosPlaybackGraphTest`). Library objects are provided inside module graphs rather than by a binding
container contributed to `AppScope`, because such a container would have to be public, and core
declarations outside `.port` stay internal.

ViewModels use Metro's own integration, [MetroX ViewModel](https://github.com/ZacSweers/metro/tree/1.4.5/metrox-viewmodel-compose).
Each internal ViewModel contributes itself with `@ViewModelKey` and
`@ContributesIntoMap(AppScope::class)`, and Metro constructs it and its use cases from the ports.
A screen that needs its route's id (category, sound and story detail) is `@AssistedInject`: its
nested `@AssistedFactory` is a `ManualViewModelAssistedFactory` contributed the same way, and the
entry calls `assistedMetroViewModel<VM, VM.Factory> { create(id) }`. `AppGraph` extends
`ViewModelGraph`; `App` places the graph's factory in `LocalMetroViewModelFactory`, and entries call
`metroViewModel()`. The maps hold providers, so registering entries creates nothing; the navigation
entry's ViewModelStore retains and clears each screen's ViewModel.

Core modules treat Metro's non-public contribution diagnostic as an error, because the contribution
it reports would silently miss the application graph. Feature modules report it as a warning: an
assisted factory contributes as its own internal type, which does reach the graph, so each one
suppresses the warning where it is declared (`@Suppress("NON_PUBLIC_CONTRIBUTION_WARNING")`), while
any other non-public contribution in a feature is still reported.

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
├── favorites
├── session
└── sound
feature/
├── browse
├── category
├── favorites
├── nowplaying
├── sound
└── story
```

Every directory under `core` with a build script is a Gradle module, discovered by settings.
`shared` automatically includes those modules on Metro's compilation classpath: settings publishes
the discovered list, so `shared` never reads another project's state to find them. Application
routes remain explicitly composed by the shell.

| Module | Owns | Delegates |
|---|---|---|
| `sound` / `story` | Catalog metadata, private sources and lookup, stable content identities, playback resolver and policy | Sounds delegate cache/download work to `DeliveryPort`; stories stream directly |
| `session` | Current playback intent, request ordering, summary for screens and the resolver contract it consumes | Item resolution and platform commands through ports |
| `playback` | Native engine, media controls, playback state, sleep timer execution | No application module dependency |
| `delivery` | Local-first delivery, downloads, cache validation and cleanup | HTTP through `NetworkPort` |
| `network` | HTTP transport and transport errors | No content, caching or playback policy |
| `favorites` | Persistence of caller-owned namespaces and IDs | No catalog knowledge |

Physical source addresses and lookup remain internal to their owning content module. Each
playable content module contributes its own `PlaybackItemResolver`; the session selects it by
kind through the consumer-owned port in `com.xwab.app.core.session.port`. A resolver reads its
own metadata and source and returns a content-neutral resolution. There is no separate source
registry or source registration step.
Sound depends on session and delivery; story depends on session; session depends only on playback.
Features cannot depend on delivery, network or the native engine. The resolver contract is public
in Kotlin because content modules implement it across module boundaries, and Kotlin has no
visibility for "these modules only" yet. Two checks stand in for one. The compiler rejects any use of
the resolver and its result/policy models that does not opt in to `@PlaybackResolverApi`; the
resolvers and the session's own adapter opt in, and screens have no reason to. Session's
`adapterOnlyTypes` policy makes `checkArchitecture` reject feature references to the same types,
the opt-in annotation included, so a feature cannot opt in quietly either.

Contracts and models live in each module's `.port` package. Adapters and manifests remain internal.
Every core module supplies an [architecture.properties](core/session/architecture.properties)
contract declaring its responsibility, feature visibility, permitted dependencies and public
interfaces, with optional `adapterOnlyTypes` for contracts reserved for adapters.
`checkArchitecture` validates those declarations against the actual code and production dependency
graph; a new module without a contract fails the check.

### Adding a content type

`:core:session` publishes the `PlaybackItemResolver` port it consumes. A content module implements
it and contributes it with
`@ContributesIntoMap(AppScope::class) @StringKey(ITS_OWN_KIND)`. `:core:session` injects
`Map<String, PlaybackItemResolver>` and looks up the requested kind. Its only core dependency is
`:core:playback`; the session never names a sound, story or other content implementation.

A new playable content type supplies an internal `PlaybackItemResolver` backed by its own private
sources. The session needs no edits and supports an empty resolver map. Infrastructure capabilities
such as favorites or network do not need a playback resolver.
Removing a content type removes its resolver registration; an unknown playback kind reports `ItemNotFound`,
including when the engine still holds an ID belonging to a removed module.

Each content module owns the string naming its kind — `SOUND_PLAYBACK_KIND`, `STORY_PLAYBACK_KIND`
— because that string is the engine source id's prefix and outlives the process. The architecture
check verifies that the app shell names every registered playback kind in its routing composition.

### Adding, removing or replacing core modules

1. Add a flat `core/<name>` module with `build.gradle.kts` and `architecture.properties`.
2. Declare its owned responsibility and exact public interfaces; specify only required core
   dependencies. Empty `dependencies=` means the module is independent.
3. Keep public contracts in `.port`. Wire the internal implementation in the module's own
   internal Metro graph, and contribute only its port to `AppScope` through a `*GraphAdapter`
   that passes the graph whatever it needs from other modules. Playable content modules also
   contribute their resolver under a stable key and keep physical sources private to the module.
   Test the module graph directly.
4. Wire any feature and route that presents the new capability in the app shell.
5. Run `:check`, Android host tests and `:androidApp:assembleDebug`.

To remove a module, remove its consumers or supply a replacement implementing the required port,
then delete the module directory and update the affected dependency contracts. Core discovery and
Metro registration update automatically. Required dependencies are intentional compile-time
requirements: removing `network` while retaining `delivery` requires another transport adapter.
This is build-time modularity; modules are not dynamically unloaded from a running application.

Replacing an adapter preserves its port and installs one implementation for that binding. Metro
rejects duplicate single bindings or duplicate contribution keys. Preserve stored IDs, cache
namespaces, favorite namespaces and route serial names, or provide an explicit migration. Removing
a feature also requires the shell changes described below; unrelated core implementations remain
untouched.

## Navigation 3

`shared` owns the app-level navigation policy and one back stack per top-level destination.
Feature route keys and entry providers live in each feature's `.navigation` package. Only
the app shell's navigation-composition boundary may import those packages; feature code publishes
intent callbacks and does not name destination features.

`BrowseRoute` is the initial destination. Browse, Favorites and Stories are top-level destinations;
Category, Sound and Story details are nested destinations. Every content row opens its detail from
the row body; only its explicit play/pause button changes playback. `StoryRoute(storyId)` opens the
selected story's description, author, narrator and duration. Each tab retains its own history and saved screen state.

`AppNavigationHost` wires features to `Navigator`; `AppNavigationDisplay` owns the actual
`NavDisplay`. Saveable state and ViewModel entry decorators are retained for every tab, including
inactive tabs, in the documented order. Tab-scoped content keys prevent the same sound in Browse
and Favorites from sharing an entry store. A destination already on the stack is revisited by
popping to it, preserving its parents and state. Anything else is pushed, as in the official
recipes: choosing Ocean after Rain beside a list keeps Rain in history, and Material's
`PopUntilCurrentDestinationChange` back behavior skips both on one Back to the list.
Selecting another tab preserves its history; reselecting returns to its root, and a further tap
at the root scrolls its list to the start.

The chrome is the same on every screen, so it sits outside `NavDisplay`: Material's
`NavigationSuiteScaffold` chooses a short navigation bar or a wide rail from the window size
class, and the now-playing bar is drawn once below the content. Screens draw their own background
behind the status bar and inset only their content. Material Adaptive Navigation 3 supplies list,
detail, extra panes and empty-detail placeholders, including internal predictive Back. Pane metadata is scoped to a tab: a sound opened from the
now-playing bar, with no category beneath it, falls back to a full-screen entry. The default
display handles compact screens. Global forward, pop and predictive-pop transitions are explicit.
Back controls disappear only when the actual parent pane is visible. While
several panes are visible, a pane's back arrow closes that pane and what was opened from it; system
Back still removes the latest entry.

See [Navigation 3 coverage and official references](docs/NAVIGATION3_ARCHITECTURE.md) for policies,
optional recipes, validation and platform limits. The navigation composition test suite is written
once for Android devices and the iOS simulator. CI runs it on the simulator; on Android it runs on
local devices, because on CI's emulator it exceeded Compose's test timeout.

`feature:nowplaying` is only the persistent now-playing bar, in its `shell` package; it has no
route or screen of its own. Tapping the bar opens the playing item's own screen in the tab it
belongs to — `SoundRoute` under Sounds, `StoryRoute` under Stories — as a fresh selection from that
tab's root, or returns to it if that detail is already open there. The tab the listener was on
keeps its stack. The shell owns that mapping; nowplaying consumes only the content-neutral session
port.

The bar's presentation ViewModel belongs to the app root and observes the app-scoped playback
session; clearing it does not stop playback. While a timer runs with nothing requested, the bar
stays on screen as a "Sleep timer" bar with its own cancel action, since there is no item screen
to open.

## Playback and delivery

`PlaybackPort` controls the single app-wide session. It accepts a `PlaybackItemId` containing a
kind and value, keeping sound and story identifiers distinct. Its internal adapter resolves
metadata and content through ports, then drives `PlaybackEnginePort`.

`PlaybackSummary` carries the title of the item it names, so `feature:nowplaying` can draw a bar
over whatever is playing without asking either catalog what a `PlaybackItemId` means. The title is
published only while the engine holds the item the summary names as requested; mid-switch it is
absent and `isPreparing` says so instead.

The now-playing bar shows play/pause, preparation or failure state, and the active sleep timer's
remaining time. It steps aside while the playing item's own detail is showing, since that screen
already has its play/pause and timer. The app does not expose a playback progress bar, seeking
controls or an in-app volume slider; the device's volume keys set loudness. `PlaybackPort` has no
volume at all: the engine's gain stays inside `:core:playback`, which features cannot reach, and
every load starts at full gain. Sound durations read as one loop ("0:12 loop").

Sound and Story details expose item-specific playback, metadata and errors; Sound also exposes
favorites. Under those, a `designsystem` card holds the session's sleep timer; it acts on whatever
is playing and can be set before pressing play. There is no repeat control: a sound always loops
until the timer stops it, and a story plays once.

`SoundPlaybackResolver` reads metadata through `SoundPort`, looks up its own internal source and
passes a request to `DeliveryPort`. The sound module owns its cache namespace, accepted MPEG types
and complete retained-file inventory. Delivery prefers cached files; otherwise it returns the
HTTPS source immediately and starts a background download.
`SoundPort.observeOfflineReady` reports verified cache readiness without starting a download.
Sound details show "Available offline" only while that signal is true, otherwise "Internet
required". Readiness updates as cached files become available or are removed.
`StoryPlaybackResolver` reads metadata through `StoryPort`, looks up its own internal HTTPS source
and returns it for streaming without caching; story details state that internet is required.
Both resolvers supply metadata and loop policy through
`PlaybackItemResolver`; neither the session nor the platform engine needs to know the content type.

Android playback uses Media3; iOS playback uses AVFoundation. Platform implementations are
internal and bound in each platform's playback graph; only `PlaybackEnginePort` reaches the
application graph.
Both native streaming paths read the application's HTTP identity from platform metadata (Android
manifest / iOS Info.plist). iOS applies it through `AVURLAssetHTTPUserAgentKey` on initial loads and
queue rebuilds; cached files need no HTTP options. No application identity is hard-coded in core
playback. The architecture check requires these values to agree with the download identity.

## Architecture enforcement

`checkArchitecture` fails when any of these rules is broken:

1. A core module lacks a valid `architecture.properties` contract, exposes a different set of
   callable interfaces, or declares a dependency outside its allowed list. Missing dependency
   targets and core dependency cycles also fail.
2. A core module depends on a non-core application module, or a feature depends on another feature.
   Core and feature modules must be flat; `api`/`impl` directory splits are forbidden.
3. A feature reaches a core module marked `featureAccessible=false`, directly or through an
   exported dependency, or references a port type listed in that module's `adapterOnlyTypes`.
   A core module that implements another's `adapterOnlyTypes` may not reference that module's
   remaining `publicInterfaces`: answering a capability's contract and calling it are separate
   roles, and one module holding both puts the coordination back where it was moved from.
4. A production core declaration outside its own exact `.port` package is public, a port member
   is non-public, or a cross-core reference bypasses the target module's `.port` package.
5. Screen state or a feature-specific use case leaks into core; a `Repository` / DI-style
   `Provider` abstraction appears in core; or Koin is reintroduced.
6. A feature exposes anything except navigation contracts and shell UI (`shell` package, e.g. the
   now-playing bar the app places below every screen), or shared references features outside the
   navigation/composition boundary.
7. Designsystem depends on an application project.
8. A module directory is absent from the build, or a core/feature module is absent from shared's
   compilation graph. Core registration is automatic; feature composition stays explicit.
9. A feature route lacks `@SerialName` or is missing from its feature's serializers module, a
   feature's serializers module is not included in the shell's route serializers, or a
   contributed playback kind has no routing reference in the shell.
10. The download source and native player application metadata disagree on the HTTP user agent.
11. A capability renames a value it has already written onto devices. Playback kinds, favourites
    and cache namespaces are pinned in `wireFormat`; the constant and its pin must change together,
    which is the moment to decide whether a migration is owed. Any `*_NAMESPACE` / `*_KIND`
    constant must be pinned, so a new content type joins the check by being named.
12. A feature ViewModel is not registered in the app graph's ViewModel map: a plain one lacks
    `@ViewModelKey` and `@ContributesIntoMap(AppScope::class)`, or an `@AssistedInject` one has no
    nested factory with an assisted-factory key and that contribution. Screens resolve ViewModels
    from the map at runtime, so this would otherwise compile and throw when the screen opens.

The core policy is module-owned rather than a central list of sound/story-specific exceptions.
For example, `core/session/architecture.properties` permits only playback and declares its screen
and resolver ports. Production dependency checks exclude test configurations so test fakes do not
become application dependencies.

```properties
responsibility=Coordinate playback through contributed resolvers and the platform engine.
featureAccessible=true
dependencies=:core:playback
publicInterfaces=PlaybackPort,PlaybackItemResolver
adapterOnlyTypes=PlaybackItemResolver,ItemResolution,PlaybackPolicy,PlaybackResolverApi
```

Port checks enforce code boundaries and dependency direction. The responsibility sentence is a
review contract: behavior and tests must still demonstrate that an adapter stays within its job.

The dependency graph the check reads is reported by the modules themselves. Every module applies
`xwab.architecture.module` — through `xwab.kmp.library`, or by id in `shared` and `androidApp` —
which writes its own production project dependencies to a file. The root resolves those files like
any other dependency, and settings publishes which modules there are. No project reads another's
configurations, which keeps the check compatible with Gradle's isolated projects mode. A module
that does not apply the plugin makes the check fail with Gradle's "no matching variant" error
naming it, rather than letting the rules run without it.

The Metro convention additionally reports non-public contributions (errors outside feature modules,
warnings inside them) and generates providers that allow internal contributed adapters to remain
hidden across modules.

## Adding a feature

```powershell
./tools/new-feature.ps1 sleep-timer
```

The script creates one `:feature:sleep-timer` module whose ViewModel contributes itself to the
graph. Then add the module, its entry provider and its serializer to `shared`, and make the route
reachable from either a top-level destination or an existing feature intent.

## Removing a feature

Delete the `feature/<name>` directory. Gradle stops including it on its own, and every remaining
reference is a compile error: the `projects.feature.<name>` accessor in `shared/build.gradle.kts`,
the registration in `AppEntryProvider`, the entry in `FEATURE_SERIALIZERS`, and the tab in
`TOP_LEVEL_DESTINATIONS` if it had one. Its ViewModels leave the graph with the module. Follow the
compiler until it stops, then run the checks below.

Two things the compiler cannot point at:

- **The tab label** in `shared/src/commonMain/composeResources/values/app.xml`. Deleting a tab
  leaves its `tab_*` string behind, resolving happily to a name nothing asks for.
  `TopLevelDestinationTest` fails on it rather than letting it ship.
- **Saved back stacks in installed copies.** A listener who was on the removed screen when they last
  closed the app restores a route this build no longer registers. That no longer crashes — see
  `RetiredRoute` — but it is why a route's `@SerialName` is a wire format and why removing a feature
  is a decision about people who already have the app, not only about this source tree.

Its favorites are a separate question. `FavoritesPort` namespaces are keys on disk, so a removed
content type's favorites stay stored until something deletes them.

## Build and checks

```powershell
./gradlew :androidApp:assembleDebug
./gradlew compileAndroidHostTest
./gradlew testAndroidHostTest
./gradlew checkArchitecture
./gradlew :check
```

`:check` runs the architecture check and the build-logic regression tests. Android CI runs it
alongside the Android host tests before assembling the APK. CI also runs
`./gradlew :androidApp:lintDebug`, which with `checkDependencies` lints every module the app ships.
A KMP library only has lint tasks when it applies `com.android.lint`; `xwab.kmp.library` and
`:shared` do.

`./gradlew staticAnalysis` runs detekt in every module (`xwab.detekt`, on detekt's defaults plus
[config/detekt/detekt.yml](config/detekt/detekt.yml)). Findings that predate it are in each
module's `detekt-baseline-*.xml`, so only new ones fail CI. Sources generated into `build/` are not
analysed. To record a module's current findings after a deliberate change, run
`./gradlew :<module>:staticAnalysisBaseline`. detekt is on 2.0.0-alpha, the first line built for
Kotlin 2.4; move to 2.0.0 once it is released.

On macOS, `./gradlew -PenableIos=true iosSimulatorArm64Test` also runs Compose screen and
navigation lifecycle/save-state tests. Real-device background playback and interruption checks
remain necessary before a release.

Open `iosApp` in Xcode to run the iOS application.

Audio provenance and licenses are recorded in
[THIRD_PARTY_AUDIO.md](./THIRD_PARTY_AUDIO.md).
