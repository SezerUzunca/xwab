package com.xwab.convention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Every architecture rule is driven once from its accepting and rejecting side. */
class FeatureFirstRulesTest {

    @Test
    fun coreDependenciesPointOutwardNeverTowardFeatures() {
        val violations = dependencyViolations(
            mapOf(":core:sound" to listOf(":feature:category")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("may not depend on a feature"))
        assertEquals(
            emptyList(),
            dependencyViolations(
                mapOf(":feature:category" to listOf(":core:sound")),
            ),
        )
    }

    @Test
    fun featuresNeverDependOnOtherFeatures() {
        val violations = dependencyViolations(
            mapOf(":feature:category:impl" to listOf(":feature:sound:api")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("connect destination intents in :shared"))
        assertEquals(
            emptyList(),
            dependencyViolations(
                mapOf(
                    ":feature:category:impl" to listOf(":feature:category:api"),
                    ":composition" to listOf(
                        ":feature:category:api", ":feature:category:impl",
                        ":feature:sound:api", ":feature:sound:impl",
                    ),
                ),
            ),
        )
    }

    @Test
    fun onlyTheCompositionRootInstallsAFeatureImplementation() {
        val violations = dependencyViolations(
            mapOf(
                ":feature:sound:api" to listOf(":feature:sound:impl"),
                ":testing:sound" to listOf(":feature:sound:impl"),
                ":androidApp" to listOf(":feature:sound:impl"),
                // The shell compiles against contracts; it never installs what is behind them.
                ":shared" to listOf(":feature:sound:impl"),
            ),
        )

        assertEquals(4, violations.size)
        assertTrue(violations.all { it.contains("Only :composition installs an implementation") })
        assertEquals(
            emptyList(),
            dependencyViolations(mapOf(":testing:sound" to listOf(":feature:sound:api"))),
        )
    }

    @Test
    fun everyFeatureIsAnApiAndAnImplModule() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.featureModuleShapeViolations(
                setOf(
                    ":feature", ":core:sound",
                    ":feature:browse:api", ":feature:browse:impl",
                    ":feature:sleep-timer:api", ":feature:sleep-timer:impl",
                ),
            ),
        )

        val misshapen = FeatureFirstRules.featureModuleShapeViolations(
            setOf(":feature:browse", ":feature:story:ui", ":feature:story:api", ":feature:story:impl"),
        )
        assertEquals(2, misshapen.size)
        assertTrue(misshapen.all { it.contains("is not a feature api or impl module") })

        val unpaired = FeatureFirstRules.featureModuleShapeViolations(
            setOf(":feature:browse:api", ":feature:sound:impl"),
        )
        assertEquals(
            listOf(
                ":feature:browse has no impl module. A feature is its api and impl modules together.",
                ":feature:sound has no api module. A feature is its api and impl modules together.",
            ),
            unpaired,
        )
    }

