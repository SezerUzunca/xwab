package com.xwab.convention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Every architecture rule is driven once from its accepting and rejecting side. */
class FeatureFirstRulesTest {

    @Test
    fun coreDependenciesPointOutwardNeverTowardFeatures() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":core:sound" to listOf(":feature:category")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("may not depend on a feature"))
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(":feature:category" to listOf(":core:sound")),
            ),
        )
    }

    @Test
    fun featuresNeverDependOnOtherFeatures() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":feature:category" to listOf(":feature:sounds")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("connect destination intents in :shared"))
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(":shared" to listOf(":feature:category", ":feature:sounds")),
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
        FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES.keys.forEach { offLimits ->
            val violations = FeatureFirstRules.dependencyViolations(
                mapOf(":feature:sounds" to listOf(offLimits)),
            )
            assertEquals(1, violations.size, offLimits)
        }

        val transitive = FeatureFirstRules.dependencyViolations(
            graph = mapOf(":feature:sounds" to listOf(":testing")),
            apiEdges = mapOf(":testing" to listOf(":core:delivery")),
        )
        assertEquals(1, transitive.size)
        assertTrue(transitive.single().contains("through :testing"))

        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(":core:session" to FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES.keys.toList()),
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
    fun sharedUsesFeatureContractsOnlyAtTheirApplicationBoundaries() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.sharedFeatureReferenceViolations(
                mapOf(
                    "shared/src/commonMain/kotlin/AppNavigation.kt" to sharedSource(
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

    @Test
    fun staleOffLimitsModuleNamesFailLoudly() {
        assertEquals(
            FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES.size,
            FeatureFirstRules.staleRuleViolations(setOf(":feature:category")).size,
        )
        assertEquals(
            emptyList(),
            FeatureFirstRules.staleRuleViolations(FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES.keys),
        )
    }

    @Test
    fun featureSpecificUseCasesStayInTheirFeature() {
        val violations = FeatureFirstRules.leakedUseCaseViolations(
            useCases = listOf("ObserveSoundsContentUseCase" to ":core:sound"),
            sourcesByFeature = mapOf(
                "category" to listOf("ObserveSoundsContentUseCase()"),
                "sounds" to listOf("unrelated"),
            ),
        )
        assertEquals(1, violations.size)

        assertEquals(
            emptyList(),
            FeatureFirstRules.leakedUseCaseViolations(
                useCases = listOf("SharedUseCase" to ":core:sound"),
                sourcesByFeature = mapOf(
                    "category" to listOf("SharedUseCase()"),
                    "sounds" to listOf("SharedUseCase()"),
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
    fun corePublicSurfaceIsExplicitAndConfinedToPortPackages() {
        val good = listOf(
            coreSource("port/SoundPort.kt", ".port", "public interface SoundPort"),
            coreSource("port/Outcome.kt", ".port", "public sealed interface Outcome"),
            coreSource("ManifestAdapter.kt", "", "internal class ManifestAdapter"),
        )
        assertEquals(emptyList(), FeatureFirstRules.coreVisibilityViolations(good))

        // A port is public by definition; Kotlin's own default visibility already says so without
        // a keyword, so a declaration that omits one is not a violation.
        val implicitPort = FeatureFirstRules.coreVisibilityViolations(
            listOf(coreSource("port/SoundPort.kt", ".port", "interface SoundPort")),
        )
        assertEquals(emptyList(), implicitPort)

        // The naming rule still applies whether or not `public` was written out.
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
                public interface SamplePort {
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
                public class Sample {
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
            listOf(coreSource("port/Catalog.kt", ".port", "public interface Catalog")),
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
        )

        val violations = FeatureFirstRules.legacyCoreAbstractionViolations(sources)
        assertEquals(3, violations.size)
        assertTrue(violations.all { it.contains("feature-local") })
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
            assertEquals(emptyList(), FeatureFirstRules.contentPortViolations(
                listOf(source("interface $port")),
            ))
            assertEquals(1, FeatureFirstRules.contentPortViolations(
                listOf(source("interface $port\ninterface ExtraPort")),
            ).size)
            assertEquals(1, FeatureFirstRules.contentPortViolations(
                listOf(source("interface $port {\n    interface ExtraPort\n}")),
            ).size)
            assertEquals(emptyList(), FeatureFirstRules.contentPortViolations(
                listOf(source("interface $port {\n    data class Model(val id: String)\n}")),
            ))
            assertEquals(1, FeatureFirstRules.contentPortViolations(
                listOf(source("data class Model(val id: String)")),
            ).size)
        }
    }

    @Test
    fun reusableCapabilitiesCannotDependOnContentModules() {
        for (module in listOf(":core:favorites", ":core:delivery")) {
            for (dependency in listOf(":core:sound", ":core:story", ":core:session", ":shared")) {
                assertTrue(FeatureFirstRules.dependencyViolations(mapOf(module to listOf(dependency)))
                    .any { it.contains("must not depend on app content") })
            }
        }
        assertEquals(emptyList(), FeatureFirstRules.dependencyViolations(mapOf(
            ":core:favorites" to listOf(":core:favorites"),
            ":core:delivery" to listOf(":core:delivery", ":core:network"),
        )))
    }

    @Test
    fun currentModuleGraphSatisfiesDependencyRules() {
        val graph = mapOf(
            ":core:sound" to emptyList<String>(),
            ":core:story" to emptyList<String>(),
            ":core:network" to emptyList<String>(),
            ":core:delivery" to listOf(":core:network"),
            ":core:favorites" to emptyList<String>(),
            ":core:playback" to emptyList<String>(),
            ":core:session" to listOf(
                ":core:sound", ":core:delivery", ":core:story",
                ":core:playback",
            ),
            ":designsystem" to emptyList<String>(),
            ":testing" to listOf(":core:sound", ":core:favorites", ":core:session"),
            ":feature:browse" to listOf(":core:sound", ":testing", ":designsystem"),
            ":feature:category" to listOf(
                ":core:sound", ":core:favorites", ":core:session",
                ":testing", ":designsystem",
            ),
            ":feature:favorites" to listOf(
                ":core:sound", ":core:favorites", ":core:session",
                ":testing", ":designsystem",
            ),
            ":feature:sounds" to listOf(
                ":core:sound", ":core:favorites", ":core:session",
                ":testing", ":designsystem",
            ),
            ":feature:story" to listOf(
                ":core:story", ":core:session", ":testing", ":designsystem",
            ),
            ":shared" to listOf(
                ":core:sound", ":core:delivery",
                ":core:favorites", ":core:story",
                ":core:session", ":core:playback", ":core:network",
                ":designsystem", ":testing", ":feature:browse", ":feature:category",
                ":feature:favorites", ":feature:sounds", ":feature:story",
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
        assertEquals(emptyList(), FeatureFirstRules.featureModuleShapeViolations(graph.keys))
        assertEquals(emptyList(), FeatureFirstRules.dependencyViolations(graph, apiEdges))
    }

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
