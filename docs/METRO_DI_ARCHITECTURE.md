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
| `xwab.kmp.feature` | Configure feature contributions, including the diagnostic policy for internal assisted ViewModel factories |
| Android / iOS application graph | Aggregate installed `AppScope` contributions and expose the MetroX ViewModel integration |
| Internal capability graph | Construct and connect one core module's implementation, SDK objects and internal services |
| `*GraphAdapter` | Contribute a capability port to the application graph and pass external inputs into the local graph |
| `*GraphHolder` | Build one local graph shared by multiple bridges; used by sound and story |
| `@BindingContainer` | Share binding declarations between graphs without making the container a complete graph |
| Feature ViewModel factory | Combine injected dependencies with runtime route arguments through MetroX |
| Resource owner | Cancel jobs, release players and sessions, and close resources when its lifetime ends |
| `checkArchitecture` | Enforce project dependency, visibility, port and ViewModel-registration rules |

Metro constructs the dependency graph at compile time. It does not choose a capability's responsibility, enforce every architectural boundary by itself, or automatically dispose of resources. [Official graph APIs](https://github.com/ZacSweers/metro/blob/1.4.5/docs/dependency-graphs.md).

## Project rules

The rules are written in the root README, each core module's `architecture.properties`, and the checks in `build-logic`. This document explains their DI consequences; it does not introduce exceptions to them.

- Every production declaration exposed by a core module belongs to its exact `.port` package. Production declarations outside it remain `internal` or `private`.
- Core modules reference other core modules only through those modules' ports. Port contracts cannot refer to implementation packages, including their own.
- Each core module declares its responsibility, allowed project dependencies, feature accessibility and public callable interfaces in `architecture.properties`.
- Features consume only capabilities permitted by those contracts. Infrastructure modules such as network, delivery and playback are not directly feature-accessible.
- Core and feature modules stay flat. Repository/provider abstraction layers, Koin, and physical `api` / `impl` module splits are not part of this architecture.
- Feature implementation types remain internal. Navigation contracts and shell UI form the feature's public composition surface.

Kotlin visibility and Gradle dependency declarations establish the boundaries; the architecture task checks them. Metro supplies objects within those boundaries. A compiling graph alone does not prove that a module obeys its responsibility.

## Application graph and contribution discovery

`AppGraph` extends MetroX's `ViewModelGraph`. `AndroidAppGraph` and `IosAppGraph` declare `@DependencyGraph(AppScope::class)` and implement that shared surface.

Android provides its application `Context` through `AndroidAppGraph.Factory`, using `@Provides @GraphPrivate`. `MainApplication` creates the application graph once. iOS provides no factory input: `MainViewController.kt` holds one lazy application graph reused by its controllers. Platform capability graphs derive their own native values.

`shared/build.gradle.kts` includes all core modules from the module list discovered by settings. Feature dependencies remain explicit. Metro discovers contributions from the compilation classpath: annotating a class in an uninstalled module does not install that module. Architecture checks enforce module registration and dependency direction.

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
| `core:session` | `SessionGraph`, `SessionScope` | `PlaybackEnginePort` and the contributed resolver map | `PlaybackPort` |
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

Sound and story bind their catalog adapters in small binding containers. Their catalogs, source
lists and resolvers have `@Inject` secondary constructors that read the manifests shipped inside
the module; the primary constructors take the data directly, which is what tests use. The catalogs
are scoped to their module; the source lists and resolvers are not.

