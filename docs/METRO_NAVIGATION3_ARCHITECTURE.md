# Metro DI and Navigation 3 architecture — Google recipes reference

October 7, 2026. This document explains the Metro integration in Google's
[Navigation 3 recipes repository](https://github.com/android/nav3-recipes) and maps
it to XWAB's current implementation. The upstream reference is commit
[`f4d1159`](https://github.com/android/nav3-recipes/commit/f4d115959f4f3a1e903e67705954c66aa008adef).
Source links below use that revision so that the examples remain reproducible.

This is a technical explanation of the two Metro recipes and their Android setup,
followed by a project comparison. It is not a verbatim reproduction of the
repository or a catalog of every Navigation 3 recipe. Upstream facts are identified
by their pinned source links; XWAB behavior and recommendations are identified
as project-specific. Lifecycle explanations also use Android Developers guidance
and version-pinned MetroX implementation sources.

Metro is maintained by [Zac Sweers](https://github.com/ZacSweers/metro). Google
provides the Navigation 3 integration recipes. This document complements
[Metro DI architecture](METRO_DI_ARCHITECTURE.md) and
[Navigation 3 architecture](NAVIGATION3_ARCHITECTURE.md); those documents describe
capability graphs, resource ownership and application navigation policies in more detail.

## Reference scope and versions

The upstream repository contains two separate Metro recipes:

| Recipe | Concept |
| --- | --- |
| [Modular navigation][modular] | Features contribute entry installers through Metro set multibindings; the application combines them into one entry provider |
| [Passing arguments to ViewModels][viewmodels] | A navigation key reaches a Metro-created ViewModel through an assisted factory |

These recipes demonstrate individual concepts. The modular recipe's
[README][modular-readme] describes an `app` module, common navigation infrastructure,
and features split into `api` and `impl` submodules. Routes belong in `api`;
composables and installers belong in `impl`. That split permits a feature to
navigate through another feature's public routes without importing its implementation.

The executable example places those illustrative declarations in the same
`metroapp` package, including `// API` and `// IMPL` sections in
`ConversationModule.kt` and `ProfileModule.kt`. The README's proposed module
structure and the example's actual file layout must be distinguished. XWAB's
different module policy is discussed only in the project comparison below.

| Dependency | Upstream reference | XWAB |
| --- | --- | --- |
| Metro compiler plugin and MetroX | `1.0.0` | `1.4.5` |
| Kotlin | `2.3.20` | `2.4.20` |
| Navigation 3 runtime | `1.2.0-SNAPSHOT` | `1.2.0` |
| Navigation 3 UI | AndroidX `1.2.0-SNAPSHOT` | JetBrains Multiplatform `1.2.0-rc01` |
| Lifecycle ViewModel Navigation 3 | AndroidX `2.11.0-beta01` | JetBrains Multiplatform `2.11.0` |

Sources: [upstream version catalog][versions] and
[project version catalog](../gradle/libs.versions.toml). The upstream `main` branch
may use alpha or snapshot dependencies, as stated in its
[README][catalog]. Apply the integration concepts against XWAB's version catalog.

The ViewModel bridge uses `metrox-viewmodel`, `metrox-viewmodel-compose`, and
`lifecycle-viewmodel-navigation3`, alongside Navigation 3 runtime and UI. The
Android sample also uses `metrox-android` for application and Activity injection.
Its [Gradle configuration][build] does not use a separate `metro-navigation3` adapter.

## Android application and Activity setup in the upstream sample

The sample's constructor-injected Activities depend on the complete Android
bootstrap path, not only on the Metro annotations on their constructors:

1. [The application module][build] applies the Android application, Kotlin Compose,
   Kotlin serialization and Metro plugins, and depends on `metrox-android`.
2. [MetroGraph][graph] declares
   `@DependencyGraph(AppScope::class, [ActivityScope::class])` and implements both
   `MetroAppComponentProviders` and `ViewModelGraph`. Its nested
   `@DependencyGraph.Factory` has a parameterless `create()` method.
3. [Nav3MetroApplication][application] implements `MetroApplication`. Its lazy
   `appComponentProviders` creates the graph using
   `createGraphFactory<MetroGraph.Factory>().create()`.
4. [The application manifest][manifest] selects `.Nav3MetroApplication` and declares
   both Metro recipe Activities. MetroX Android `1.0.0` supplies
   `MetroAppComponentFactory` through its [library manifest][metro-android-manifest],
   which participates in manifest merging; the app manifest does not repeat that
   attribute itself.
5. Both recipe Activities use `@Inject`, `@ActivityKey`, and
   `@ContributesIntoMap(ActivityScope::class, binding<Activity>())`. This registers
   their constructor providers in the Android component map. The modular Activity
   requests `Navigator` and `Set<@JvmSuppressWildcards EntryProviderInstaller>`;
   the ViewModel Activity requests `MetroViewModelFactory`.

The launcher `RecipePickerActivity` selects between these recipe Activities.
Its UI and the shared colors, content wrappers and edge-to-edge helpers are demo
presentation support. XWAB uses the same MetroX Activity injection for its one launcher
Activity, with the graph and that Activity in `:shared`; iOS has no equivalent and builds its
graph in `MainViewController`.

## Responsibility boundaries

| Component | Responsibility |
| --- | --- |
| Metro dependency graph | Resolve constructor dependencies and aggregate contributed bindings |
| MetroX ViewModel factory | Select a provider or assisted factory for the requested ViewModel |
| `LocalMetroViewModelFactory` | Make that factory available to Compose ViewModel helpers |
| Feature entry builder | Resolve a route to UI and request its ViewModel inside entry content |
| Application navigator | Change the application's back stack according to application policy |
| Navigation 3 entry decorators | Supply saved-state and ViewModel owners for entry content |
| Lifecycle `ViewModelStore` | Retain ViewModels and clear them when their owner is removed |

Metro contribution scope and ViewModel ownership answer different questions.
`@ContributesIntoMap(AppScope::class)` registers a factory/provider in the graph;
it does not make the resulting ViewModel an application singleton. MetroX's
[Compose helpers at 1.4.5][metro-compose] use Lifecycle's `viewModel()` and the
current `ViewModelStoreOwner`. Navigation 3 supplies the entry owner through its
decorator. The now-playing bar has a different owner because it is composed
outside the navigation entries.

## Recipe 1: collect feature entries with Metro

The [common declarations][common] define `EntryProviderInstaller` as an extension
function on `EntryProviderScope<Any>`. A feature supplies a function that adds its
routes to that builder.

The exact contract is `typealias EntryProviderInstaller = EntryProviderScope<Any>.() -> Unit`.
The sample back stack is `SnapshotStateList<Any>`; its route declarations do not
implement `NavKey`. `ConversationList` and `ConversationDetail(id: Int)` are
`@Serializable`, whereas [Profile][profile] is an unannotated `object`. These are
source facts, not a model of XWAB's serialized `NavKey` contracts.

In [ConversationModule][conversation], `@BindingContainer` holds the provider
declarations. `@ContributesTo(ActivityScope::class)` includes those declarations
in graphs aggregating that scope; `@Provides` and `@IntoSet` contribute one
installer. The provider receives the shared `Navigator`, and the entry callbacks
use it to navigate.

Conversation contributes entries for its list and detail routes. A list selection
navigates to `ConversationDetail`; the detail button navigates to `Profile`.
[ProfileModule][profile] contributes a second installer without needing a
`Navigator` dependency, since its screen has no outgoing navigation callback.
Together they demonstrate combining contributions from two features.

The conversation click and button callbacks use `dropUnlessResumed`, as do the
route-selection buttons in the ViewModel recipe. This is a lifecycle guard for
user actions, separate from DI and entry ownership.

The composition sequence is:

```text
Feature binding containers
  -> Set<EntryProviderInstaller> supplied by Metro
     -> entryProvider invokes every installer
        -> NavDisplay resolves back-stack keys to feature content
```

[MetroModularActivity][activity] receives the navigator and the installer set
through constructor injection. It executes the installers inside `entryProvider`
and passes that provider, the navigator's back stack and its back callback to
`NavDisplay`. Installing the feature entry declarations is separate from creating
ViewModels when entry content is composed.

The [sample graph][graph] aggregates `AppScope` and `ActivityScope`. Its scope
marker names alone do not establish a separate Activity-retained graph or a
navigation-entry owner. [AppModule][app-module] supplies a scoped navigator, while
the common navigator stores a mutable in-memory back stack. The modular recipe
does not implement the process-restoration path used by XWAB's saved tab stacks.

Specifically, `AppModule` starts the navigator at `ConversationList`, `goTo`
appends a destination, and `goBack` calls `removeLastOrNull`. This basic navigator
does not implement XWAB's root protection, tab switching, pop-to-existing or
reselection policies. The modular Activity also does not explicitly install the
ViewModel-store decorator; that installation is shown in the separate ViewModel
recipe.

Set multibindings collect contributions; application navigation must not depend
on their iteration order. Each route should have one entry registration. These are
integration constraints and project guidance, not additional behavior implemented
by the sample navigator. Tab order, start destination and back behavior remain
explicit application policies.

## Recipe 2: pass route arguments to a ViewModel

The [ViewModel recipe][vm-activity] wires assisted creation in this order:

1. Declare the serialized navigation key carrying the destination's arguments.
   The source uses `RouteA` and `RouteB(id: String)`, annotated with `@Serializable`;
   neither implements `NavKey` in this recipe.
2. Mark the ViewModel constructor with `@AssistedInject` and its runtime argument
   with `@Assisted`. The sample's `RouteBViewModel` has only this route argument;
   resolving additional graph dependencies is supported by assisted injection but
   is not demonstrated by that constructor.
3. Declare an `@AssistedFactory` extending `ManualViewModelAssistedFactory`.
   Register it with `@ManualViewModelAssistedFactoryKey` and
   `@ContributesIntoMap(ActivityScope::class)`. The source also annotates the
   factory's `create` parameter with `@Assisted`.
4. Supply the concrete `MetroViewModelFactory` through
   `CompositionLocalProvider(LocalMetroViewModelFactory provides factory)`.
5. Inside the destination's entry content, call
   `assistedMetroViewModel<VM, VM.Factory>` and pass the route argument to the
   generated factory's `create` function.

The factory callback passes the complete `RouteB` key, and `ScreenB` displays
`viewModel.navKey.id`. The Activity owns a back stack created with
`rememberSaveable { mutableStateListOf<Any>(RouteA) }`, appends `RouteB` from its
selection buttons, and removes the last entry on Back. This documents the code
as written; it is not a verified claim of cross-platform or process-death
restoration for that `Any`-typed list. XWAB's registered `NavKey` serialization is
a separate implementation.

The upstream [InjectedViewModelFactory][vm-factory] receives three provider maps:
ordinary ViewModels, `ViewModelAssistedFactory` instances, and
`ManualViewModelAssistedFactory` instances. The exact map contracts are:

| Factory property | Map contract |
| --- | --- |
| `viewModelProviders` | `Map<KClass<out ViewModel>, () -> ViewModel>` |
| `assistedFactoryProviders` | `Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>` |
| `manualAssistedFactoryProviders` | `Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>` |

The upstream concrete factory uses `@Inject` and
`@ContributesBinding(AppScope::class)`; it does not declare `@SingleIn`.
`ViewModelGraph` exposes the factory as `metroViewModelFactory`. XWAB's
[AppViewModelFactory](../shared/src/commonMain/kotlin/com/xwab/app/di/AppViewModelFactory.kt)
implements the same MetroX factory contract. The maps hold providers; resolving
the factory does not eagerly construct every screen ViewModel. XWAB additionally
scopes its concrete factory with `@SingleIn(AppScope::class)`; that is a local
configuration choice.

In MetroX `1.0.0`, [ViewModelGraph][metro-viewmodel-graph] also inherits
`MetroViewModelMultibindings`, whose three maps use `@Multibinds(allowEmpty = true)`.
The sample can therefore provide all three map contracts even though its
demonstrated ViewModel contributes only a manual assisted factory.

An ordinary ViewModel whose inputs all come from the graph uses `@Inject`,
`@ViewModelKey`, `@ContributesIntoMap(AppScope::class)`, and `metroViewModel()` in
XWAB. Route-dependent ViewModels use assisted factories. The sample's manual
assisted API is also present in MetroX `1.4.5`.

### Entry state and lifecycle

The upstream ViewModel recipe places the saveable-state decorator before the
ViewModel-store decorator. XWAB uses the same sequence for each tab:

```kotlin
rememberSaveableStateHolderNavEntryDecorator()
rememberViewModelStoreNavEntryDecorator()
```

The first decorator supplies the entry's saved-state context; the second supplies
its ViewModel owner. A ViewModel is created when entry content requests it and
retained in that store. Removing the entry allows its store to be cleared;
recomposition alone does not recreate the ViewModel. See
[Navigation 3 state and ViewModel owners](https://developer.android.com/guide/navigation/navigation-3/save-state).

The decorator does not serialize arbitrary ViewModel fields or replace back-stack
serialization. Configuration retention, process restoration and DI object sharing
have distinct owners and mechanisms.

For entry-local `SavedStateHandle` data, XWAB's assisted creation callback receives
`CreationExtras`; `createSavedStateHandle()` must use those extras. The plain
provider path cannot inject the current entry's handle by itself. The existing
composition tests exercise this assisted path; production detail ViewModels
currently take typed route IDs.

`SavedStateHandle`, ordinary `metroViewModel()` usage, inactive-tab
retention and the root now-playing owner are project or MetroX explanations.
The Google Metro ViewModel recipe itself demonstrates manual assisted creation
with a route key; it does not demonstrate those additional XWAB flows.

## Mapping to XWAB

| Concern | Current implementation | Relationship to the recipes |
| --- | --- | --- |
| Graph surface | [AppGraph](../shared/src/commonMain/kotlin/com/xwab/app/di/AppGraph.kt) extends `ViewModelGraph`; platform graphs aggregate `AppScope` | Same MetroX ViewModel graph contract |
| Compose factory | [App](../shared/src/commonMain/kotlin/com/xwab/app/App.kt) provides `LocalMetroViewModelFactory` | Same Compose factory boundary |
| Route arguments | [CategoryEntry](../feature/category/src/commonMain/kotlin/com/xwab/app/feature/category/navigation/CategoryEntry.kt) converts the serialized ID to `CategoryId` before assisted creation | Same assisted mechanism; the ViewModel receives a typed capability ID rather than the entire wire-format route |
| Factory registration | [CategoryViewModel](../feature/category/src/commonMain/kotlin/com/xwab/app/feature/category/CategoryViewModel.kt) contributes its internal assisted factory to `AppScope` | Same manual assisted factory contract; project visibility rules remain applicable |
| Entry registration | [AppEntryGraph](../shared/src/commonMain/kotlin/com/xwab/app/composition/AppEntryGraph.kt) aggregates `EntryProviderScope::class` and collects `Set<EntryProviderInstaller>`; [AppEntryProvider](../shared/src/commonMain/kotlin/com/xwab/app/composition/AppEntryProvider.kt) invokes it | Same `@ContributesTo` scope discovery and `@Provides @IntoSet` multibinding; the scope marker is Navigation 3's type rather than a common-module class |
| Route serializers | [RouteSerializersGraph](../shared/src/commonMain/kotlin/com/xwab/app/navigation/RouteSerializersGraph.kt) aggregates `NavKey::class` contributions into `FEATURE_SERIALIZERS` | Not in the recipe, whose back stack is not restored; same discovery mechanism |
| Navigator | [AppNavigationHost](../shared/src/commonMain/kotlin/com/xwab/app/composition/AppNavigationHost.kt) restores the saved stacks and passes them to `AppEntryGraph` as a factory input; the graph builds the one scoped `Navigator` the host and the feature callbacks share | Injected through Metro as in the recipe, but into the shell's graph only and not scoped to the application graph: features receive callbacks, and navigation state remains owned by the composition root |
| Back-stack restoration | [RememberNavigationState](../shared/src/commonMain/kotlin/com/xwab/app/navigation/RememberNavigationState.kt) creates saved tab stacks with the registered feature serializers | Extends the basic recipes for this application's KMP and tab requirements |
| Entry owners | [TabEntries](../shared/src/commonMain/kotlin/com/xwab/app/ui/TabEntries.kt) retains state and ViewModel decorators for every tab | Same entry-owner mechanism, preserved while another tab is selected |

XWAB discovers ViewModel providers, entry installers and route serializers through Metro
aggregation.
Google's [modularization guide](https://developer.android.com/guide/navigation/navigation-3/modularize)
documents both direct entry-builder composition and DI collection.

The installer-set adaptation keeps callback boundaries: each feature's public
`.navigation` binding container consumes its own callback contract, while
`AppEntryGraph` maps its intents to destination routes. A feature never receives
the shared `Navigator` or imports another feature. Each feature provider returns the underlying
`EntryProviderScope<NavKey>.() -> Unit` function type. The readable `EntryProviderInstaller`
alias is internal to shared's composition boundary, so features need no common contract module.
The recipe's `ActivityScope` marker needs one; XWAB's scope markers are
`EntryProviderScope::class` for entries and `NavKey::class` for route serializers, types both
sides already see. [Scope aggregation][metro-aggregation] and
[concrete graph providers][metro-providers] are documented Metro 1.4.5 APIs.
`AppEntryGraph` aggregates the entry installers for one navigation host and receives that host's
restored `NavigationState` as a factory input; it builds one `Navigator` over it, scoped to the
graph, and the host's `remember(state)` keeps that graph, and its installer closures, with those stacks. Unlike the recipe, the navigator is not an application-graph singleton: its
stacks are the saved ones the host restores, and features never receive it. Route serializers have their own graph because saved back stacks are restored before
that host's navigator exists.

This follows the upstream installer-set mechanism and scope discovery with project-specific graph
ownership. The recipe uses `Any`, injects `Navigator` in an Android Activity
scope, and describes physical `api` / `impl` modules. XWAB uses `NavKey`, flat feature
modules and composition-owned saved tab state. Tab order,
the start destination and route wire formats remain explicit and independent of
set iteration. On Android, `MainActivity` is constructor-injected through MetroX as in the
recipe; both platforms then hand the same ViewModel factory to the shared `App` root.

XWAB's flat feature modules and ports-only core boundaries continue to apply.
This reference document does not introduce physical `api` / `impl` splits,
feature-to-feature implementation dependencies, or feature access to core graphs.
Those constraints are defined in the [project architecture](../README.md).

## Verification and maintenance

This document was prepared by reviewing the pinned upstream source and the local
implementation. The local mapping now describes the installer-set adaptation;
build and test results are reported separately for each implementation change.

The source-fidelity audit on October 7, 2026 confirmed that the repository's
`main` HEAD still matched the pinned commit. It reviewed both Metro recipe READMEs,
all their Kotlin files, the application bootstrap, manifest, Gradle setup and
version catalog. Shared demonstration UI was excluded from detailed architecture
coverage. Other Navigation 3 recipes, including deep links, scenes, results,
shared ViewModels, retain, Hilt and Koin, are outside this Metro document's scope.

The explanation distinguishes the following evidence:

| Evidence | Scope |
| --- | --- |
| Pinned `android/nav3-recipes` source | Exact setup, annotations, route types, installers, factories and navigation calls in the two Metro recipes |
| Pinned MetroX sources and Android Developers guidance | Factory internals and lifecycle interpretation |
| Linked local project files | XWAB implementation and its architectural constraints |
| Project adaptation | Installer collection with feature-owned callbacks and one graph-built navigator over the stacks Compose restores; no upstream mandate is claimed |

The existing
[NavigationCompositionTest](../shared/src/composeTest/kotlin/com/xwab/app/navigation/NavigationCompositionTest.kt)
covers MetroX creation through the production navigation display, distinct entry
stores, tab switching, entry cleanup and saved-state restoration. It also checks typed tab
reselection delivery through the graph's shared `ResultEventBus`, discarding a queued event
when navigating away, and that a receiver which stays composed through that clear, including a
list beside a detail pane, keeps receiving. Architecture
checks enforce ViewModel registration and feature serializer installation.
`AppEntryProviderTest` resolves every saveable route through the production Metro
installer set without rendering a screen, detecting a missing feature installer binding or container.
`AppEntryCallbacksTest` checks intent destinations and ID forwarding, and detail Back callbacks.
`AndroidAppIntegrationTest` and `IosAppIntegrationTest` draw the real app root on the production
graph, with only the catalog replaced, verify repeated Browse reselection scrolls the real list to
its start, and follow Browse to Category through the collected entries, the feature callback, the
navigator and the category's assisted ViewModel.
When integration code changes, run the applicable architecture and composition
checks and validate platform behavior. iOS framework and simulator validation
requires a supported macOS environment.

When refreshing this reference, review both upstream Metro recipes, update the
pinned commit and version comparison, and recheck the local mapping. Keep the
distinction between upstream demonstration code and implemented project behavior.

## Source index

- [Google Navigation 3 recipe catalog][catalog]
- [Metro modular navigation recipe][modular]
- [Metro assisted ViewModel recipe][viewmodels]
- [Metro modular recipe README][modular-readme]
- [Metro ViewModel recipe README][viewmodel-readme]
- [Upstream Metro application bootstrap][application]
- [Upstream application manifest][manifest]
- [Upstream Metro application Gradle configuration][build]
- [Upstream version catalog][versions]
- [MetroX ViewModel Compose sources at 1.4.5][metro-compose]
- [MetroX ViewModel graph and empty multibindings at the sample's 1.0.0 version][metro-viewmodel-graph]
- [MetroX ViewModel factory sources at 1.4.5](https://github.com/ZacSweers/metro/blob/1.4.5/metrox-viewmodel/src/commonMain/kotlin/dev/zacsweers/metrox/viewmodel/MetroViewModelFactory.kt)
- [Metro aggregation at 1.4.5][metro-aggregation]
- [Metro graph providers at 1.4.5][metro-providers]

[catalog]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/README.md
[modular]: https://github.com/android/nav3-recipes/tree/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/modular/metro
[viewmodels]: https://github.com/android/nav3-recipes/tree/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/passingarguments/viewmodels/metro
[versions]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/gradle/libs.versions.toml
[build]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/build.gradle.kts
[common]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/modular/metro/CommonModule.kt
[conversation]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/modular/metro/ConversationModule.kt
[activity]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/modular/metro/MetroModularActivity.kt
[graph]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/modular/metro/MetroGraph.kt
[app-module]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/modular/metro/AppModule.kt
[vm-activity]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/passingarguments/viewmodels/metro/MetroViewModelsActivity.kt
[vm-factory]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/passingarguments/viewmodels/metro/InjectedViewModelFactory.kt
[metro-compose]: https://github.com/ZacSweers/metro/blob/1.4.5/metrox-viewmodel-compose/src/commonMain/kotlin/dev/zacsweers/metrox/viewmodel/MetroViewModel.kt
[modular-readme]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/modular/metro/README.md
[viewmodel-readme]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/passingarguments/viewmodels/metro/README.md
[profile]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/modular/metro/ProfileModule.kt
[application]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/java/com/example/nav3recipes/Nav3MetroApplication.kt
[manifest]: https://github.com/android/nav3-recipes/blob/f4d115959f4f3a1e903e67705954c66aa008adef/metroapp/src/main/AndroidManifest.xml
[metro-android-manifest]: https://github.com/ZacSweers/metro/blob/1.0.0/metrox-android/src/main/AndroidManifest.xml
[metro-viewmodel-graph]: https://github.com/ZacSweers/metro/blob/1.0.0/metrox-viewmodel/src/commonMain/kotlin/dev/zacsweers/metrox/viewmodel/ViewModelGraph.kt
[metro-aggregation]: https://github.com/ZacSweers/metro/blob/1.4.5/docs/aggregation.md
[metro-providers]: https://github.com/ZacSweers/metro/blob/1.4.5/docs/provides.md
