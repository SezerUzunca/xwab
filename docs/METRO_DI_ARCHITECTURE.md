# Metro DI architecture and scope

October 7, 2026. This document describes the source tree reviewed on this date, with Metro 1.4.5, including the platform-specific network graphs. It separates Metro's dependency-injection mechanisms from this project's port boundaries and resource ownership. Navigation composition and entry lifetimes are documented in [Navigation 3 architecture](NAVIGATION3_ARCHITECTURE.md).

## Versions

- Metro compiler plugin and MetroX ViewModel / ViewModel Compose: 1.4.5.
- Kotlin: 2.4.20; Compose Multiplatform: 1.12.1.
- Lifecycle: 2.11.0; kotlinx.coroutines: 1.11.0.
- Libraries constructed inside capability graphs include Ktor 3.6.0, DataStore 1.2.1 and Media3 1.11.1.

The version catalog is the source of truth. The official documentation's latest examples may contain APIs newer than this project. Version-pinned Metro sources are linked below.

## Responsibilities

| Component | Responsibility |
| --- | --- |
| `xwab.kmp.library` | Apply Metro to library modules, generate contribution providers and reject unsupported non-public contributions |
| `xwab.kmp.feature.api` | Configure a feature's contract module: routes, their serializer contributions and callback / shell chrome contracts, with no UI toolkit or ViewModel |
| `xwab.kmp.feature.impl` | Configure feature contributions, including the diagnostic policy for internal assisted ViewModel factories, and add the sibling api module |
| Android / iOS application graph | Aggregate installed `AppScope` contributions and expose the MetroX ViewModel integration |
| Internal capability graph | Construct and connect one core module's implementation, SDK objects and internal services |
| `*GraphAdapter` | Contribute a capability port to the application graph and pass external inputs into the local graph |
| `*GraphHolder` | Build one local graph shared by multiple bridges; used by sound and story |
| `@BindingContainer` | Share binding declarations between graphs without making the container a complete graph |
| Feature ViewModel factory | Combine injected dependencies with runtime route arguments through MetroX |
| `AppEntryGraph` | In `:composition`: aggregate the entry installers and the now-playing bar features contribute to `EntryProviderScope::class`, with the shell's navigation callbacks, over the navigator the host passes in |
| `RouteSerializersGraph` | Aggregate the route serializer modules features contribute to `NavKey::class` for saved back stacks |
| Resource owner | Cancel jobs, release players and sessions, and close resources when its lifetime ends |
| `checkArchitecture` | Enforce project dependency, visibility, port and ViewModel-registration rules |

Metro constructs the dependency graph at compile time. It does not choose a capability's responsibility, enforce every architectural boundary by itself, or automatically dispose of resources. [Official graph APIs](https://github.com/ZacSweers/metro/blob/1.4.5/docs/dependency-graphs.md).

## Project rules

The rules are written in the root README, each core module's `architecture.properties`, and the checks in `build-logic`. This document explains their DI consequences; it does not introduce exceptions to them.

- Every production declaration exposed by a core module belongs to its exact `.port` package. Production declarations outside it remain `internal` or `private`.
- Core modules reference other core modules only through those modules' ports. Port contracts cannot refer to implementation packages, including their own.
- Each core module declares its responsibility, allowed project dependencies, feature accessibility and public callable interfaces in `architecture.properties`.
- Features consume only capabilities permitted by those contracts. Infrastructure modules such as network, delivery and playback are not directly feature-accessible.
- Each core capability is an `api` module, holding its `.port` package alone, and an `impl` module, holding the adapters and the internal module graph; its `architecture.properties` sits beside them. Every consumer compiles against api modules; a capability's impl depends on its own api and the api of the capabilities it declares; only `:composition` depends on an impl. Repository/provider abstraction layers and Koin are not part of this architecture.
- Each feature is an `api` module and an `impl` module. The api module is the feature's public composition surface: routes, callback contracts and shell chrome contracts. The impl module keeps its types internal and exposes only the binding containers it contributes with `@ContributesTo`; only `:composition` depends on it.
- `:composition` is the composition root: the only module that sees every implementation, so it declares the graphs that collect them and names no feature itself. `:shared`, the app shell, depends on feature api modules only and receives what the graphs build through `App`'s parameters.

Kotlin visibility and Gradle dependency declarations establish the boundaries; the architecture task checks them. Metro supplies objects within those boundaries. A compiling graph alone does not prove that a module obeys its responsibility.

## Application graph and contribution discovery

`AppGraph` extends MetroX's `ViewModelGraph`. `AndroidAppGraph` and `IosAppGraph` declare `@DependencyGraph(AppScope::class)` and implement that shared surface.