`@AssistedInject` and `@AssistedFactory` combine graph dependencies with caller-provided values. Playback uses generated factories for callbacks that capture facade or service state, including `PlaybackStore`, `SleepTimerTicker`, controller connections and native engine/session helpers. The caller supplies runtime callbacks; Metro supplies the remaining dependencies. Callback creation and invoking a generated factory are not manual DI gaps. [Assisted injection](https://github.com/ZacSweers/metro/blob/1.4.5/docs/injection-types.md).

## Scopes and resource lifetimes

`@SingleIn` caches a binding within a graph instance. It does not create a process-wide global singleton. The project achieves application-wide sharing by creating one application graph and scoping each capability bridge or shared holder to `AppScope`. [Metro scopes](https://github.com/ZacSweers/metro/blob/1.4.5/docs/scopes.md).

| Resource | Sharing and ownership |
| --- | --- |
| Network engine and `HttpClient` | Cached in `NetworkScope`; used for the application lifetime through one platform bridge |
| Favorites DataStore and IO scope | Cached in `FavoritesScope`; one active store for the production file through the single capability graph |
| Delivery cache store | Shared within `DeliveryScope` by the adapter and background prefetcher |
| Sound and story catalogs | Cached in their module scope; each module's `GraphHolder` builds one graph that its catalog bridge and resolver bridge share |
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

The bridge requests `Map<String, PlaybackItemResolver>`, so the application graph builds every
contributed resolver when it builds the session. Each resolver bridge delegates to its module
graph's resolver, and the session implements no resolver cache. Story resolves its own private
HTTPS stream manifest directly; it introduces no dependency on delivery or network.

The resolver contract belongs to its consumer, session. Its `adapterOnlyTypes` and `PlaybackResolverApi` opt-in distinguish content adapters from screen consumers. Sound and story implement that resolver contract while publishing their own catalog ports.

The session bridge's `resolversByKind = emptyMap()` constructor default is an intentional optional binding: no content contribution is a supported installation. `EmptyContentSessionGraphTest` verifies that an unknown kind becomes `ItemNotFound` without sending a playback command. [Optional bindings](https://github.com/ZacSweers/metro/blob/1.4.5/docs/bindings.md#optional-bindings).

Replacing an implementation preserves its port and installs one binding for that port. Metro supports contribution `replaces` and graph `excludes`; local graph tests also use dynamic graphs with replacement binding containers. Removing infrastructure such as network requires replacing the port or removing its consumers. Port isolation enables replacement; it does not make a required dependency optional or provide runtime plugin loading.

## ViewModels and Navigation 3

`AppViewModelFactory` extends the official `MetroViewModelFactory` and receives the three provider maps exposed by `ViewModelGraph`. It contains no manual list of feature classes. `App` supplies it through `LocalMetroViewModelFactory`. This is the [MetroX ViewModel setup](https://zacsweers.github.io/metro/latest/metrox-viewmodel/) and its [Compose integration](https://zacsweers.github.io/metro/latest/metrox-viewmodel-compose/).

| ViewModel kind | Registration | Entry lookup |
| --- | --- | --- |
| Plain ViewModel | `@Inject`, `@ViewModelKey`, `@ContributesIntoMap(AppScope::class)` | `metroViewModel()` |
| Route-dependent ViewModel | `@AssistedInject`; nested `@AssistedFactory` implements `ManualViewModelAssistedFactory`, with `@ManualViewModelAssistedFactoryKey` and map contribution | `assistedMetroViewModel<VM, VM.Factory> { create(id) }` |

Category, sound and story-detail entries supply their typed IDs to generated assisted factories. Despite its name, `ManualViewModelAssistedFactory` is an official MetroX interface; these implementations are generated by Metro. Its role is to let the entry supply runtime arguments explicitly.

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
| `SoundGraphTest`, `StoryGraphTest` | Module graph construction and catalog/resolver wiring |
| `SessionGraphTest`, `EmptyContentSessionGraphTest` | Session assembly and operation without installed content resolvers |
| `AndroidDeliveryGraphTest`, `IosDeliveryGraphTest`, `DeliveryGraphChecks` | Platform graph wiring, adapter identity and shared cache visibility during prefetch; dynamic graph resource replacements. On Android, the unmodified graph also downloads to a real temporary directory and resolves the file next time |
| `AndroidFavoritesGraphTest`, `IosFavoritesGraphTest` | Lazy file callback and adapter identity; unmodified platform graphs write a favorite to disk and read it back |
| `AndroidPlaybackGraphTest`, `IosPlaybackGraphTest` | Platform graph construction and facade identity on the required main thread; construction off the main thread is refused |
| `ServiceSleepTimerTest` | The service's sleep timer through its injected clock and scheduler: refusal of a past deadline, the fade, cancel and restart |
| `AndroidAppGraphTest`, `IosAppGraphTest` | Production application graph and MetroX map/factory availability |
| `NavigationCompositionTest` | MetroX resolution with entry owners, restoration, saved-state handles and root chrome ownership |

Graph tests follow three patterns:

- **Dynamic graph replacements replace existing bindings.** They do not only fill missing ones. Delivery's shared check asserts that the download landed in the fake file system, which proves the replacement.
- **Pass a class instance as the replacement container.** An `object` container makes Metro report "Graph input … is unused", because only its functions are needed. A remaining "Context is unused" warning on the favorites device test is expected: the binding that needed the `Context` was replaced.
- **Reach platform graphs from common tests through small `expect` helpers.** Common tests cannot see platform graphs. Network declares `productionNetworkGraph()` and `networkGraphWith(engine)` in `commonTest`, with actuals in `androidHostTest` and `iosTest`. Device tests cannot see `commonTest` at all, so a favorites device test states its own replacement container.

An adapter identity assertion alone does not directly prove SDK-object identity or SDK configuration. Mock-based behavior tests complement graph tests; real platform tests cover paths that need Android or native APIs. Favorites' disk tests verify a write/read within one store lifetime, not persistence after process restart.

Android host tests run on the JVM. A host test cannot construct a `Context`. That is why the Android delivery graph takes a cache directory, while favorites and playback, which need the `Context`, are tested on a device. Favorites, playback and application-graph device tests require Android instrumentation. iOS graph tests run on the simulator. CI includes architecture checks, Android host/device tests and iOS simulator tests; iOS builds and execution require macOS. The device job runs each module's `connectedAndroidTest` in its own Gradle invocation. Under the configuration cache, tasks of different modules run in parallel, and two instrumentation runs at once overloaded the two-core emulator. Consult the current workflows and test reports for execution results.

Two transport limitations remain relevant to interpreting network configuration: Darwin does not support Ktor's 10-second `connectTimeoutMillis`, although the 30-second socket timeout is supported; network also lacks a real-engine test for a response interrupted mid-body. These are transport configuration/coverage issues, not missing Metro bindings. [Ktor timeout support](https://ktor.io/docs/client-timeout.html#limitations), [module coverage note](../core/network/README.md).

Source and documentation references were checked for this document. On a Windows machine where Gradle stops with "Unable to establish loopback connection", set `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=<a short directory>`. The JDK's Unix-domain socket cannot be created under a long temporary path. Local runs cover Android host tests, device-test compilation, lint, `staticAnalysis` and `checkArchitecture`; iOS and device execution are verified by CI.

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
- [MetroX ViewModel](https://zacsweers.github.io/metro/latest/metrox-viewmodel/)
- [MetroX ViewModel Compose](https://zacsweers.github.io/metro/latest/metrox-viewmodel-compose/)
- [MetroX ViewModel sources at 1.4.5](https://github.com/ZacSweers/metro/tree/1.4.5/metrox-viewmodel)
- [MetroX ViewModel Compose sources at 1.4.5](https://github.com/ZacSweers/metro/tree/1.4.5/metrox-viewmodel-compose)
- [Metro coroutine support and scope ownership](https://zacsweers.github.io/metro/latest/coroutines/)
- [Navigation 3 state and ViewModel owners](https://developer.android.com/guide/navigation/navigation-3/save-state)
- [DataStore KMP setup](https://developer.android.com/kotlin/multiplatform/datastore)
- [Ktor engine construction and configuration](https://ktor.io/docs/client-engines.html)

## Project references

- [Root architecture and rules](../README.md)
- [Library convention](../build-logic/src/main/kotlin/com/xwab/convention/KmpLibraryConventionPlugin.kt)
- [Feature convention](../build-logic/src/main/kotlin/com/xwab/convention/KmpFeatureConventionPlugin.kt)
- [Architecture task](../build-logic/src/main/kotlin/com/xwab/convention/CheckArchitectureTask.kt)
- [Architecture rule implementations](../build-logic/src/main/kotlin/com/xwab/convention/FeatureFirstRules.kt)
- [Shared application graph surface](../shared/src/commonMain/kotlin/com/xwab/app/di/AppGraph.kt)
- [Application ViewModel factory](../shared/src/commonMain/kotlin/com/xwab/app/di/AppViewModelFactory.kt)
- [Android workflow](../.github/workflows/android.yml)
- [iOS workflow](../.github/workflows/ios.yml)