    @Test
    fun onlyACapabilityOrFeatureRootSplitsIntoApiAndImpl() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.legacySplitDirectoryViolations(
                listOf(
                    "feature/browse/api",
                    "feature/browse/impl",
                    "core/network/api",
                    "core/network/impl",
                    "feature/browse/impl/src/commonMain/kotlin/com/xwab/app/feature/browse/navigation",
                ),
            ),
        )

        val violations = FeatureFirstRules.legacySplitDirectoryViolations(
            listOf(
                "core/network/impl/src/commonMain/kotlin/com/xwab/app/core/network/impl",
                "core/network/impl/api",
                "feature/browse/impl/src/commonMain/kotlin/com/xwab/app/feature/browse/api",
                "feature/browse/impl/api",
            ),
        )

        assertEquals(4, violations.size)
        assertTrue(violations.all { it.contains("api/impl split") })
    }

    @Test
    fun koinCannotReturnAlongsideMetro() {
        val violations = FeatureFirstRules.koinUsageViolations(
            mapOf(
                "feature/browse/Screen.kt" to "import org.koin.compose.koinInject",
                "feature/browse/build.gradle.kts" to
                    "implementation(\"io.insert-koin:koin-core:4.0.0\")",
                "feature/story/Screen.kt" to "import dev.zacsweers.metro.Inject",
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("Use Metro") })
    }

    @Test
    fun featuresCannotDeclareOrReachAdapterModules() {
        (
            corePolicies.filterValues { !it.featureAccessible }.keys +
                FeatureFirstRules.SHELL_MODULE + FeatureFirstRules.COMPOSITION_ROOT
            ).forEach { offLimits ->
            val violations = dependencyViolations(
                mapOf(":feature:sound" to listOf(offLimits)),
            )
            assertEquals(1, violations.size, offLimits)
        }

        val transitive = dependencyViolations(
            graph = mapOf(":feature:sound" to listOf(":testing")),
            apiEdges = mapOf(":testing" to listOf(":core:delivery")),
        )
        assertEquals(1, transitive.size)
        assertTrue(transitive.single().contains("through :testing"))

        val engineBoundary = dependencyViolations(
            graph = mapOf(":feature:story" to listOf(":testing")),
            apiEdges = mapOf(":testing" to listOf(":core:playback")),
        )
        assertEquals(1, engineBoundary.size)
        assertTrue(engineBoundary.single().contains("adapter-only capability"))

        // The adapter boundary is about features. A core module may declare these: `:core:sound`
        // reaches delivery and the session contribution contract to answer for its own content.
        assertEquals(
            emptyList(),
            dependencyViolations(
                mapOf(":core:sound" to listOf(":core:delivery", ":core:session")),
            ),
        )
    }

    @Test
    fun onlyApiConfigurationsReExportProjectDependencies() {
        listOf("api", "commonMainApi", "androidMainApi", "iosMainApi").forEach {
            assertTrue(FeatureFirstRules.isApiConfiguration(it), it)
        }
        listOf("implementation", "commonMainImplementation", "apiElements").forEach {
            assertEquals(false, FeatureFirstRules.isApiConfiguration(it), it)
        }
    }

    @Test
    fun downloadAndroidAndIosUserAgentsAreAllowedWhileTheyAgree() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.userAgentAgreementViolations(
                mapOf(
                    "core/sound/SoundSourceManifest.kt" to
                        """private const val WIKIMEDIA_USER_AGENT = "Sleep/1.0 (https://example.test)"""",
                    "androidApp/src/main/AndroidManifest.xml" to
                        """
                        <meta-data
                            android:name="com.xwab.app.core.playback.USER_AGENT"
                            android:value="Sleep/1.0 (https://example.test)" />
                        """.trimIndent(),
                    "iosApp/iosApp/Info.plist" to
                        """
                        <key>com.xwab.app.core.playback.USER_AGENT</key>
                        <string>Sleep/1.0 (https://example.test)</string>
                        """.trimIndent(),
                ),
            ),
        )
    }

    @Test
    fun aDifferentIosPlaybackIdentityFailsTheSameRule() {
        val violations = FeatureFirstRules.userAgentAgreementViolations(
            mapOf(
                "core/sound/SoundSourceManifest.kt" to
                    """private const val WIKIMEDIA_USER_AGENT = "Sleep/1.0"""",
                "iosApp/iosApp/Info.plist" to
                    """<key>com.xwab.app.core.playback.USER_AGENT</key><string>Sleep/2.0</string>""",
            ),
        )
        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("iosApp/iosApp/Info.plist"))
    }

    @Test
    fun iosMetadataCommentsAndUnrelatedKeysAreNotIdentities() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.userAgentAgreementViolations(
                mapOf(
                    "core/sound/SoundSourceManifest.kt" to
                        """private const val WIKIMEDIA_USER_AGENT = "Sleep/1.0"""",
                    "iosApp/iosApp/Info.plist" to
                        """
                        <!-- <key>x.USER_AGENT</key><string>Sleep/old</string> -->
                        <key>CFBundleName</key><string>Sleep/other</string>
                        <key>x.USER_AGENT</key>
                        <string>Sleep/1.0</string>
                        """.trimIndent(),
                ),
            ),
        )
    }

    @Test
    fun aUserAgentThatDriftsBetweenThePlaybackAndDownloadPathsFails() {
        val violations = FeatureFirstRules.userAgentAgreementViolations(
            mapOf(
                "core/sound/SoundSourceManifest.kt" to
                    """private const val WIKIMEDIA_USER_AGENT = "Sleep/2.0 (https://example.test)"""",
                "androidApp/src/main/AndroidManifest.xml" to
                    """<meta-data android:name="x.USER_AGENT" android:value="Sleep/1.0" />""",
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("Sleep/2.0 (https://example.test)"))
        assertTrue(violations.single().contains("Sleep/1.0"))
    }

    /**
     * The three things the first version of this rule reported and should not have: its own test
     * fixtures, and the constant naming the manifest entry a user agent is *read from*.
     */
    @Test
    fun theUserAgentCheckIgnoresTestDataAndTheKeyItIsStoredUnder() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.userAgentAgreementViolations(
                mapOf(
                    "core/sound/src/commonMain/kotlin/SoundSourceManifest.kt" to
                        """private const val WIKIMEDIA_USER_AGENT = "Sleep/1.0"""",
                    "core/playback/src/androidMain/kotlin/PlaybackService.kt" to
                        """private const val USER_AGENT_METADATA_KEY = "com.example.USER_AGENT"""",
                    "build-logic/src/test/kotlin/FeatureFirstRulesTest.kt" to
                        """private const val WIKIMEDIA_USER_AGENT = "Sleep/9.9"""",
                    "feature/sound/src/commonTest/kotlin/SoundScreenTest.kt" to
                        """private const val USER_AGENT = "Sleep/8.8"""",
                ),
            ),
        )
    }

    /** A comment is not a declaration, and neither is metadata about something else. */
    @Test
    fun theUserAgentCheckReadsDeclarationsAndNotProseOrOtherMetadata() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.userAgentAgreementViolations(
                mapOf(
                    "core/sound/SoundSourceManifest.kt" to
                        """
                        // const val OLD_USER_AGENT = "Sleep/0.1"
                        private const val WIKIMEDIA_USER_AGENT = "Sleep/1.0"
                        """.trimIndent(),
                    "androidApp/src/main/AndroidManifest.xml" to
                        """
                        <meta-data android:name="com.other.THING" android:value="Sleep/9.9" />
                        <meta-data android:name="x.USER_AGENT" android:value="Sleep/1.0" />
                        """.trimIndent(),
                ),
            ),
        )
    }

    @Test
    fun sharedUsesFeatureContractsOnlyAtTheirApplicationBoundaries() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.sharedFeatureReferenceViolations(
                mapOf(
                    "shared/src/commonMain/kotlin/TopLevelDestinations.kt" to sharedSource(
                        "navigation",
                        "import com.xwab.app.feature.browse.navigation.BrowseRoute",
                    ),
                    "shared/src/commonMain/kotlin/AppEntryCallbacks.kt" to sharedSource(
                        "composition",
                        "import com.xwab.app.feature.browse.navigation.BrowseEntryCallbacks as Callbacks",
                    ),
                    "shared/src/commonMain/kotlin/AppEntries.kt" to sharedSource(
                        "composition",
                        "import com.xwab.app.feature.nowplaying.shell.NowPlayingBar",
                    ),
                ),
            ),
        )
    }

    @Test
    fun sharedFeatureReferencesFailOutsideTheBoundaryInEveryPackageAndPlatform() {
        val violations = FeatureFirstRules.sharedFeatureReferenceViolations(
            mapOf(
                "shared/src/commonMain/kotlin/Ui.kt" to sharedSource(
                    "ui",
                    "import com.xwab.app.feature.browse.navigation.BrowseRoute",
                ),
                "shared/src/androidMain/kotlin/Platform.kt" to sharedSource(
                    "ui",
                    "internal val route = com.xwab.app.feature.browse.navigation.BrowseRoute",
                ),
                "shared/src/iosMain/kotlin/Root.kt" to sharedSource(
                    "di",
                    "import com.xwab.app.feature.browse.navigation.BrowseRoute as Start",
                ),
                "shared/src/commonMain/kotlin/Wildcard.kt" to sharedSource(
                    "ui",
                    "import com.xwab.app.feature.browse.navigation.*",
                ),
                // An api contract is still out of bounds outside navigation/composition.
                "shared/src/commonMain/kotlin/App.kt" to sharedSource(
                    "ui",
                    "import com.xwab.app.feature.nowplaying.shell.NowPlayingBar",
                ),
            ),
        )
        assertEquals(5, violations.size)
        assertTrue(violations.any { it.contains("androidMain") })
        assertTrue(violations.any { it.contains("iosMain") })
        assertTrue(violations.all { it.contains("other shared packages may not reference features") })
    }

    @Test
    fun sharedReferenceChecksIgnoreCommentsAndStringsButKeepTrailingCommentImports() {
        val allowed = sharedSource(
            "ui",
            "// import com.xwab.app.feature.browse.navigation.BrowseRoute\n" +
                "/*\nimport com.xwab.app.feature.browse.navigation.BrowseRoute\n*/\n" +
                "internal val docs = \"com.xwab.app.feature.browse.navigation.BrowseRoute\"\n" +
                "internal val example = \"\"\"\n" +
                "import com.xwab.app.feature.browse.navigation.BrowseRoute\n\"\"\"",
        )
        assertEquals(
            emptyList(),
            FeatureFirstRules.sharedFeatureReferenceViolations(mapOf("Ui.kt" to allowed)),
        )

        val forbidden = sharedSource(
            "ui",
            "import com.xwab.app.feature.browse.navigation.BrowseRoute // outside the boundary",
        )
        assertEquals(
            1,
            FeatureFirstRules.sharedFeatureReferenceViolations(mapOf("Ui.kt" to forbidden)).size,
        )
    }

    @Test
    fun aFeatureImplementationExposesOnlyItsContributedContainers() {
        val sources = mapOf(
            // The api module is the feature's public surface.
            "feature/browse/api/src/commonMain/kotlin/Navigation.kt" to featureSource(
                ".navigation",
                """
                    data object BrowseRoute : NavKey
                    class BrowseEntryCallbacks(val onBack: () -> Unit)
                    interface BrowseBar
                """.trimIndent(),
            ),
            "feature/browse/impl/src/commonMain/kotlin/Entry.kt" to featureSource(
                ".navigation",
                """
                    @ContributesTo(EntryProviderScope::class)
                    @BindingContainer
                    object BrowseEntryBindings
                """.trimIndent(),
            ),
            "feature/browse/impl/src/commonMain/kotlin/Bar.kt" to featureSource(
                ".shell",
                "@ContributesTo(EntryProviderScope::class) object BrowseBarBindings\nprivate object ViewModelBrowseBar",
            ),
            "feature/browse/impl/src/commonMain/kotlin/Screen.kt" to featureSource(
                "",
                """
                    internal class BrowseViewModel
                    internal data class BrowseState(val loading: Boolean)
                    internal fun BrowseScreen() = Unit
                    private fun Preview() = Unit
                """.trimIndent(),
            ),
        )
        assertEquals(emptyList(), FeatureFirstRules.featureVisibilityViolations(sources))

        val leaks = mapOf(
            "feature/browse/impl/src/commonMain/kotlin/ViewModel.kt" to
                featureSource("", "public class BrowseViewModel"),
            "feature/browse/impl/src/androidMain/kotlin/Screen.kt" to
                featureSource("", "@Composable fun BrowseScreen() = Unit"),
            "feature/browse/impl/src/commonMain/kotlin/State.kt" to
                featureSource("", "data class BrowseState(val loading: Boolean)"),
            "feature/browse/impl/src/commonMain/kotlin/UseCase.kt" to
                featureSource(".domain", "class BrowseUseCase"),
            // Routes and callbacks belong in the api module, even in the navigation package.
            "feature/browse/impl/src/commonMain/kotlin/Route.kt" to
                featureSource(".navigation", "data object BrowseRoute : NavKey"),
            "feature/browse/impl/src/commonMain/kotlin/Bar.kt" to
                featureSource(".shell", "@Composable fun BrowseBar() = Unit"),
            // Public but not contributed: nothing collects it, so nothing needs to see it.
            "feature/browse/impl/src/commonMain/kotlin/Bindings.kt" to
                featureSource(".navigation", "@BindingContainer\nobject BrowseBindings"),
            // A ViewModel contributes itself to the graph; no public dependency bag is needed.
            "feature/browse/impl/src/commonMain/kotlin/Dependencies.kt" to
                featureSource(".di", "class BrowseDependencies(internal val catalog: SoundPort)"),
        )
        val violations = FeatureFirstRules.featureVisibilityViolations(leaks)
        assertEquals(8, violations.size)
        assertTrue(violations.all { it.contains("from a feature implementation") })
    }

    @Test
    fun everyFeatureViewModelIsRegisteredInTheAppGraph() {
        val registered = mapOf(
            "feature/browse/src/commonMain/kotlin/BrowseViewModel.kt" to featureSource(
                "",
                """
                    internal data class BrowseState(val loading: Boolean)

                    /** A { brace } in a comment is not the class body. */
                    @Inject
                    @ViewModelKey
                    @ContributesIntoMap(AppScope::class)
                    internal class BrowseViewModel(
                        port: SoundPort,
                    ) : ViewModel() {
                        val title = "}"
                    }
                """.trimIndent(),
            ),
            "feature/browse/src/commonMain/kotlin/DetailViewModel.kt" to featureSource(
                "",
                """
                    @AssistedInject
                    internal class DetailViewModel(@Assisted id: String) : ViewModel() {
                        @AssistedFactory
                        @ManualViewModelAssistedFactoryKey
                        @ContributesIntoMap(AppScope::class)
                        fun interface Factory : ManualViewModelAssistedFactory {
                            fun create(id: String): DetailViewModel
                        }
                    }
                """.trimIndent(),
            ),
        )
        assertEquals(emptyList(), FeatureFirstRules.unregisteredViewModelViolations(registered))

        val unregistered = mapOf(
            "feature/browse/src/commonMain/kotlin/Plain.kt" to featureSource(
                "",
                "@Inject\ninternal class PlainViewModel(port: SoundPort) : ViewModel()",
            ),
            "feature/browse/src/commonMain/kotlin/KeyOnly.kt" to featureSource(
                "",
                "@Inject\n@ViewModelKey\ninternal class KeyOnlyViewModel : ViewModel()",
            ),
            "feature/browse/src/commonMain/kotlin/Assisted.kt" to featureSource(
                "",
                """
                    @AssistedInject
                    internal class AssistedViewModel(@Assisted id: String) : ViewModel() {
                        @AssistedFactory
                        fun interface Factory {
                            fun create(id: String): AssistedViewModel
                        }
                    }
                    // A later registered class must not lend its contribution to the one above.
                    @ContributesIntoMap(AppScope::class)
                    @ManualViewModelAssistedFactoryKey
                    internal class Unrelated
                """.trimIndent(),
            ),
        )
        val violations = FeatureFirstRules.unregisteredViewModelViolations(unregistered)
        assertEquals(3, violations.size)
        assertTrue(violations.any { "PlainViewModel" in it && "Plain.kt:3" in it })
        assertTrue(violations.any { "KeyOnlyViewModel" in it })
        assertTrue(violations.any { "AssistedViewModel" in it })
    }

    /**
     * The shape that crashed a device: `key = { it.id }` compiles, passes every test that runs on
     * a simulator, and throws on Android the moment the list is measured inside a navigation entry.
     */
    @Test
    fun aValueClassIdUsedAsALazyKeyIsReported() {
        val offender = mapOf(
            "feature/browse/src/commonMain/kotlin/BrowseScreen.kt" to
                "items(state.categories, key = { it.id }) { category -> }",
        )

        val reported = FeatureFirstRules.lazyListKeyViolations(offender)

        assertEquals(1, reported.size)
        assertTrue(reported.single().contains(".value"), "the fix belongs in the message")
        assertTrue(reported.single().contains(":1"), "the line is what a reader needs")
    }

    /** Everything that is already a key a Bundle can hold, and the comment that talks about one. */
    @Test
    fun keysAlreadySafeAreLeftAlone() {
        val safe = mapOf(
            "a.kt" to "items(state.tracks, key = { it.id.value }) { track -> }",
            "b.kt" to "items(state.rows, key = { it.name }) { row -> }",
            "c.kt" to "itemsIndexed(state.rows) { index, row -> }",
            "d.kt" to "// a key = { it.id } here is prose, not code",
            "e.kt" to """val hint = "key = { it.id }"""",
        )

        assertEquals(emptyList(), FeatureFirstRules.lazyListKeyViolations(safe))
    }

    /**
     * The type just removed from `:designsystem`, and the shape of its return: a two-state wrapper
     * in a module that owns no screen, which every feature then has to import.
     */
    @Test
    fun aSharedLoadingStateOutsideAFeatureIsReported() {
        val offenders = mapOf(
            "designsystem/src/commonMain/kotlin/Loadable.kt" to """
                package com.xwab.app.designsystem.state

                sealed interface Loadable<out T> {
                    data object Loading : Loadable<Nothing>

                    data class Ready<T>(val value: T) : Loadable<T>
                }
            """.trimIndent(),
            "shared/src/commonMain/kotlin/ScreenState.kt" to """
                package com.xwab.app.state

                internal sealed interface ScreenState {
                    data object Loading : ScreenState
                }
            """.trimIndent(),
        )

        val reported = FeatureFirstRules.featureStateViolations(offenders)

        assertEquals(4, reported.size)
        assertTrue(
            reported.all { it.contains("that screen's own state") },
            "the message has to say where the state belongs",
        )
    }

    /**
     * An engine phase is a real capability state that happens to use the same two words.
     *
     * Feature sources are not exercised here because they never reach this rule: the task hands it
     * only the production sources outside `feature/`.
     */
    @Test
    fun capabilityStatesThatMerelyShareTheNameAreLeftAlone() {
        val safe = mapOf(
            "core/playback/src/commonMain/kotlin/AudioPlayerState.kt" to """
                package com.xwab.app.core.playback.port

                enum class PlaybackPhase {
                    Idle,
                    Loading,
                    Ready,
                }
            """.trimIndent(),
            "core/session/src/commonMain/kotlin/PlaybackSummary.kt" to """
                package com.xwab.app.core.session.port

                data class PlaybackSummary(
                    val playWhenReady: Boolean = false,
                    val isPreparing: Boolean = false,
                )
            """.trimIndent(),
        )

        assertEquals(emptyList(), FeatureFirstRules.featureStateViolations(safe))
    }

    @Test
    fun fixedApplicationStructureStillDetectsStaleRules() {
        val modules = FeatureFirstRules.INDEPENDENT_SUPPORT_MODULES + FeatureFirstRules.SHELL_MODULE +
            FeatureFirstRules.COMPOSITION_ROOT
        assertEquals(emptyList(), FeatureFirstRules.staleRuleViolations(modules))
        assertEquals(3, FeatureFirstRules.staleRuleViolations(emptySet()).size)
        assertTrue(FeatureFirstRules.staleRuleViolations(modules - ":designsystem")
            .single().contains("INDEPENDENT_SUPPORT_MODULES"))
        assertTrue(FeatureFirstRules.staleRuleViolations(modules - ":shared")
            .single().contains("SHELL_MODULE"))
        assertTrue(FeatureFirstRules.staleRuleViolations(modules - ":composition")
            .single().contains("COMPOSITION_ROOT"))
    }

    @Test
    fun aFeatureDeclaresOnlyInItsOwnPackage() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.featurePackageOwnershipViolations(
                mapOf(
                    "feature/browse/api/src/commonMain/kotlin/Route.kt" to featureSource(".navigation", "data object BrowseRoute"),
                    "feature/browse/impl/src/commonMain/kotlin/Screen.kt" to featureSource("", "internal fun BrowseScreen() = Unit"),
                    "feature/sleep-timer/impl/src/commonMain/kotlin/Timer.kt" to
                        "package com.xwab.app.feature.sleeptimer.domain\ninternal class Timer",
                    // Not a feature source; the rule leaves it to the others.
                    "shared/src/commonMain/kotlin/App.kt" to "package com.xwab.app\nfun App() = Unit",
                ),
            ),
        )

        val violations = FeatureFirstRules.featurePackageOwnershipViolations(
            mapOf(
                // Filed under another feature's package.
                "feature/category/impl/src/commonMain/kotlin/Browse.kt" to featureSource("", "internal class Leak"),
                // Under no feature at all.
                "feature/browse/impl/src/commonMain/kotlin/Shared.kt" to "package com.xwab.app.ui\ninternal class Leak",
                // A name that only starts like the feature's.
                "feature/sound/impl/src/commonMain/kotlin/Sounds.kt" to
                    "package com.xwab.app.feature.soundscape\ninternal class Leak",
            ),
        )
        assertEquals(3, violations.size)
        assertTrue(violations.all { it.contains("owns only com.xwab.app.feature.") })
    }

    @Test
    fun onlyTestsAndOtherFakesDependOnTestFakes() {
        val violations = dependencyViolations(
            mapOf(
                ":feature:sound:impl" to listOf(":testing:sound"),
                ":shared" to listOf(":testing:session"),
                ":composition" to listOf(":testing:favorites"),
            ),
        )
        assertEquals(3, violations.count { it.contains("in production. Test fakes belong to test configurations") })

        assertEquals(
            emptyList(),
            dependencyViolations(mapOf(":testing:sound" to listOf(":core:sound:api", ":testing:favorites"))),
        )
    }

    @Test
    fun featureSpecificUseCasesStayInTheirFeature() {
        val violations = FeatureFirstRules.leakedUseCaseViolations(
            useCases = listOf("ObserveSoundsContentUseCase" to ":core:sound"),
            sourcesByFeature = mapOf(
                "category" to listOf("ObserveSoundsContentUseCase()"),
                "sound" to listOf("unrelated"),
            ),
        )
        assertEquals(1, violations.size)

        assertEquals(
            emptyList(),
            FeatureFirstRules.leakedUseCaseViolations(
                useCases = listOf("SharedUseCase" to ":core:sound"),
                sourcesByFeature = mapOf(
                    "category" to listOf("SharedUseCase()"),
                    "sound" to listOf("SharedUseCase()"),
                ),
            ),
        )
    }

    @Test
    fun contentSourcesAreAttributedToTheirModule() {
        assertEquals(
            ":core:sound",
            FeatureFirstRules.owningModule(
                "core/sound/src/commonMain/kotlin/Port.kt",
                listOf(":core", ":core:sound"),
            ),
        )
        assertNull(
            FeatureFirstRules.owningModule("gradle/libs.versions.toml", listOf(":core:sound")),
        )
    }

    @Test
    fun corePublicSurfaceIsConfinedToPortPackages() {
        val good = listOf(
            coreSource("port/SoundPort.kt", ".port", "interface SoundPort"),
            coreSource("port/Outcome.kt", ".port", "public sealed interface Outcome"),
            coreSource("ManifestAdapter.kt", "", "internal class ManifestAdapter"),
        )
        assertEquals(emptyList(), FeatureFirstRules.coreVisibilityViolations(good))

        val explicitPublic = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("port/SoundPort.kt", ".port", "public interface SoundPort")),
        )
        assertEquals(emptyList(), explicitPublic)

        val implicitWrongName = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("port/Catalog.kt", ".port", "interface Catalog")),
        )
        assertEquals(1, implicitWrongName.size)
        assertTrue(implicitWrongName.single().contains("must end in Port"))

        val hiddenPortDeclaration = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("port/Helper.kt", ".port", "private object Helper")),
        )
        assertEquals(1, hiddenPortDeclaration.size)
        assertTrue(hiddenPortDeclaration.single().contains("must be public"))

        val leakedAdapter = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("ManifestAdapter.kt", "", "class ManifestAdapter")),
        )
        assertEquals(1, leakedAdapter.size)
        assertTrue(leakedAdapter.single().contains("outside a port package"))

        val leakedSuspendingHelper = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("Refresh.kt", "", "suspend fun refresh() = Unit")),
        )
        assertEquals(1, leakedSuspendingHelper.size)

        val reorderedVisibility = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("Refresh.kt", "", "suspend public fun refresh() = Unit")),
        )
        assertEquals(1, reorderedVisibility.size)

        val genericHelper = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("Mapper.kt", "", "inline fun <T> map(value: T) = value")),
        )
        assertEquals(1, genericHelper.size)

        val annotatedLeak = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("Adapter.kt", "", "@Deprecated(\"old\") class Adapter")),
        )
        assertEquals(1, annotatedLeak.size)

        val escapedNameLeak = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("Odd.kt", "", "class `Leaked adapter`")),
        )
        assertEquals(1, escapedNameLeak.size)

        val indentedTopLevelLeak = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("Indented.kt", "", "    class IndentedLeak")),
        )
        assertEquals(1, indentedTopLevelLeak.size)

        // The same holds for a nested port declaration: a member with no keyword is still public.
        val implicitPortMember = FeatureFirstRules.CoreSource(
            path = "core/sample/src/commonMain/kotlin/port/SamplePort.kt",
            module = ":core:sample",
            packageName = "com.xwab.app.core.sample.port",
            source = """
                package com.xwab.app.core.sample.port
                interface SamplePort {
                    fun observe()
                }
            """.trimIndent(),
        )
        assertEquals(emptyList(), FeatureFirstRules.coreVisibilityViolations(listOf(implicitPortMember)))

        val implicitCompanion = FeatureFirstRules.CoreSource(
            path = "core/sample/src/commonMain/kotlin/port/Sample.kt",
            module = ":core:sample",
            packageName = "com.xwab.app.core.sample.port",
            source = """
                package com.xwab.app.core.sample.port
                class Sample {
                    companion object
                }
            """.trimIndent(),
        )
        assertEquals(emptyList(), FeatureFirstRules.coreVisibilityViolations(listOf(implicitCompanion)))

        val internalAdapterMember = FeatureFirstRules.CoreSource(
            path = "core/sample/src/commonMain/kotlin/SampleAdapter.kt",
            module = ":core:sample",
            packageName = "com.xwab.app.core.sample",
            source = """
                package com.xwab.app.core.sample
                internal class SampleAdapter(
                    val implementationDetail: String,
                ) {
                    fun work() = Unit
                }
            """.trimIndent(),
        )
        assertEquals(emptyList(), FeatureFirstRules.coreVisibilityViolations(listOf(internalAdapterMember)))

        val disguisedPortPackage = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("adapter/port/EscapePort.kt", ".adapter.port", "public interface EscapePort")),
        )
        assertEquals(1, disguisedPortPackage.size)

        val packageLessLeak = FeatureFirstRules.coreVisibilityViolations(
            listOf(
                FeatureFirstRules.CoreSource(
                    path = "core/sample/src/commonMain/kotlin/Leak.kt",
                    module = ":core:sample",
                    packageName = "",
                    source = "class Leak",
                ),
            ),
        )
        assertEquals(1, packageLessLeak.size)

        val wrongName = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("port/Catalog.kt", ".port", "interface Catalog")),
        )
        assertEquals(1, wrongName.size)
        assertTrue(wrongName.single().contains("must end in Port"))
    }

    @Test
    fun coreModulesImportOnlyOtherCorePorts() {
        val port = FeatureFirstRules.CoreSource(
            path = "core/beta/src/commonMain/kotlin/com/xwab/app/core/beta/port/BetaPort.kt",
            module = ":core:beta",
            packageName = "com.xwab.app.core.beta.port",
            source = """
                package com.xwab.app.core.beta.port
                public interface BetaPort
            """.trimIndent(),
        )
        val helper = FeatureFirstRules.CoreSource(
            path = "core/beta/src/commonMain/kotlin/com/xwab/app/core/beta/BetaHelper.kt",
            module = ":core:beta",
            packageName = "com.xwab.app.core.beta",
            source = """
                package com.xwab.app.core.beta
                internal class BetaHelper
            """.trimIndent(),
        )
        val validConsumer = FeatureFirstRules.CoreSource(
            path = "core/alpha/src/commonMain/kotlin/com/xwab/app/core/alpha/Alpha.kt",
            module = ":core:alpha",
            packageName = "com.xwab.app.core.alpha",
            source = """
                package com.xwab.app.core.alpha
                import com.xwab.app.core.beta.port.BetaPort
                internal class Alpha(private val beta: BetaPort)
            """.trimIndent(),
        )
        assertEquals(
            emptyList(),
            FeatureFirstRules.coreImportViolations(listOf(port, helper, validConsumer)),
        )

        val invalidConsumer = validConsumer.copy(
            source = """
                package com.xwab.app.core.alpha
                import com.xwab.app.core.beta.BetaHelper
                internal class Alpha(private val beta: BetaHelper)
            """.trimIndent(),
        )
        val violations = FeatureFirstRules.coreImportViolations(listOf(port, helper, invalidConsumer))
        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("only through port packages"))

        val wildcardConsumer = validConsumer.copy(
            source = """
                package com.xwab.app.core.alpha
                import com.xwab.app.core.beta.*
                internal class Alpha(private val beta: BetaHelper)
            """.trimIndent(),
        )
        assertEquals(
            1,
            FeatureFirstRules.coreImportViolations(listOf(port, helper, wildcardConsumer)).size,
        )

        val fullyQualifiedConsumer = validConsumer.copy(
            source = """
                package com.xwab.app.core.alpha
                internal class Alpha(
                    private val beta: com.xwab.app.core.beta.BetaHelper,
                )
            """.trimIndent(),
        )
        assertEquals(
            1,
            FeatureFirstRules.coreImportViolations(listOf(port, helper, fullyQualifiedConsumer)).size,
        )

        val fullyQualifiedPortConsumer = validConsumer.copy(
            source = """
                package com.xwab.app.core.alpha
                internal class Alpha(
                    private val beta: com.xwab.app.core.beta.port.BetaPort,
                )
            """.trimIndent(),
        )
        assertEquals(
            emptyList(),
            FeatureFirstRules.coreImportViolations(listOf(port, helper, fullyQualifiedPortConsumer)),
        )

        val wildcardPortConsumer = validConsumer.copy(
            source = """
                package com.xwab.app.core.alpha
                import com.xwab.app.core.beta.port.*
                internal class Alpha(private val beta: BetaPort)
            """.trimIndent(),
        )
        assertEquals(
            emptyList(),
            FeatureFirstRules.coreImportViolations(listOf(port, helper, wildcardPortConsumer)),
        )

        val disguisedPortReference = validConsumer.copy(
            source = """
                package com.xwab.app.core.alpha
                import com.xwab.app.core.beta.adapter.port.BetaHelper
                internal class Alpha(private val beta: BetaHelper)
            """.trimIndent(),
        )
        assertEquals(
            1,
            FeatureFirstRules.coreImportViolations(listOf(port, helper, disguisedPortReference)).size,
        )

        val portSubpackageType = FeatureFirstRules.CoreSource(
            path = "core/beta/src/commonMain/kotlin/com/xwab/app/core/beta/port/internal/Hidden.kt",
            module = ":core:beta",
            packageName = "com.xwab.app.core.beta.port.internal",
            source = """
                package com.xwab.app.core.beta.port.internal
                internal class Hidden
            """.trimIndent(),
        )
        val portSubpackageConsumer = validConsumer.copy(
            source = """
                package com.xwab.app.core.alpha
                import com.xwab.app.core.beta.port.internal.Hidden
                internal class Alpha(private val hidden: Hidden)
            """.trimIndent(),
        )
        assertEquals(
            1,
            FeatureFirstRules.coreImportViolations(
                listOf(port, helper, portSubpackageType, portSubpackageConsumer),
            ).size,
        )

        val textOnlyReferences = validConsumer.copy(
            source = "package com.xwab.app.core.alpha\n" +
                "// com.xwab.app.core.beta.BetaHelper is documentation only.\n" +
                "internal const val ACTION = \"com.xwab.app.core.beta.BetaHelper\"\n" +
                "internal val raw = \"\"\"com.xwab.app.core.beta.BetaHelper\"\"\"",
        )
        assertEquals(
            emptyList(),
            FeatureFirstRules.coreImportViolations(listOf(port, helper, textOnlyReferences)),
        )

        val declarationText = FeatureFirstRules.CoreSource(
            path = "core/alpha/src/commonMain/kotlin/com/xwab/app/core/alpha/Docs.kt",
            module = ":core:alpha",
            packageName = "com.xwab.app.core.alpha",
            source = "package com.xwab.app.core.alpha\n" +
                "internal val docs = \"\"\"\n" +
                "class NotCodeRepository\n" +
                "\"\"\"\n" +
                "internal class RealAdapter",
        )
        assertEquals(emptyList(), FeatureFirstRules.coreVisibilityViolations(listOf(declarationText)))
        assertEquals(emptyList(), FeatureFirstRules.legacyCoreAbstractionViolations(listOf(declarationText)))
    }

    @Test
    fun coreHasNoRepositoryOrProviderAbstractions() {
        val sources = listOf(
            coreSource("SoundRepository.kt", "", "internal interface SoundRepository"),
            coreSource("SoundRepositoryImpl.kt", "", "internal class SoundRepositoryImpl"),
            FeatureFirstRules.CoreSource(
                path = "core/sample/src/commonMain/kotlin/Container.kt",
                module = ":core:sample",
                packageName = "com.xwab.app.core.sample",
                source = """
                    package com.xwab.app.core.sample
                    internal class Container {
                        class NestedProviderAdapter
                    }
                """.trimIndent(),
            ),
            coreSource("SoundAdapter.kt", "", "internal class SoundAdapter"),
            coreSource("SoundPortImpl.kt", "", "internal class SoundPortImpl"),
        )

        val violations = FeatureFirstRules.legacyCoreAbstractionViolations(sources)
        assertEquals(4, violations.size)
        assertEquals(3, violations.count { it.contains("feature-local") })
        assertTrue(violations.single { it.contains("SoundPortImpl") }.contains("Adapter name"))
    }

    @Test
    fun contentModulesExposeExactlyOnePort() {
        listOf("sound" to "SoundPort", "story" to "StoryPort").forEach { (name, port) ->
            fun source(declaration: String) = FeatureFirstRules.CoreSource(
                path = "core/$name/src/commonMain/kotlin/port/$port.kt",
                module = ":core:$name",
                packageName = "com.xwab.app.core.$name.port",
                source = declaration,
            )
            assertEquals(emptyList(), corePortViolations(
                listOf(source("interface $port")),
            ))
            assertEquals(1, corePortViolations(
                listOf(source("interface $port\ninterface ExtraPort")),
            ).size)
            assertEquals(1, corePortViolations(
                listOf(source("interface $port {\n    interface ExtraPort\n}")),
            ).size)
            assertEquals(emptyList(), corePortViolations(
                listOf(source("interface $port {\n    data class Model(val id: String)\n}")),
            ))
            assertEquals(1, corePortViolations(
                listOf(source("data class Model(val id: String)")),
            ).size)
        }
    }

    /**
     * Two reasons to be on this list, and the rule treats them the same: what is not written down
     * is not allowed. Favorites and delivery stay reusable outside this app; `:core:session` stays
     * ignorant of what a sound or a story is.
     */
    @Test
    fun aModuleWithAnExhaustiveDependencyListCannotStrayFromIt() {
        for (module in listOf(":core:favorites", ":core:delivery")) {
            for (dependency in listOf(":core:sound", ":core:story", ":core:session", ":shared", ":composition")) {
                assertTrue(
                    dependencyViolations(mapOf(module to listOf(dependency)))
                        .any { it.contains("is not among them") || it.contains("may not depend on UI") },
                    "$module -> $dependency",
                )
            }
        }

        // Letting any of these back in is what would put a content type in this module again.
        for (dependency in listOf(":core:sound", ":core:story", ":core:delivery")) {
            assertTrue(
                dependencyViolations(mapOf(":core:session" to listOf(dependency)))
                    .any { it.contains("is not among them") },
                ":core:session -> $dependency",
            )
        }

        assertEquals(
            emptyList(),
            dependencyViolations(
                mapOf(
                    ":core:favorites" to emptyList(),
                    ":core:delivery" to listOf(":core:network"),
                    ":core:session" to listOf(":core:playback"),
                ),
            ),
        )
    }

    @Test
    fun currentModuleGraphSatisfiesDependencyRules() {
        val capabilities = listOf("sound", "story", "network", "delivery", "favorites", "playback", "session")
        val coreModules = capabilities.flatMap { listOf(":core:$it:api", ":core:$it:impl") }
        val graph = mapOf(
            ":core:sound:api" to emptyList(),
            ":core:sound:impl" to listOf(":core:sound:api", ":core:session:api", ":core:delivery:api"),
            ":core:story:api" to emptyList(),
            ":core:story:impl" to listOf(":core:story:api", ":core:session:api"),
            ":core:network:api" to emptyList(),
            ":core:network:impl" to listOf(":core:network:api"),
            ":core:delivery:api" to emptyList(),
            ":core:delivery:impl" to listOf(":core:delivery:api", ":core:network:api"),
            ":core:favorites:api" to emptyList(),
            ":core:favorites:impl" to listOf(":core:favorites:api"),
            ":core:playback:api" to emptyList(),
            ":core:playback:impl" to listOf(":core:playback:api"),
            ":core:session:api" to emptyList(),
            ":core:session:impl" to listOf(":core:session:api", ":core:playback:api"),
            ":designsystem" to emptyList(),
            ":testing:sound" to listOf(":core:sound:api", ":testing:favorites"),
            ":testing:favorites" to listOf(":core:favorites:api"),
            ":testing:session" to listOf(":core:session:api"),
            ":feature:browse:api" to listOf(":core:sound:api"),
            ":feature:browse:impl" to listOf(":feature:browse:api", ":core:sound:api", ":designsystem"),
            ":feature:category:api" to listOf(":core:sound:api"),
            ":feature:category:impl" to listOf(
                ":feature:category:api", ":core:sound:api", ":core:favorites:api", ":core:session:api",
                ":designsystem",
            ),
            ":feature:favorites:api" to listOf(":core:sound:api"),
            ":feature:favorites:impl" to listOf(
                ":feature:favorites:api", ":core:sound:api", ":core:favorites:api", ":core:session:api",
                ":designsystem",
            ),
            ":feature:sound:api" to emptyList(),
            ":feature:sound:impl" to listOf(
                ":feature:sound:api", ":core:sound:api", ":core:favorites:api", ":core:session:api",
                ":designsystem",
            ),
            ":feature:story:api" to listOf(":core:story:api"),
            ":feature:story:impl" to listOf(
                ":feature:story:api", ":core:story:api", ":core:session:api", ":designsystem",
            ),
            ":feature:nowplaying:api" to listOf(":core:session:api"),
            ":feature:nowplaying:impl" to listOf(":feature:nowplaying:api", ":core:session:api", ":designsystem"),
            ":shared" to listOf(
                ":core:session:api", ":core:sound:api", ":core:story:api",
                ":designsystem",
                ":feature:browse:api", ":feature:category:api", ":feature:favorites:api",
                ":feature:sound:api", ":feature:story:api", ":feature:nowplaying:api",
            ),
            ":composition" to listOf(":shared") + coreModules + listOf(
                ":feature:browse:api", ":feature:browse:impl",
                ":feature:category:api", ":feature:category:impl",
                ":feature:favorites:api", ":feature:favorites:impl",
                ":feature:sound:api", ":feature:sound:impl",
                ":feature:story:api", ":feature:story:impl",
                ":feature:nowplaying:api", ":feature:nowplaying:impl",
            ),
            ":androidApp" to listOf(":composition"),
        )
        val apiEdges = mapOf(
            ":testing:sound" to listOf(":core:sound:api", ":testing:favorites"),
            ":testing:favorites" to listOf(":core:favorites:api"),
            ":testing:session" to listOf(":core:session:api"),
        )

        assertEquals(emptyList(), FeatureFirstRules.staleRuleViolations(graph.keys))
        assertEquals(emptyList(), FeatureFirstRules.corePolicyViolations(graph.keys, corePolicies))
        assertEquals(emptyList(), FeatureFirstRules.unwiredModuleViolations(graph))
        assertEquals(emptyList(), FeatureFirstRules.featureModuleShapeViolations(graph.keys))
        assertEquals(emptyList(), FeatureFirstRules.coreModuleShapeViolations(graph.keys))
        assertEquals(emptyList(), dependencyViolations(graph, apiEdges))
    }

    @Test
    fun supportModulesCannotAcquireApplicationDependencies() {
        for (module in listOf(":designsystem")) {
            for (dependency in listOf(":feature:sound", ":core:sound", ":shared", ":composition", ":testing")) {
                assertTrue(dependencyViolations(mapOf(module to listOf(dependency)))
                    .any { it.contains("must remain independent") })
            }
            assertEquals(emptyList(), dependencyViolations(mapOf(module to listOf(module))))
        }
    }

    @Test
    fun coreCannotDependOnUiOrTheShell() {
        for (dependency in listOf(":designsystem", ":shared", ":composition")) {
            assertTrue(dependencyViolations(mapOf(":core:sound" to listOf(dependency)))
                .any { it.contains("may not depend on UI") })
        }
    }

    @Test
    fun aModuleDirectoryMissingFromTheBuildIsReported() {
        val violations = FeatureFirstRules.unregisteredModuleViolations(
            moduleDirectories = listOf("core/sound", "core/meditation", "feature/browse"),
            modules = setOf(":core:sound", ":feature:browse"),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("core/meditation"))
        assertTrue(violations.single().contains("settings.gradle.kts"))
    }

    @Test
    fun everyRegisteredModuleDirectoryIsAccepted() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.unregisteredModuleViolations(
                moduleDirectories = listOf("core/sound", "feature/browse", "designsystem"),
                modules = setOf(":core:sound", ":feature:browse", ":designsystem"),
            ),
        )
    }

    @Test
    fun aCapabilityOrScreenTheCompositionRootNeverDeclaresIsReported() {
        val violations = FeatureFirstRules.unwiredModuleViolations(
            mapOf(
                FeatureFirstRules.COMPOSITION_ROOT to listOf(":core:sound", ":feature:browse:api"),
                ":core:sound" to emptyList(),
                ":core:meditation" to emptyList(),
                ":feature:browse:api" to emptyList(),
                ":feature:browse:impl" to emptyList(),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("${FeatureFirstRules.COMPOSITION_ROOT} does not depend on it") })
        assertTrue(violations.any { it.contains(":core:meditation") })
        assertTrue(violations.any { it.contains(":feature:browse:impl") })
    }

    @Test
    fun aFeatureContractTheShellNeverDeclaresIsReported() {
        val violations = FeatureFirstRules.unwiredModuleViolations(
            mapOf(
                FeatureFirstRules.SHELL_MODULE to listOf(":feature:browse:api"),
                ":feature:browse:api" to emptyList(),
                ":feature:sound:api" to emptyList(),
                // The shell is not where implementations are installed; their absence is right.
                ":feature:sound:impl" to emptyList(),
                ":core:sound" to emptyList(),
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains(":feature:sound:api is in the build but ${FeatureFirstRules.SHELL_MODULE}"))
    }

    /** Support modules are not capabilities; the rule is scoped to core and feature on purpose. */
    @Test
    fun theCompositionRootNeedNotDeclareSupportModules() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.unwiredModuleViolations(
                mapOf(
                    FeatureFirstRules.COMPOSITION_ROOT to listOf(":core:sound", ":feature:browse:api"),
                    ":core:sound" to emptyList(),
                    ":feature:browse:api" to emptyList(),
                    ":testing" to emptyList(),
                    ":androidApp" to listOf(FeatureFirstRules.COMPOSITION_ROOT),
                ),
            ),
        )
    }

    @Test
    fun theCompositionRootNamesNoFeature() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.compositionRootFeatureReferenceViolations(
                mapOf(
                    "composition/src/commonMain/kotlin/AppEntryGraph.kt" to
                        "package com.xwab.app.composition\n" +
                        "import com.xwab.app.navigation.Navigator\n" +
                        "// Entries come from com.xwab.app.feature.browse.navigation through Metro.\n" +
                        "internal interface AppEntryGraph : AppEntries",
                ),
            ),
        )

        val violations = FeatureFirstRules.compositionRootFeatureReferenceViolations(
            mapOf(
                "composition/src/commonMain/kotlin/AppEntryGraph.kt" to
                    "package com.xwab.app.composition\n" +
                    "import com.xwab.app.feature.nowplaying.shell.NowPlayingBar\n" +
                    "val bar = com.xwab.app.feature.browse.navigation.BrowseEntryBindings",
            ),
        )
        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("The composition root names no feature") })
    }

    @Test
    fun aRouteWithoutAnExplicitSerialNameIsReported() {
        val violations = FeatureFirstRules.routeSerialNameViolations(
            mapOf(
                "feature/browse/src/commonMain/kotlin/BrowseNavigation.kt" to featureSource(
                    ".navigation",
                    """
                    @Serializable
                    data object BrowseRoute : NavKey
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("BrowseRoute"))
        assertTrue(violations.single().contains("@SerialName"))
    }

    @Test
    fun aRouteThatNamesItselfIsAccepted() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.routeSerialNameViolations(
                mapOf(
                    "feature/sound/src/commonMain/kotlin/SoundNavigation.kt" to featureSource(
                        ".navigation",
                        """
                        @Serializable
                        @SerialName("com.xwab.app.feature.sound.navigation.SoundRoute")
                        data class SoundRoute(val trackId: String) : NavKey
                        """.trimIndent(),
                    ),
                ),
            ),
        )
    }

    /** A commented-out name is not a name; `codeOnly` is what makes the rule see that. */
    @Test
    fun aSerialNameInACommentDoesNotCount() {
        val violations = FeatureFirstRules.routeSerialNameViolations(
            mapOf(
                "feature/story/src/commonMain/kotlin/StoriesNavigation.kt" to featureSource(
                    ".navigation",
                    """
                    @Serializable
                    // @SerialName("com.xwab.app.feature.story.navigation.StoriesRoute")
                    data object StoriesRoute : NavKey
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("StoriesRoute"))
    }

    @Test
    fun aRouteEveryFeatureRegistersInItsOwnContributionIsAccepted() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.unregisteredRouteViolations(
                mapOf(
                    "feature/story/src/commonMain/kotlin/StoriesNavigation.kt" to featureSource(
                        ".navigation",
                        """
                        data object StoriesRoute : NavKey
                        data class StoryRoute(val storyId: String) : NavKey
                        @ContributesTo(NavKey::class)
                        @BindingContainer
                        object StoriesNavigationBindings {
                            @Provides
                            @IntoSet
                            fun provideRouteSerializers(): SerializersModule = SerializersModule {
                                polymorphic(NavKey::class) {
                                    subclass(StoriesRoute::class)
                                    subclass(StoryRoute::class, StoryRouteSerializer)
                                }
                            }
                        }
                        """.trimIndent(),
                    ),
                    // A block body and annotations in the other order are still one contribution.
                    "feature/browse/src/commonMain/kotlin/BrowseNavigation.kt" to featureSource(
                        ".navigation",
                        """
                        data object BrowseRoute : NavKey
                        @ContributesTo(NavKey::class)
                        @BindingContainer
                        object BrowseNavigationBindings {
                            @IntoSet
                            @Provides
                            fun provideRouteSerializers(): SerializersModule {
                                return SerializersModule { polymorphic(NavKey::class) { subclass(BrowseRoute::class) } }
                            }
                        }
                        """.trimIndent(),
                    ),
                ),
            ),
        )
    }

    /** Registered by another feature, or only in a comment, is still not registered by this one. */
    @Test
    fun aRouteMissingFromItsFeaturesContributionIsReported() {
        val violations = FeatureFirstRules.unregisteredRouteViolations(
            mapOf(
                "feature/story/src/commonMain/kotlin/StoriesNavigation.kt" to featureSource(
                    ".navigation",
                    """
                    data object StoriesRoute : NavKey
                    data class StoryRoute(val storyId: String) : NavKey
                    @ContributesTo(NavKey::class)
                    @BindingContainer
                    object StoriesNavigationBindings {
                        @Provides
                        @IntoSet
                        fun provideRouteSerializers(): SerializersModule = SerializersModule {
                            subclass(StoriesRoute::class)
                            // subclass(StoryRoute::class)
                        }
                    }
                    """.trimIndent(),
                ),
                "feature/sound/src/commonMain/kotlin/SoundNavigation.kt" to featureSource(
                    ".navigation",
                    """
                    @ContributesTo(NavKey::class)
                    @BindingContainer
                    object SoundNavigationBindings {
                        @Provides
                        @IntoSet
                        fun provideRouteSerializers(): SerializersModule = SerializersModule { subclass(StoryRoute::class) }
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("StoryRoute"))
        assertTrue(violations.single().contains("feature/story"))
    }

    /** Every marker the rule needs is in the feature, but not on the one provider that counts. */
    @Test
    fun aRegistrationOutsideAnIntoSetProviderOfTheRouteScopeIsReported() {
        val violations = FeatureFirstRules.unregisteredRouteViolations(
            mapOf(
                // Registered, but its provider lacks @IntoSet; the entry container below has one.
                "feature/favorites/src/commonMain/kotlin/FavoritesNavigation.kt" to featureSource(
                    ".navigation",
                    """
                    data object FavoritesRoute : NavKey
                    @ContributesTo(NavKey::class)
                    @BindingContainer
                    object FavoritesNavigationBindings {
                        @Provides
                        fun provideRouteSerializers(): SerializersModule = SerializersModule {
                            subclass(FavoritesRoute::class)
                        }
                    }
                    """.trimIndent(),
                ),
                "feature/favorites/src/commonMain/kotlin/FavoritesEntry.kt" to featureSource(
                    ".navigation",
                    """
                    @ContributesTo(EntryProviderScope::class)
                    @BindingContainer
                    object FavoritesEntryBindings {
                        @Provides
                        @IntoSet
                        fun provideEntryProviderInstaller(): EntryProviderScope<NavKey>.() -> Unit = {}
                    }
                    """.trimIndent(),
                ),
                // Provided @IntoSet, but into the entry scope the route serializers graph never reads.
                "feature/sound/src/commonMain/kotlin/SoundNavigation.kt" to featureSource(
                    ".navigation",
                    """
                    data class SoundRoute(val trackId: String) : NavKey
                    @ContributesTo(EntryProviderScope::class)
                    @BindingContainer
                    object SoundNavigationBindings {
                        @Provides
                        @IntoSet
                        fun provideRouteSerializers(): SerializersModule = SerializersModule {
                            subclass(SoundRoute::class)
                        }
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.any { "FavoritesRoute" in it })
        assertTrue(violations.any { "SoundRoute" in it })
    }

    @Test
    fun aPlaybackKindWithNoRouteIsReported() {
        val violations = FeatureFirstRules.unroutedPlaybackKindViolations(
            coreSources = mapOf(
                "core/meditation/src/commonMain/kotlin/MeditationResolver.kt" to
                    """
                    @ContributesIntoMap(AppScope::class)
                    @StringKey(MEDITATION_PLAYBACK_KIND)
                    internal class MeditationPlaybackResolver : PlaybackItemResolver
                    """.trimIndent(),
            ),
            compositionSources = mapOf(
                "shared/src/commonMain/kotlin/AppNowPlayingSceneDecorator.kt" to
                    "SOUND_PLAYBACK_KIND -> SoundRoute(value)",
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("MEDITATION_PLAYBACK_KIND"))
        assertTrue(violations.single().contains("no screen to open"))
    }

    @Test
    fun aPlaybackKindTheShellRoutesIsAccepted() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.unroutedPlaybackKindViolations(
                coreSources = mapOf(
                    "core/sound/src/commonMain/kotlin/SoundPlaybackResolver.kt" to
                        """
                        @StringKey(SOUND_PLAYBACK_KIND)
                        internal class SoundPlaybackResolver : PlaybackItemResolver
                        """.trimIndent(),
                ),
                compositionSources = mapOf(
                    "shared/src/commonMain/kotlin/AppNowPlayingSceneDecorator.kt" to
                        "SOUND_PLAYBACK_KIND -> SoundRoute(value)",
                ),
            ),
        )
    }

    /** A map key on something that is not a resolver is none of this rule's business. */
    @Test
    fun aMapKeyOutsideAResolverIsIgnored() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.unroutedPlaybackKindViolations(
                coreSources = mapOf(
                    "core/other/src/commonMain/kotlin/Thing.kt" to
                        "@StringKey(SOME_OTHER_KEY)\ninternal class Thing : SomethingElse",
                ),
                compositionSources = emptyMap(),
            ),
        )
    }

    /** A commented-out registration is not a registration; `codeOnly` is what sees that. */
    @Test
    fun aCommentedOutPlaybackKindIsIgnored() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.unroutedPlaybackKindViolations(
                coreSources = mapOf(
                    "core/sound/src/commonMain/kotlin/SoundPlaybackResolver.kt" to
                        "// @StringKey(GONE_PLAYBACK_KIND)\ninternal class X : PlaybackItemResolver",
                ),
                compositionSources = emptyMap(),
            ),
        )
    }

    /** Each way the shell opens a destination today: a tab, a callback, the now-playing bar. */
    @Test
    fun aRouteTheShellOpensIsAccepted() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.unopenedRouteViolations(
                featureSources = mapOf(
                    "feature/browse/api/src/commonMain/kotlin/BrowseNavigation.kt" to featureSource(
                        ".navigation",
                        "data object BrowseRoute : NavKey\ndata class CategoryRoute(val categoryId: String) : NavKey",
                    ),
                    "feature/story/api/src/commonMain/kotlin/StoriesNavigation.kt" to featureSource(
                        ".navigation",
                        "data object StoriesRoute : NavKey\ndata class StoryRoute(val storyId: String) : NavKey",
                    ),
                    "feature/favorites/api/src/commonMain/kotlin/FavoritesNavigation.kt" to featureSource(
                        ".navigation",
                        "data object FavoritesRoute : NavKey",
                    ),
                ),
                shellSources = mapOf(
                    "shared/src/commonMain/kotlin/TopLevelDestination.kt" to sharedSource(
                        "navigation",
                        "val TABS = listOf(TopLevelDestination(route = BrowseRoute, label = tab_browse))",
                    ),
                    "shared/src/commonMain/kotlin/AppEntryCallbacks.kt" to sharedSource(
                        "composition",
                        """
                        fun provideBrowseCallbacks(navigator: Navigator) = BrowseEntryCallbacks(
                            onCategoryClick = { navigator.navigate(CategoryRoute (it.value)) },
                        )
                        fun provideFavoritesCallbacks(navigator: Navigator) = FavoritesEntryCallbacks(
                            onBrowse = { navigator.navigate(FavoritesRoute) },
                        )
                        """.trimIndent(),
                    ),
                    "shared/src/commonMain/kotlin/PlaybackRoutes.kt" to sharedSource(
                        "composition",
                        "fun open(kind: String) = when (kind) { STORY -> open(StoriesRoute, StoryRoute(id)) }",
                    ),
                ),
            ),
        )
    }

    /**
     * Pane layout, a reverse mapping, a comparison, an import and a comment all name a route, and
     * none of them opens it. A feature whose only shell edit was its pane metadata is still
     * unreachable.
     */
    @Test
    fun aRouteTheShellOnlyRecognisesIsReported() {
        val violations = FeatureFirstRules.unopenedRouteViolations(
            featureSources = mapOf(
                "feature/sleep-timer/api/src/commonMain/kotlin/SleepTimerNavigation.kt" to featureSource(
                    ".navigation",
                    "data object TimersRoute : NavKey\ndata class TimerRoute(val timerId: String) : NavKey",
                ),
            ),
            shellSources = mapOf(
                "shared/src/commonMain/kotlin/AppEntryMetadata.kt" to sharedSource(
                    "composition",
                    """
                    import com.xwab.app.feature.sleeptimer.navigation.TimerRoute
                    fun metadata(tab: NavKey, route: NavKey) = when (tab) {
                        BrowseRoute,
                        TimersRoute -> when (route) {
                            TimersRoute -> listPane()
                            is TimerRoute -> detailPane()
                            else -> emptyMap()
                        }
                        else -> emptyMap()
                    }
                    fun isTimers(tab: NavKey) = tab == TimersRoute || tab != TimersRoute
                    // navigator.navigate(TimerRoute(id)); route = TimersRoute
                    val hint = "open(TimersRoute)"
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.any { "route TimersRoute" in it })
        assertTrue(violations.any { "route TimerRoute" in it })
        assertTrue(violations.all { "never opens it" in it && "feature/sleep-timer" in it })
    }

    private val corePolicies = mapOf(
        ":core:sound" to policy(true, "SoundPort", ":core:session", ":core:delivery"),
        ":core:story" to policy(true, "StoryPort", ":core:session"),
        ":core:network" to policy(false, "NetworkPort"),
        ":core:delivery" to policy(false, "DeliveryPort", ":core:network"),
        ":core:favorites" to policy(true, "FavoritesPort"),
        ":core:playback" to policy(false, "PlaybackEnginePort"),
        ":core:session" to policy(true, "PlaybackPort", ":core:playback", interfaces = setOf("PlaybackPort", "PlaybackItemResolver")),
    )

    private fun policy(
        featureAccessible: Boolean,
        port: String,
        vararg dependencies: String,
        interfaces: Set<String> = setOf(port),
    ) = CoreModulePolicy("Test capability", featureAccessible, dependencies.toSet(), interfaces)

    private fun dependencyViolations(
        graph: Map<String, List<String>>,
        apiEdges: Map<String, List<String>> = emptyMap(),
    ) = FeatureFirstRules.dependencyViolations(graph, apiEdges, corePolicies)

    private fun corePortViolations(sources: List<FeatureFirstRules.CoreSource>) =
        FeatureFirstRules.corePortViolations(sources, corePolicies.filterKeys { module -> sources.any { it.module == module } })
    private fun coreSource(path: String, packageSuffix: String, declaration: String) =
        FeatureFirstRules.CoreSource(
            path = "core/sample/src/commonMain/kotlin/$path",
            module = ":core:sample",
            packageName = "com.xwab.app.core.sample$packageSuffix",
            source = "package com.xwab.app.core.sample$packageSuffix\n$declaration",
        )

    private fun sharedSource(packageSuffix: String, declarations: String): String =
        "package com.xwab.app.$packageSuffix\n$declarations"

    private fun featureSource(packageSuffix: String, declarations: String): String =
        "package com.xwab.app.feature.browse$packageSuffix\n$declarations"
}
