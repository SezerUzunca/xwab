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

Every feature is one Gradle module. Routes, entry providers, UI, state, ViewModels, use cases and
the feature's Metro dependency bag live together; there is no feature API/implementation module
split. Features never depend on one another. The app shell connects outgoing feature intents to
destination routes.

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
implicit public visibility and an explicit `public` modifier are both valid; the current port code
uses the implicit style. Hand-written production declarations outside `.port` packages are
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

Implementations such as manifest, DataStore, Ktor, cache and platform playback adapters stay
internal. Metro discovers them through `@ContributesBinding(AppScope::class)`; `@Inject` constructs
them and `@SingleIn(AppScope::class)` owns their lifetime. Android and iOS graphs are generated at
compile time, so missing or ambiguous bindings fail compilation.

Metro's generated public contribution providers return ports, keeping the concrete adapter types
hidden. Feature `*Dependencies` classes are public DI contracts containing ports with internal
properties; `shared.di` exposes `() -> Dependencies` providers so the composition root can register
feature entries without initializing their ports. Each feature invokes its provider only inside
the entry's ViewModel initializer.

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

The seven directories directly under `core` are Gradle modules, discovered automatically by Gradle.
`shared` automatically includes those modules on Metro's compilation classpath: settings publishes
the discovered list, so `shared` never reads another project's state to find them. Application
routes remain explicitly composed by the shell.

`SOUND_CACHE_NAMESPACE` is public for one reader, the composition root. Only the module that
assembles the app knows which cache namespaces are still installed, and it names them so that
downloads left behind by a removed content type can be swept at launch.

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
3. Keep public contracts in `.port` and contribute internal implementations through Metro.
   Playable content modules contribute a playback resolver under a stable key and keep physical
   sources private to that module.
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

`BrowseRoute` is the initial destination. Browse and Favorites are top-level destinations;
Category and Sound are nested destinations. Story remains a separate content feature while
sharing the content-neutral playback port.

`feature:nowplaying` is the one feature with no route. It is chrome rather than a destination, and
reaches the screen as a `NavDisplay` scene decorator: `NavDisplay` draws it around whichever scene
is showing, so it survives every destination change and every tab switch. Its public contract is
still a single declaration in its `.navigation` package, and the composition root is still the only
module allowed to name it — a composable the shell decorates scenes with, where the others hand
back an entry the shell registers. No feature knows it exists.

A scaffold slot would be simpler, and is what Google's Common UI recipe uses for the tab bar, which
stays there. The bar is inside the navigation area instead because that is the only place it can
reach `NavDisplay`'s `SharedTransitionScope` — a bar that expands into the screen for what it is
playing has to hand its content to that screen, and shared elements only match within one
`SharedTransitionLayout`. The bar opens the current sound's detail or the Stories tab; the
expand-into-the-screen animation remains future work.

`NavDisplay` animates between *decorated* scenes, so during a navigation the outgoing and incoming
scenes both draw a bar. One shared-element key matches the two, which moves the bar rather than
cross-fading it. Google's `navscenedecorator` recipe goes further — one `movableContentOf` carried
between scenes, plus a size-caching modifier for the vacated space — because its navigation bar owns
animation state that must not be duplicated. This bar owns none: its state is a ViewModel on the
root store, so both compositions read the same instance. Give the bar state of its own and the
recipe's version becomes the right one again.

## Playback and delivery

`PlaybackPort` controls the single app-wide session. It accepts a `PlaybackItemId` containing a
kind and value, keeping sound and story identifiers distinct. Its internal adapter resolves
metadata and content through ports, then drives `PlaybackEnginePort`.

`PlaybackSummary` carries the title of the item it names, so `feature:nowplaying` can draw a bar
over whatever is playing without asking either catalog what a `PlaybackItemId` means. The title is
published only while the engine holds the item the summary names as requested; mid-switch it is
absent and `isPreparing` says so instead.

That bar offers play/pause and an open-item intent. `shared.composition` maps that intent to a
route; the bar never imports another feature. Its ViewModel is not scoped to a navigation entry:
the scene decorator draws it beside the entry content, outside the entry's ViewModel decorator,
so `viewModel` resolves the root owner. Both transitioning scenes share that one instance.

Sound details and Stories both expose the session sleep timer. Each feature observes and controls
it through `PlaybackPort`; only the stateless timer control and its labels live in `designsystem`.

`SoundPlaybackResolver` reads metadata through `SoundPort`, looks up its own internal source and
passes a request to `DeliveryPort`. The sound module owns its cache namespace, accepted MPEG types
and complete retained-file inventory. Delivery prefers cached files; otherwise it returns the
HTTPS source immediately and starts a background download.
`StoryPlaybackResolver` reads metadata through `StoryPort`, looks up its own internal HTTPS source
and returns it for streaming without caching. Both resolvers supply metadata and loop policy through
`PlaybackItemResolver`; neither the session nor the platform engine needs to know the content type.

Android playback uses Media3; iOS playback uses AVFoundation. Platform implementations are
internal Metro contributions behind `PlaybackEnginePort`.
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
6. A feature exposes anything except navigation contracts or DI `*Dependencies` classes, or shared
   references features outside the navigation/composition and DI boundaries.
7. Designsystem depends on an application project.
8. A module directory is absent from the build, or a core/feature module is absent from shared's
   compilation graph. Core registration is automatic; feature composition stays explicit.
9. A feature route lacks `@SerialName`, or a contributed playback kind has no routing reference
   in the shell.
10. The download source and native player application metadata disagree on the HTTP user agent.
11. A capability renames a value it has already written onto devices. Playback kinds, favourites
    and cache namespaces are pinned in `wireFormat`; the constant and its pin must change together,
    which is the moment to decide whether a migration is owed. Any `*_NAMESPACE` / `*_KIND`
    constant must be pinned, so a new content type joins the check by being named.

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

The Metro convention additionally treats non-public contribution problems as errors and generates
providers that allow internal contributed adapters to remain hidden across modules.

## Adding a feature

```powershell
./tools/new-feature.ps1 sleep-timer
```

The script creates one `:feature:sleep-timer` module. Then wire its dependency bag, entry provider
and serializer into `shared`, and make the route reachable from either a top-level destination or
an existing feature intent.

## Removing a feature

Delete the `feature/<name>` directory. Gradle stops including it on its own, and every remaining
reference is a compile error: the `projects.feature.<name>` accessor in `shared/build.gradle.kts`,
the accessor in `AppGraph`, the registration in `AppEntryProvider`, the entry in
`FEATURE_SERIALIZERS`, and the tab in `TOP_LEVEL_DESTINATIONS` if it had one. Follow the compiler
until it stops, then run the checks below.

Two things the compiler cannot point at:

- **The tab label** in `shared/src/commonMain/composeResources/values/app.xml`. Deleting a tab
  leaves its `tab_*` string behind, resolving happily to a name nothing asks for.
  `TopLevelDestinationsTest` fails on it rather than letting it ship.
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