Android provides its application `Context` through `AndroidAppGraph.Factory`, using `@Provides @GraphPrivate`. `MainApplication` implements MetroX's `MetroApplication` and creates the application graph once, lazily. `AndroidAppGraph` also extends `MetroAppComponentProviders`, so MetroX's `AppComponentFactory` (API 28+, hence `minSdk` 28) constructor-injects `MainActivity`, contributed with `@ActivityKey`; it receives the `MetroViewModelFactory` and passes it to `App` with `AppEntryGraphs`, the factory for each navigation host's entry graph. The Activity lives in `:composition`'s Android sources beside the graph, because only the module declaring a graph sees its contributions. The device-test APKs of `:composition` (its graph tests) and `:shared` (the shell against the composition root) restore AndroidX's default factory, since neither has a `MetroApplication`; lint's `Instantiatable` check is silenced on the Activity as in Metro's Android sample. iOS provides no factory input: `MainViewController.kt` holds one lazy application graph reused by its controllers. Platform capability graphs derive their own native values.

`composition/build.gradle.kts` includes all core and feature modules from the module lists discovered by settings, and `shared/build.gradle.kts` the feature api modules; none is listed by hand. The shell's tests also depend on `:composition` and the modules it installs, because a dynamic graph is generated from its own compilation's classpath; test configurations are not production dependencies. Metro discovers contributions from the compilation classpath: annotating a class in an uninstalled module does not install that module. Architecture checks enforce module registration and dependency direction.

The base convention sets:

```kotlin
metro.generateContributionProviders.set(true)
metro.nonPublicContributionSeverity.set(DiagnosticSeverity.ERROR)
```

Generated contribution providers allow an internal `@ContributesBinding` adapter to contribute its public bound port across a module boundary. This is Metro's documented cross-module mechanism, not a public implementation API written by the project. [Contribution providers](https://github.com/ZacSweers/metro/blob/1.4.5/docs/aggregation.md#generatecontributionproviders).

This mechanism does not make an internal `@ContributesTo` binding container accessible to an application graph generated in another module. Under the current rules, SDK providers belong in the core module's own graph. A container included by a local graph can remain internal because both are compiled within the same module.

Features override the diagnostic severity to `WARN`. Their internal nested assisted factories contribute as their own factory type; the existing declarations locally suppress `NON_PUBLIC_CONTRIBUTION_WARNING`. This narrow suppression does not disable the diagnostic for the rest of a feature.

## Capability graphs

| Module | Graph and scope | External inputs | Export through the application graph |
| --- | --- | --- | --- |
| `core:network` | `AndroidNetworkGraph` / `IosNetworkGraph`, `NetworkScope` | None; each platform graph provides its own engine | `NetworkPort` |
| `core:delivery` | `AndroidDeliveryGraph` / `IosDeliveryGraph`, `DeliveryScope` | `NetworkPort`; Android receives a cache directory derived by its bridge | `DeliveryPort` |
| `core:favorites` | `AndroidFavoritesGraph` / `IosFavoritesGraph`, `FavoritesScope` | Android `Context`; iOS resolves its file manager locally | `FavoritesPort` |
| `core:playback` | `AndroidPlaybackGraph` / `IosPlaybackGraph`, `PlaybackScope` | Android `Context`; iOS resolves native player dependencies locally | `PlaybackEnginePort` |
| `core:session` | `SessionGraph`, `SessionScope` | `PlaybackEnginePort` and the contributed resolver provider map | `PlaybackPort` |
| `core:sound` | `SoundGraph`, `SoundScope` | `DeliveryPort` | `SoundPort` and one `PlaybackItemResolver` map entry |
| `core:story` | `StoryGraph`, `StoryScope` | None | `StoryPort` and one `PlaybackItemResolver` map entry |

Android playback additionally has `PlaybackServiceGraph` with `PlaybackServiceScope`. Its inputs are the service's `Context` and `MediaSession.Callback`; its player and session belong to that service instance, independently of the application graph.

The construction path for a capability is:

```text
Application graph (AppScope)
  -> internal GraphAdapter, requested as a public port
     -> internal module graph (module scope)
        -> injected implementation and provided SDK objects
```

External capability ports travel through the local graph's factory as `@Provides` inputs. Local graphs do not implicitly inherit application bindings. They also do not depend on another module's concrete graph.

For a single exported port, the bridge creates the graph directly. Sound and story export a catalog and a resolver, so each uses one scoped holder shared by its two bridges. Constructing a graph in each bridge would create separate module instances. `GraphAdapter` and `GraphHolder` are project composition patterns built from official Metro APIs, not Metro annotations or framework-managed lifecycle owners.

