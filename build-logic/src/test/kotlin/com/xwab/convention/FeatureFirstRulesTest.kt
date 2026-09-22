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
            mapOf(":feature:category" to listOf(":feature:sound")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("connect destination intents in :shared"))
        assertEquals(
            emptyList(),
            dependencyViolations(
                mapOf(":shared" to listOf(":feature:category", ":feature:sound")),
            ),
        )
    }

    @Test
    fun everyFeatureIsExactlyOneGradleModule() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.featureModuleShapeViolations(
                setOf(":feature", ":feature:browse", ":core:sound"),
            ),
        )

        val violations = FeatureFirstRules.featureModuleShapeViolations(
            setOf(":feature:browse", ":feature:browse:api", ":feature:browse:impl"),
        )
        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("exactly one") })
    }

    @Test
    fun apiImplSourceDirectoriesCannotReturn() {
        val violations = FeatureFirstRules.legacySplitDirectoryViolations(
            listOf(
                "feature/browse/api",
                "feature/browse/src/commonMain/kotlin/com/xwab/app/feature/browse/navigation",
                "core/network/src/commonMain/kotlin/com/xwab/app/core/network/impl",
            ),
        )

        assertEquals(2, violations.size)
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
        (corePolicies.filterValues { !it.featureAccessible }.keys + FeatureFirstRules.SHELL_MODULE).forEach { offLimits ->
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
                    "shared/src/commonMain/kotlin/AppEntryProvider.kt" to sharedSource(
                        "composition",
                        "import com.xwab.app.feature.browse.navigation.browseEntry as entry",
                    ),
                    "shared/src/androidMain/kotlin/AndroidAppGraph.kt" to sharedSource(
                        "di",
                        "import com.xwab.app.feature.browse.di.BrowseDependencies",
                    ),
                    "shared/src/iosMain/kotlin/IosAppGraph.kt" to sharedSource(
                        "di",
                        "internal val dependencies: com.xwab.app.feature.story.di.StoriesDependencies? = null",
                    ),
                ),
            ),
        )
    }

    @Test
    fun sharedImplementationReferencesFailInEveryPackageAndPlatform() {
        val violations = FeatureFirstRules.sharedFeatureReferenceViolations(
            mapOf(
                "shared/src/commonMain/kotlin/Navigation.kt" to sharedSource(
                    "navigation",
                    "import com.xwab.app.feature.browse.BrowseScreen",
                ),
                "shared/src/commonMain/kotlin/Composition.kt" to sharedSource(
                    "composition",
                    "import com.xwab.app.feature.browse.di.BrowseDependencies",
                ),
                "shared/src/commonMain/kotlin/Ui.kt" to sharedSource(
                    "ui",
                    "import com.xwab.app.feature.browse.navigation.BrowseRoute",
                ),
                "shared/src/androidMain/kotlin/Platform.kt" to sharedSource(
                    "ui",
                    "internal val screen = com.xwab.app.feature.browse.BrowseScreen()",
                ),
                "shared/src/iosMain/kotlin/Graph.kt" to sharedSource(
                    "di",
                    "import com.xwab.app.feature.browse.BrowseViewModel as ScreenModel",
                ),
                "shared/src/commonMain/kotlin/DiWildcard.kt" to sharedSource(
                    "di",
                    "import com.xwab.app.feature.browse.di.*",
                ),
                "shared/src/commonMain/kotlin/RootWildcard.kt" to sharedSource(
                    "navigation",
                    "import com.xwab.app.feature.browse.*",
                ),
            ),
        )
        assertEquals(7, violations.size)
        assertTrue(violations.any { it.contains("androidMain") })
        assertTrue(violations.any { it.contains("iosMain") })
        assertTrue(violations.all { it.contains("Other shared packages may not reference features") })
    }

    @Test
    fun sharedReferenceChecksIgnoreCommentsAndStringsButKeepTrailingCommentImports() {
        val allowed = sharedSource(
            "ui",
            "// import com.xwab.app.feature.browse.BrowseScreen\n" +
                "/*\nimport com.xwab.app.feature.browse.BrowseViewModel\n*/\n" +
                "internal val docs = \"com.xwab.app.feature.browse.BrowseScreen\"\n" +
                "internal val example = \"\"\"\n" +
                "import com.xwab.app.feature.browse.BrowseScreen\n\"\"\"",
        )
        assertEquals(
            emptyList(),
            FeatureFirstRules.sharedFeatureReferenceViolations(mapOf("Ui.kt" to allowed)),
        )

        val forbidden = sharedSource(
            "ui",
            "import com.xwab.app.feature.browse.BrowseScreen // implementation leak",
        )
        assertEquals(
            1,
            FeatureFirstRules.sharedFeatureReferenceViolations(mapOf("Ui.kt" to forbidden)).size,
        )
    }

    @Test
    fun featurePublicSurfaceIsLimitedToNavigationAndDependencyBags() {
        val sources = mapOf(
            "feature/browse/src/commonMain/kotlin/Navigation.kt" to featureSource(
                ".navigation",
                """
                    data object BrowseRoute : NavKey
                    val browseNavigationSerializers = SerializersModule {}
                    fun EntryProviderScope<NavKey>.browseEntry(dependencies: BrowseDependencies) {}
                """.trimIndent(),
            ),
            "feature/browse/src/commonMain/kotlin/Dependencies.kt" to featureSource(
                ".di",
                """
                    @Inject
                    class BrowseDependencies(
                        internal val catalog: SoundPort,
                    )
                    internal class Helper {
                        fun localMember() = Unit
                    }
                """.trimIndent(),
            ),
            "feature/browse/src/commonMain/kotlin/Screen.kt" to featureSource(
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
            "feature/browse/src/commonMain/kotlin/ViewModel.kt" to
                featureSource("", "public class BrowseViewModel"),
            "feature/browse/src/androidMain/kotlin/Screen.kt" to
                featureSource("", "@Composable fun BrowseScreen() = Unit"),
            "feature/browse/src/commonMain/kotlin/State.kt" to
                featureSource("", "data class BrowseState(val loading: Boolean)"),
            "feature/browse/src/commonMain/kotlin/UseCase.kt" to
                featureSource(".domain", "class BrowseUseCase"),
            "feature/browse/src/commonMain/kotlin/Factory.kt" to
                featureSource(".di", "class BrowseViewModelFactory"),
            "feature/browse/src/commonMain/kotlin/NavigationHelper.kt" to
                featureSource(".navigation.internal", "class NavigationHelper"),
        )
        assertEquals(6, FeatureFirstRules.featureVisibilityViolations(leaks).size)
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
        val modules = FeatureFirstRules.INDEPENDENT_SUPPORT_MODULES + FeatureFirstRules.SHELL_MODULE
        assertEquals(emptyList(), FeatureFirstRules.staleRuleViolations(modules))
        assertEquals(2, FeatureFirstRules.staleRuleViolations(emptySet()).size)
        assertTrue(FeatureFirstRules.staleRuleViolations(modules - ":designsystem")
            .single().contains("INDEPENDENT_SUPPORT_MODULES"))
        assertTrue(FeatureFirstRules.staleRuleViolations(modules - ":shared")
            .single().contains("SHELL_MODULE"))
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
            for (dependency in listOf(":core:sound", ":core:story", ":core:session", ":shared")) {
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
        val graph = mapOf(
            ":core:sound" to listOf(":core:session", ":core:delivery"),
            ":core:story" to listOf(":core:session"),
            ":core:network" to emptyList<String>(),
            ":core:delivery" to listOf(":core:network"),
            ":core:favorites" to emptyList<String>(),
            ":core:playback" to emptyList<String>(),
            ":core:session" to listOf(":core:playback"),
            ":designsystem" to emptyList<String>(),
            ":testing" to listOf(":core:sound", ":core:favorites", ":core:session"),
            ":feature:browse" to listOf(":core:sound", ":designsystem"),
            ":feature:category" to listOf(
                ":core:sound", ":core:favorites", ":core:session",
                ":designsystem",
            ),
            ":feature:favorites" to listOf(
                ":core:sound", ":core:favorites", ":core:session",
                ":designsystem",
            ),
            ":feature:sound" to listOf(
                ":core:sound", ":core:favorites", ":core:session",
                ":designsystem",
            ),
            ":feature:story" to listOf(
                ":core:story", ":core:session", ":designsystem",
            ),
            ":feature:nowplaying" to listOf(":core:session", ":designsystem"),
            ":shared" to listOf(
                ":core:sound", ":core:delivery",
                ":core:favorites", ":core:story",
                ":core:session", ":core:playback", ":core:network",
                ":designsystem", ":feature:browse", ":feature:category",
                ":feature:favorites", ":feature:sound", ":feature:story", ":feature:nowplaying",
            ),
            ":androidApp" to listOf(":shared"),
        )
        val apiEdges = mapOf(
            ":core:delivery" to emptyList(),
            ":core:favorites" to emptyList(),
            ":core:session" to emptyList(),
            ":testing" to listOf(":core:sound", ":core:favorites", ":core:session"),
        )

        assertEquals(emptyList(), FeatureFirstRules.staleRuleViolations(graph.keys))
        assertEquals(emptyList(), FeatureFirstRules.corePolicyViolations(graph.keys, corePolicies))
        assertEquals(emptyList(), FeatureFirstRules.unwiredModuleViolations(graph))
        assertEquals(emptyList(), FeatureFirstRules.featureModuleShapeViolations(graph.keys))
        assertEquals(emptyList(), dependencyViolations(graph, apiEdges))
    }

    @Test
    fun supportModulesCannotAcquireApplicationDependencies() {
        for (module in listOf(":designsystem")) {
            for (dependency in listOf(":feature:sound", ":core:sound", ":shared", ":testing")) {
                assertTrue(dependencyViolations(mapOf(module to listOf(dependency)))
                    .any { it.contains("must remain independent") })
            }
            assertEquals(emptyList(), dependencyViolations(mapOf(module to listOf(module))))
        }
    }

    @Test
    fun coreCannotDependOnUiOrTheShell() {
        for (dependency in listOf(":designsystem", ":shared")) {
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
    fun aCapabilityOrScreenTheShellNeverDeclaresIsReported() {
        val violations = FeatureFirstRules.unwiredModuleViolations(
            mapOf(
                FeatureFirstRules.SHELL_MODULE to listOf(":core:sound"),
                ":core:sound" to emptyList(),
                ":core:meditation" to emptyList(),
                ":feature:browse" to emptyList(),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.any { it.contains(":core:meditation") })
        assertTrue(violations.any { it.contains(":feature:browse") })
    }

    /** Support modules are not capabilities; the rule is scoped to core and feature on purpose. */
    @Test
    fun theShellNeedNotDeclareSupportModules() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.unwiredModuleViolations(
                mapOf(
                    FeatureFirstRules.SHELL_MODULE to listOf(":core:sound", ":feature:browse"),
                    ":core:sound" to emptyList(),
                    ":feature:browse" to emptyList(),
                    ":testing" to emptyList(),
                    ":androidApp" to listOf(FeatureFirstRules.SHELL_MODULE),
                ),
            ),
        )
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