## Injection, providers and factories

Use `@Inject` for implementation classes whose dependencies can be resolved by the graph. Use `@Binds` to expose such an implementation as its internal interface or public port. Use `@Provides` for SDK objects, platform lookups and configuration that cannot be constructor-injected. [Injection types](https://github.com/ZacSweers/metro/blob/1.4.5/docs/injection-types.md), [providers](https://github.com/ZacSweers/metro/blob/1.4.5/docs/provides.md).

Network demonstrates shared bindings included by platform graphs that need no external input:

```kotlin
@BindingContainer
internal interface NetworkBindings {
    @Binds val KtorNetworkAdapter.bindNetwork: NetworkPort

    companion object {
        @Provides
        @SingleIn(NetworkScope::class)
        fun provideHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
            expectSuccess = false
            followRedirects = true
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            }
        }
    }
}
```

Calling an SDK constructor inside a provider is the provider's job. The network adapter receives `HttpClient` through injection; it does not construct a private client in its constructor. Likewise, favorites supplies DataStore through a provider that calls `PreferenceDataStoreFactory.createWithPath`.

`AndroidNetworkGraph` provides a scoped `HttpClientEngine` with `OkHttp.create()`; `IosNetworkGraph` provides it with `Darwin.create()`. Both include `NetworkBindings`, which builds the client from the injected engine. This explicit platform configuration is supported by Ktor and lets tests replace the engine while exercising the production client provider. [Ktor engine configuration](https://ktor.io/docs/client-engines.html).

`createGraph<T>()` creates graphs without runtime inputs. `createGraphFactory<T.Factory>()` creates the factory for graphs with runtime inputs. These bridge calls are composition entry points, not a separate service locator. A binding container is useful for shared declarations, such as `FavoritesBindings` and `AndroidPlaybackBindings`; it is not required for every provider.

Sound's single module graph declares its `@Binds` aliases and three manifest `@Provides` functions
directly. The catalog, source index and resolver receive their dependencies through injected
constructors. Selecting shipped data belongs to the graph; duplicate-ID validation and building
the cache inventory remain the source index's responsibility. `List<Track>`, `List<Category>` and
`List<SoundSource>` are distinct binding types, so this configuration needs no qualifiers.

Story follows the same pattern with two manifest providers, `List<Story>` and
`List<StorySource>`. Its catalog and resolver use injected constructors, and the resolver owns
source validation and indexing. Neither module needs a separate binding container used by only
one graph.

`@AssistedInject` and `@AssistedFactory` combine graph dependencies with caller-provided values. Playback uses generated factories for callbacks that capture facade or service state, including `PlaybackStore`, `SleepTimerTicker`, controller connections and native engine/session helpers. The caller supplies runtime callbacks; Metro supplies the remaining dependencies. Callback creation and invoking a generated factory are not manual DI gaps. [Assisted injection](https://github.com/ZacSweers/metro/blob/1.4.5/docs/injection-types.md).

## Scopes and resource lifetimes

`@SingleIn` caches a binding within a graph instance. It does not create a process-wide global singleton. The project achieves application-wide sharing by creating one application graph and scoping each capability bridge or shared holder to `AppScope`. [Metro scopes](https://github.com/ZacSweers/metro/blob/1.4.5/docs/scopes.md).

| Resource | Sharing and ownership |
| --- | --- |
| Network engine and `HttpClient` | Cached in `NetworkScope`; used for the application lifetime through one platform bridge |
| Favorites DataStore and IO scope | Cached in `FavoritesScope`; one active store for the production file through the single capability graph |
| Delivery cache store | Shared within `DeliveryScope` by the adapter and background prefetcher |
| Sound source index and playback resolver | Cached in `SoundScope`; catalog and resolver share one validated source index |
| Story catalog and playback resolver | Cached in `StoryScope`; the resolver validates and indexes private stream sources once per graph |
| Delivery background scope | Provided once in `DeliveryScope`; background work uses `SupervisorJob` and `Dispatchers.Default` |
| Android service player and media session | Cached in `PlaybackServiceScope`; explicitly released by the service |
| iOS `AVQueuePlayer` | Unscoped provider; the engine can obtain a fresh player after a media-services reset |
| Tick schedulers | Unscoped on purpose: scheduling replaces the pending tick, so each owner (Android countdown, load timeout, service timer; iOS countdown) gets its own. The iOS scheduler receives the main dispatcher and creates and cancels its own scope |
| Feature ViewModel | Retained and cleared by its Lifecycle `ViewModelStore`, not by `AppScope` |

Metro does not call `HttpClient.close()`, cancel a `CoroutineScope`, or release a native player just because a graph becomes unreachable. Resource cleanup belongs to the component that owns that lifetime. Creating multiple application graphs also creates multiple capability graphs; their local scope markers do not merge them.

This distinction matters for DataStore: AndroidX requires a single active DataStore per file in a process. `@SingleIn(FavoritesScope)` protects one graph; the single application graph establishes the production lifetime. Tests or future shorter-lived graphs must account for existing stores and their scopes. [DataStore usage rules](https://developer.android.com/topic/libraries/architecture/datastore#use-datastore-correctly).

Favorites passes `file::path` to DataStore. Android's `filesDir` lookup and iOS's documents-directory lookup occur on first storage access, rather than during construction of the capability graph. The platform file classes are useful deferred dependencies, not redundant factory layers.

## Coroutine dependencies and qualifiers

Delivery provides its file system, IO dispatcher, background scope and monotonic time source in its local graph. Favorites provides the DataStore IO scope there. Playback supplies its platform scheduler, dispatcher/executor and timer clock. These are ordinary bindings whose configuration and test replacement belong to the module.

These used to be constructor default values, through Metro's optional bindings. They are now explicit providers, so a missing binding fails compilation instead of silently falling back. detekt's `InjectDispatcher` rule treats a hard-coded dispatcher as a missed injection; `config/detekt/detekt.yml` exempts `@Provides` functions, because a provider is where the dispatcher is injected from.

Network uses the caller's suspend context rather than creating an independent background job. ViewModels use Lifecycle's `viewModelScope`. A dispatcher or scope provider is added when there is an actual dependency to configure or replace, not for every suspend function.

Metro's experimental suspend-provider support concerns asynchronous dependency construction. Ordinary suspending work, Flow collection and DataStore operations do not require it. A Metro binding scope is also different from a Kotlin `CoroutineScope`; DI scoping does not perform coroutine lifecycle management. [Official coroutine support and scoping](https://zacsweers.github.io/metro/latest/coroutines/#scoping).

`ApplicationUserAgent` is an internal playback qualifier identifying the nullable user-agent configuration read from Android metadata or iOS Info.plist. Provider and consumer use it within their own module. Network, favorites and delivery currently have one binding for each relevant client, store, dispatcher or coroutine scope, so their module graphs need no extra qualifiers. Separate module graphs prevent unrelated same-typed bindings from competing. [Qualified bindings](https://github.com/ZacSweers/metro/blob/1.4.5/docs/bindings.md).

## Content registration and replaceability

Sound and story contribute resolver bridges to `AppScope` using `@ContributesIntoMap` and `@StringKey` with their stable playback kinds. The application graph aggregates the map; `SessionGraphAdapter` passes it to the session graph. The session implements selection and playback coordination without importing sound or story implementations. [Official map contributions](https://github.com/ZacSweers/metro/blob/1.4.5/docs/aggregation.md#contributesintosetcontributesintomap).

The bridge requests `Map<String, () -> PlaybackItemResolver>`, so Metro supplies providers for the
existing contributions. Constructing or observing the session does not instantiate every resolver.
Only a play request requiring a source lookup invokes the selected kind's provider; resuming an
already-held source does not. Each provider uses the contributing binding's scope. The session
implements no additional resolver cache. The map enters the session graph wrapped in `ContentResolvers`:
as a bare `Map<String, () -> PlaybackItemResolver>` factory input, Metro would treat it as a request
for provider-valued map entries, which only a multibinding can satisfy, and the session graph has
none. A strongly typed wrapper is Metro's guidance for a function type carried as a value.

Sound's resolver bridge is scoped to `AppScope`, and its delegated resolver is scoped to
`SoundScope`. Repeated provider calls reuse the same bridge and resolver within the application
graph. This caches the resolver object, not its results: each lookup still asks delivery for the
current playable URI.

Story's resolver bridge is likewise scoped to `AppScope`, and its resolver is scoped to
`StoryScope`. Provider calls reuse its resolver and source index. Story resolves its own private
HTTPS stream manifest directly; it introduces no dependency on delivery or network.

The resolver contract belongs to its consumer, session. Its `adapterOnlyTypes` and `PlaybackResolverApi` opt-in distinguish content adapters from screen consumers. Sound and story implement that resolver contract while publishing their own catalog ports.

The session bridge's `resolverProvidersByKind = emptyMap()` constructor default is an intentional optional binding: no content contribution is a supported installation. `SessionGraphTest` verifies that an unknown kind becomes `ItemNotFound` without sending a playback command when the graph excludes every resolver contribution. [Optional bindings](https://github.com/ZacSweers/metro/blob/1.4.5/docs/bindings.md#optional-bindings).

Replacing an implementation preserves its port and installs one binding for that port. Metro supports contribution `replaces` and graph `excludes`; local graph tests also use dynamic graphs with replacement binding containers. Removing infrastructure such as network requires replacing the port or removing its consumers. Port isolation enables replacement; it does not make a required dependency optional or provide runtime plugin loading.

## ViewModels and Navigation 3

Feature entries use the Metro set-multibinding pattern from Google's
[Navigation 3 Metro modular recipe](https://github.com/android/nav3-recipes/tree/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/modular/metro).
Each feature's impl module has a public binding container that defines a `@Provides @IntoSet`
installer of type `EntryProviderScope<NavKey>.() -> Unit` and is contributed with
`@ContributesTo(EntryProviderScope::class)`. `AppEntryGraph`, in the composition root (the only
module that sees those impl modules), aggregates that scope and exposes the set as the shell's
`AppEntries` contract; the shell's `AppEntryProvider` runs it. The now-playing bar reaches the host
the same way: `feature:nowplaying:api` declares the `NowPlayingBar` contract, its impl module
contributes a `@Provides` for it to the same scope, and `AppEntries` exposes it, so the shell draws
the bar without its implementation on its classpath.
Route `SerializersModule`s, declared in each feature's api module beside its routes, follow the same [aggregation](https://github.com/ZacSweers/metro/blob/1.4.5/docs/aggregation.md)
into `NavKey::class`, collected by the shell's `RouteSerializersGraph` for `FEATURE_SERIALIZERS`. That graph is
separate because back stacks are restored before the navigator, and so the callbacks, exist, and it
needs only the api modules. The recipe's scope marker lives in a common module; XWAB uses
Navigation 3's own types instead, since every side already sees them. The shell contributes the
feature-owned callback contracts to the same scope from its public `AppEntryCallbacks` container, so
destination mapping stays in the app and features acquire no shared or cross-feature dependency.
The host builds the one `Navigator` over its restored `NavigationState`, with one `ResultEventBus`,
inside its `remember(state)`, and passes the navigator to `AppEntryGraphs`, which creates the entry
graph with it as a factory input; every callback drives that navigator. The navigator sends tab
reselections on the bus, which the host hands to every tab's `rememberResultEventBusNavEntryDecorator`;
Navigation 3 documents hoisting a bus out of composition ([return results](https://developer.android.com/guide/navigation/navigation-3/return-results#hoist)).
The `Navigator` type is public so the graph can take it, but only the shell can construct or call
one, and it is never an
`AppScope` binding; the graph owns no ViewModels, and installer order defines no navigation policy. See
[Metro and Navigation 3](METRO_NAVIGATION3_ARCHITECTURE.md) for the upstream comparison.

MetroX 1.4.5 requires the application to supply a `MetroViewModelFactory` subclass when using `ViewModelGraph`. `AppViewModelFactory` is that adapter: Metro injects the three provider maps assembled through multibindings, and the adapter contains no manual list of feature classes or ViewModel-construction branches. `App` receives it as its one parameter and supplies it through `LocalMetroViewModelFactory`. This is the [MetroX ViewModel setup](https://zacsweers.github.io/metro/1.4.5/metrox-viewmodel/) and its [Compose integration](https://zacsweers.github.io/metro/1.4.5/metrox-viewmodel-compose/).

| ViewModel kind | Registration | Entry lookup |
| --- | --- | --- |
| Plain ViewModel | `@Inject`, `@ViewModelKey`, `@ContributesIntoMap(AppScope::class)` | `metroViewModel()` |
| Route-dependent ViewModel | `@AssistedInject`; nested `@AssistedFactory` implements `ManualViewModelAssistedFactory`, with `@ManualViewModelAssistedFactoryKey` and map contribution | `assistedMetroViewModel<VM, VM.Factory> { create(id) }` |

The current feature registrations use these two patterns:

| Feature module | ViewModel | Construction and lookup | Runtime factory input |
| --- | --- | --- | --- |
| `feature:browse` | `BrowseViewModel` | Plain injection; `metroViewModel()` | None |
| `feature:category` | `CategoryViewModel` | Assisted injection; `assistedMetroViewModel()` | `CategoryId` from the route |
| `feature:favorites` | `FavoritesViewModel` | Plain injection; `metroViewModel()` | None |
| `feature:nowplaying` | `NowPlayingViewModel` | Plain injection; `metroViewModel()` in the bar's `NowPlayingBarRoute` | None |
| `feature:sound` | `SoundViewModel` | Assisted injection; `assistedMetroViewModel()` | `TrackId` from the route |
| `feature:story` | `StoriesViewModel` | Plain injection; `metroViewModel()` | None |
| `feature:story` | `StoryDetailViewModel` | Assisted injection; `assistedMetroViewModel()` | `StoryId` from the route |

Category, sound and story-detail entries supply their typed IDs to generated assisted factories. Despite its name, `ManualViewModelAssistedFactory` is an official MetroX interface; these implementations are generated by Metro. Its role is to let the entry supply runtime arguments explicitly.

Feature-owned use cases receive their capability ports through constructor `@Inject`. The sound, category and favorites use cases consume `SoundPort`, `FavoritesPort` and `PlaybackPort`; the story list and detail use cases consume `StoryPort` and `PlaybackPort`. Browse and now-playing inject their ports directly into their ViewModels. MetroX registration and lookup supply the construction path; the features declare assisted factory interfaces where needed, and Metro generates their implementations.

`FavoritesViewModel` keeps `timeSource: TimeSource = TimeSource.Monotonic` as an intentional [optional binding](https://github.com/ZacSweers/metro/blob/1.4.5/docs/bindings.md#optional-bindings). Metro uses the default when the application graph has no `TimeSource` binding; an installed binding can supply another clock. `FavoritesRemovalTest` passes a `TestTimeSource` through the constructor and advances it to verify the ten-second Undo window without waiting for wall-clock time. This feature clock remains a constructor default, independently of the explicit infrastructure providers described above.

Contributing a ViewModel factory to `AppScope` does not make the ViewModel an application singleton. Provider maps defer construction until a screen asks. Navigation 3's `rememberViewModelStoreNavEntryDecorator` supplies the entry's owner; Lifecycle retains and clears its ViewModels. The now-playing bar uses the root owner because it is application chrome outside destination entries.

Plain MetroX ViewModels do not receive entry creation extras through their no-argument provider. A ViewModel requiring `SavedStateHandle` uses an assisted path: obtain it from the entry's `CreationExtras.createSavedStateHandle()` and pass it to the factory. The navigation composition tests exercise this path; the current production route-dependent ViewModels take route IDs.

## Comparison with official solutions

| Concern | Implementation here | Official mechanism / project decision |
| --- | --- | --- |
| Compile-time assembly | Platform application graphs and internal capability graphs | Metro `@DependencyGraph` and creation intrinsics |
| Interface implementation | Injected adapters bound to ports or internal services | Metro `@Inject` and `@Binds` |
| SDK construction | Local scoped providers for HttpClient, DataStore and player resources | Metro `@Provides`; SDK configuration remains module-owned |
| Common platform wiring | Shared binding containers included by platform graphs | Metro `bindingContainers` |
| Cross-module contributions | Internal bridges contribute public ports | Metro aggregation with generated contribution providers |
| Shared capability state | Module scope plus one bridge/holder per application graph | Metro scoping; application lifetime is a project ownership decision |
| Runtime inputs | Factory parameters and assisted callbacks/route IDs | Metro graph factories and assisted injection |
| Extensible playback kinds | Resolver map contributed by content modules | Metro multibindings; kind selection is session policy |
| ViewModel construction | Provider maps and generated assisted factories | Official MetroX ViewModel and Compose APIs |
| Navigation lifetime | Entry-specific ViewModel owners | Navigation 3 and Lifecycle; Metro constructs the model |
| Port isolation | Internal implementation packages and declared dependencies | Kotlin/Gradle boundaries plus project architecture checks |

Framework-created objects still have framework entry points. Android creates the application and playback service; the service builds its own Metro graph in `onCreate`. Metro does not replace Android's component lifecycle. Platform factory calls, per-operation buffers, response models, callbacks and state transitions are normal implementation work, not dependencies that all need bindings.

## Verification

The following tests exist in the current source tree. Listing them describes coverage, not a claim that every target has just passed:

| Tests | What they exercise |
| --- | --- |
| `NetworkGraphTest` | Unmodified platform graph construction through `productionNetworkGraph()` and adapter identity; no real HTTP request |
| `KtorNetworkAdapterTest` | The production client from `networkGraphWith(engine)`, with only the engine replaced by a `MockEngine`; includes the timeouts the engine receives. One test builds its own client, because following a redirect to cleartext is the only way to reach the adapter's final-URL check |
| `RealEngineInterruptionTest` | The production client on the real OkHttp engine against a local HTTPS server that drops the connection mid-body, over HTTP/2 and HTTP/1.1 (Android host only) |
| `SoundGraphTest` | Production manifest providers, scoped catalog/resolver wiring, and playback/offline availability using the same delivery cache key |
| `StoryGraphTest` | Production manifest providers, scoped catalog/resolver wiring, and contributed provider-map resolution of every published story |
| `SessionGraphTest` | Lazy resolver creation, resuming without creating a resolver, application-map provider aggregation and operation without installed content resolvers |
| `AndroidDeliveryGraphTest`, `IosDeliveryGraphTest`, `DeliveryGraphChecks` | Platform graph wiring, adapter identity and shared cache visibility during prefetch; dynamic graph resource replacements. On Android, the unmodified graph also downloads to a real temporary directory and resolves the file next time |
| `AndroidFavoritesGraphTest`, `IosFavoritesGraphTest` | Lazy file callback and adapter identity; unmodified platform graphs write a favorite to disk and read it back |
| `AndroidPlaybackGraphTest`, `IosPlaybackGraphTest` | Platform graph construction and facade identity on the required main thread; construction off the main thread is refused |
| `ServiceSleepTimerTest` | The service's sleep timer through its injected clock and scheduler: refusal of a past deadline, the fade, cancel and restart |
| `AndroidAppGraphTest`, `IosAppGraphTest` | In `:composition`: production application graph and MetroX map/factory availability |
| `NavigationCompositionTest` | MetroX resolution with entry owners, restoration, saved-state handles and root chrome ownership; typed tab reselection through the host's shared result bus, queued-event cleanup, and delivery to a receiver that stays composed through a cleanup |
| `AppEntryCallbacksTest` | The shell's callback container on a real navigator: each feature intent's destination and id, and detail Back callbacks |
| `AndroidAppIntegrationTest`, `IosAppIntegrationTest` | The real app root on the production graph, only the catalog replaced through a dynamic graph: repeated Browse reselection scrolls the real list to its start, then a category tap reaches the Category screen with its assisted ViewModel |

For features, `AndroidAppGraphTest` and `IosAppGraphTest` call `assertEveryViewModelResolves`. That helper checks factory identity, that `viewModelProviders` and `manualAssistedFactoryProviders` are nonempty, and whether each registered manual assisted factory matches its class key. It does not assert the complete expected set of feature registrations or instantiate every plain ViewModel. `checkArchitecture` checks source annotations and module dependencies; individual ViewModel and use-case tests construct models with fakes to verify behavior. These checks provide complementary coverage, but do not resolve every feature's ViewModel through the real graph.

Graph tests follow three patterns:

- **Dynamic graph replacements replace existing bindings.** They do not only fill missing ones. Delivery's shared check asserts that the download landed in the fake file system, which proves the replacement.
- **Pass a class instance as the replacement container.** An `object` container makes Metro report "Graph input … is unused", because only its functions are needed. A remaining "Context is unused" warning on the favorites device test is expected: the binding that needed the `Context` was replaced.
- **Reach platform graphs from common tests through small `expect` helpers.** Common tests cannot see platform graphs. Network declares `productionNetworkGraph()` and `networkGraphWith(engine)` in `commonTest`, with actuals in `androidHostTest` and `iosTest`. Device tests cannot see `commonTest` at all, so a favorites device test states its own replacement container.

An adapter identity assertion alone does not directly prove SDK-object identity or SDK configuration. Mock-based behavior tests complement graph tests; real platform tests cover paths that need Android or native APIs. Favorites' disk tests verify a write/read within one store lifetime, not persistence after process restart.

Android host tests run on the JVM. A host test cannot construct a `Context`. That is why the Android delivery graph takes a cache directory, while favorites and playback, which need the `Context`, are tested on a device. Favorites, playback and application-graph device tests require Android instrumentation. iOS graph tests run on the simulator. CI runs the iOS simulator tests on pull requests and pushes to `main`, since iOS builds and execution require macOS. Everything Android — architecture checks, host and device tests, detekt, lint and APK builds — runs locally before a PR; the Android workflow, including its device job, starts only on manual dispatch. The device job runs each module's `connectedAndroidTest` in its own Gradle invocation. Under the configuration cache, tasks of different modules run in parallel, and two instrumentation runs at once overloaded the two-core emulator. Consult the current workflows and test reports for execution results.

Two transport limitations remain relevant to interpreting network configuration: Darwin does not support Ktor's 10-second `connectTimeoutMillis`, although the 30-second socket timeout is supported. A response interrupted mid-body is covered on the real OkHttp engine by `RealEngineInterruptionTest`, which also caught HTTP/2 ending such a body without an error; the adapter now compares the streamed bytes with the declared length. Darwin has no equivalent real-engine test. These are transport configuration/coverage issues, not missing Metro bindings. [Ktor timeout support](https://ktor.io/docs/client-timeout.html#limitations), [module coverage note](../core/network/README.md).

Source and documentation references were checked for this document. On a Windows machine where Gradle stops with "Unable to establish loopback connection", set `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=<a short directory>`. The JDK's Unix-domain socket cannot be created under a long temporary path. Local runs cover Android host tests, device tests on an emulator, lint, `staticAnalysis` and `checkArchitecture`; iOS execution is verified by CI.

## Adding or changing a capability

1. Declare its responsibility, allowed dependencies, public interfaces and feature access in `architecture.properties`.
2. Keep its public contract and models in `.port`; keep implementations and DI declarations internal.
3. Use constructor injection and local bindings. Provide external library/platform objects inside the module graph.
4. Pass required ports and platform runtime inputs through a graph factory. Use assisted factories for caller-owned runtime values.
5. Export the capability through a scoped port bridge. Add a shared holder only when multiple bridges must use the same graph.
6. Register a content resolver under its pinned kind when the capability introduces playable content. Keep required shell routing/serialization consistent with that kind.
7. Give features only their permitted port dependencies; register new ViewModels through MetroX instead of editing an application factory list.
8. Test real graph construction and meaningful sharing/lifetime behavior. Replace only what a test must control, and keep one test on the unmodified graph where its real resources can run in the test environment. Run `checkArchitecture` and the applicable host, device or simulator tests. A module that adds device tests also needs its own `connectedAndroidTest` invocation in the CI device job.
9. Update this document when the change alters its graphs, resources or verification tables.

Avoid creating public containers outside `.port` or moving SDK configuration into `.port` to make a cross-module graph compile. The existing local graph pattern preserves both official Metro assembly and the project's implementation boundary.

## Official sources

- [Metro 1.4.5 dependency graphs, factories and binding containers](https://github.com/ZacSweers/metro/blob/1.4.5/docs/dependency-graphs.md)
- [Metro 1.4.5 injection types and assisted injection](https://github.com/ZacSweers/metro/blob/1.4.5/docs/injection-types.md)
- [Metro 1.4.5 providers](https://github.com/ZacSweers/metro/blob/1.4.5/docs/provides.md)
- [Metro 1.4.5 scopes](https://github.com/ZacSweers/metro/blob/1.4.5/docs/scopes.md)
- [Metro 1.4.5 bindings, qualifiers and optional bindings](https://github.com/ZacSweers/metro/blob/1.4.5/docs/bindings.md)
- [Metro 1.4.5 aggregation and contribution providers](https://github.com/ZacSweers/metro/blob/1.4.5/docs/aggregation.md)
- [MetroX ViewModel at 1.4.5](https://zacsweers.github.io/metro/1.4.5/metrox-viewmodel/)
- [MetroX ViewModel Compose at 1.4.5](https://zacsweers.github.io/metro/1.4.5/metrox-viewmodel-compose/)
- [MetroX ViewModel sources at 1.4.5](https://github.com/ZacSweers/metro/tree/1.4.5/metrox-viewmodel)
- [MetroX ViewModel Compose sources at 1.4.5](https://github.com/ZacSweers/metro/tree/1.4.5/metrox-viewmodel-compose)
- [Metro coroutine support and scope ownership](https://zacsweers.github.io/metro/latest/coroutines/)
- [Navigation 3 state and ViewModel owners](https://developer.android.com/guide/navigation/navigation-3/save-state)
- [DataStore KMP setup](https://developer.android.com/kotlin/multiplatform/datastore)
- [Ktor engine construction and configuration](https://ktor.io/docs/client-engines.html)

## Project references

- [Root architecture and rules](../README.md)
- [Library convention](../build-logic/src/main/kotlin/com/xwab/convention/KmpLibraryConventionPlugin.kt)
- [Feature api convention](../build-logic/src/main/kotlin/com/xwab/convention/KmpFeatureApiConventionPlugin.kt)
- [Feature impl convention](../build-logic/src/main/kotlin/com/xwab/convention/KmpFeatureImplConventionPlugin.kt)
- [Architecture task](../build-logic/src/main/kotlin/com/xwab/convention/CheckArchitectureTask.kt)
- [Architecture rule implementations](../build-logic/src/main/kotlin/com/xwab/convention/FeatureFirstRules.kt)
- [Shared application graph surface](../composition/src/commonMain/kotlin/com/xwab/app/di/AppGraph.kt)
- [Application ViewModel factory](../composition/src/commonMain/kotlin/com/xwab/app/di/AppViewModelFactory.kt)
- [Android workflow](../.github/workflows/android.yml)
- [iOS workflow](../.github/workflows/ios.yml)
