package com.xwab.convention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Every rule from both sides.
 *
 * A rule only ever run against a repository that satisfies it has never been shown to reject
 * anything — which is how the source-scanning version of rule 4 could have stopped matching after
 * a rename and gone on reporting success. So each test here pairs a graph that must pass with one
 * that must fail.
 */
class FeatureFirstRulesTest {

    // Rule 1 — dependencies point one way.

    @Test
    fun aCoreModuleDependingOnAFeatureIsAViolation() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":core:sound:catalog" to listOf(":feature:category:impl")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("may not depend on a feature"), violations.single())
    }

    @Test
    fun aFeatureDependingOnACoreModuleIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(":feature:category:impl" to listOf(":core:sound:catalog", ":core:sound:favorites")),
            ),
        )
    }

    // Rule 2 — feature modules have no cross-feature dependency edges.

    @Test
    fun aFeatureReachingAnotherFeaturesImplementationIsAViolation() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":feature:category:impl" to listOf(":feature:sounds:impl")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("must not depend"), violations.single())
    }

    @Test
    fun aFeatureReachingAnotherFeaturesApiModuleIsAViolation() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":feature:category:impl" to listOf(":feature:sounds:api")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("intent callback"), violations.single())
    }

    /** A feature's own API module is part of the same slice, not a cross-feature edge. */
    @Test
    fun aFeatureDependingOnItsOwnSubmodulesIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(":feature:category:impl" to listOf(":feature:category:api")),
            ),
        )
    }

    @Test
    fun theCompositionRootMayConnectFeatureApisAndImplementations() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(
                    ":shared" to listOf(
                        ":feature:browse:impl",
                        ":feature:category:api",
                        ":feature:sounds:api",
                    ),
                ),
            ),
        )
    }

    @Test
    fun aFeatureApiDependingOnItsOwnImplementationIsAViolation() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":feature:category:api" to listOf(":feature:category:impl")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("must remain implementation-free"), violations.single())
    }

    /** A cross-feature edge is reported as isolation, even when it also breaks API purity. */
    @Test
    fun aFeatureApiReachingAnotherFeaturesImplementationIsReportedAsCrossFeature() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":feature:category:api" to listOf(":feature:sounds:impl")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("must not depend"), violations.single())
    }

    // Rule 4 — what a screen may not declare.

    @Test
    fun aFeatureDeclaringAnyOffLimitsModuleIsAViolation() {
        FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES.keys.forEach { offLimits ->
            val violations = FeatureFirstRules.dependencyViolations(
                mapOf(":feature:sounds:impl" to listOf(offLimits)),
            )

            assertEquals(1, violations.size, "expected exactly one violation for $offLimits")
            assertTrue(
                violations.single().startsWith(":feature:sounds:impl depends on $offLimits."),
                violations.single(),
            )
        }
    }

    /**
     * The modules that assemble a session, and the composition root, are exactly who may reach
     * them — so the rule has to stay quiet for everything that is not a feature.
     */
    @Test
    fun theSessionAndTheCompositionRootMayDeclareOffLimitsModules() {
        val offLimits = FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES.keys.toList()

        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(
                    ":core:playback:session" to offLimits,
                    ":shared" to offLimits,
                    ":androidApp" to offLimits,
                ),
            ),
        )
    }

    @Test
    fun aFeaturesApiModuleIsHeldToRuleFourToo() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":feature:sounds:api" to listOf(":core:sound:delivery")),
        )

        assertEquals(1, violations.size)
    }

    // Rule 4 — what a screen can reach, not only what it names.

    /**
     * The hole this closes. `:core:testing` is a module every feature declares, so one `api` edge
     * added to it — a fake resolver would be reason enough — puts delivery on three screens'
     * compile classpaths. Nobody declared anything forbidden, and before the rule followed api
     * edges it reported success.
     */
    @Test
    fun aFeatureReachingAnOffLimitsModuleThroughAnApiEdgeIsAViolation() {
        val violations = FeatureFirstRules.dependencyViolations(
            graph = mapOf(":feature:category:impl" to listOf(":core:testing")),
            apiEdges = mapOf(":core:testing" to listOf(":core:sound:delivery")),
        )

        assertEquals(1, violations.size)
        assertTrue(
            violations.single().startsWith(":feature:category:impl reaches :core:sound:delivery through :core:testing,"),
            violations.single(),
        )
    }

    /** The shape the build actually has: session declares delivery, and keeps it to itself. */
    @Test
    fun aModuleThatKeepsAnOffLimitsDependencyToItselfIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                graph = mapOf(
                    ":feature:category:impl" to listOf(":core:playback:session"),
                    ":core:playback:session" to listOf(":core:sound:delivery"),
                ),
                apiEdges = mapOf(":core:playback:session" to emptyList<String>()),
            ),
        )
    }

    @Test
    fun anApiEdgeIsFollowedAsFarAsItGoes() {
        val violations = FeatureFirstRules.dependencyViolations(
            graph = mapOf(":feature:sounds:impl" to listOf(":core:testing")),
            apiEdges = mapOf(
                ":core:testing" to listOf(":core:playback:session"),
                ":core:playback:session" to listOf(":core:playback:engine"),
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(
            violations.single().contains(":core:testing -> :core:playback:session"),
            violations.single(),
        )
    }

    /** A cycle in api edges must not hang the check. */
    @Test
    fun apiEdgesThatLoopAreWalkedOnce() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                graph = mapOf(":feature:category:impl" to listOf(":core:sound:catalog")),
                apiEdges = mapOf(
                    ":core:sound:catalog" to listOf(":core:testing"),
                    ":core:testing" to listOf(":core:sound:catalog"),
                ),
            ),
        )
    }

    /** Declared *and* reachable reports the declaration: that is the line to delete. */
    @Test
    fun aModuleBothDeclaredAndReachedIsReportedOnce() {
        val violations = FeatureFirstRules.dependencyViolations(
            graph = mapOf(":feature:category:impl" to listOf(":core:sound:delivery", ":core:testing")),
            apiEdges = mapOf(":core:testing" to listOf(":core:sound:delivery")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().startsWith(":feature:category:impl depends on"), violations.single())
    }

    @Test
    fun onlyConfigurationsThatReExportDependenciesCount() {
        listOf("api", "commonMainApi", "androidMainApi", "iosMainApi", "commonTestApi").forEach {
            assertTrue(FeatureFirstRules.isApiConfiguration(it), "$it re-exports its dependencies")
        }
        listOf("implementation", "commonMainImplementation", "apiElements", "compileOnly").forEach {
            assertEquals(false, FeatureFirstRules.isApiConfiguration(it), "$it does not")
        }
    }

    // Rule 5 — navigation may know feature contracts, not feature implementations.

    @Test
    fun navigationImportingAFeatureImplementationIsAViolation() {
        val violations = FeatureFirstRules.navigationImplementationImportViolations(
            mapOf(
                "shared/src/commonMain/kotlin/com/xwab/app/navigation/AppNavigation.kt" to
                    "import com.xwab.app.feature.browse.impl.navigation.browseEntry",
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("composition root"), violations.single())
    }

    @Test
    fun navigationImportingFeatureApisAndNavigationLibrariesIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.navigationImplementationImportViolations(
                mapOf(
                    "shared/src/commonMain/kotlin/com/xwab/app/navigation/AppNavigation.kt" to """
                        import androidx.navigation3.runtime.NavKey
                        import com.xwab.app.feature.browse.api.navigation.BrowseRoute
                    """.trimIndent(),
                ),
            ),
        )
    }

    // Rule 4's own upkeep.

    @Test
    fun aRuleNamingAModuleThisBuildDoesNotHaveIsItselfAViolation() {
        val violations = FeatureFirstRules.staleRuleViolations(setOf(":core:sound:catalog", ":feature:category:impl"))

        assertEquals(FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES.size, violations.size)
        assertTrue(violations.first().contains("protects nothing"), violations.first())
    }

    @Test
    fun aRuleWhoseModulesAllExistIsQuiet() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.staleRuleViolations(FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES.keys),
        )
    }

    // Rule 3 — a shared use case has to serve more than one screen.

    @Test
    fun aCoreUseCaseOnlyOneFeatureUsesIsAViolation() {
        val violations = FeatureFirstRules.leakedUseCaseViolations(
            useCases = listOf("ObserveSoundsContentUseCase" to ":core:sound:catalog"),
            sourcesByFeature = mapOf(
                "category" to listOf("val x = ObserveSoundsContentUseCase(get())"),
                "player" to listOf("nothing to see here"),
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("only feature:category uses it"), violations.single())
    }

    @Test
    fun aCoreUseCaseTwoFeaturesUseIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.leakedUseCaseViolations(
                useCases = listOf("ObserveTrackUseCase" to ":core:sound:catalog"),
                sourcesByFeature = mapOf(
                    "category" to listOf("ObserveTrackUseCase()"),
                    "player" to listOf("ObserveTrackUseCase()"),
                ),
            ),
        )
    }

    /** An unused declaration is a different problem, and not this rule's to report. */
    @Test
    fun aCoreUseCaseNoFeatureUsesIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.leakedUseCaseViolations(
                useCases = listOf("ObserveNothingUseCase" to ":core:sound:catalog"),
                sourcesByFeature = mapOf("browse" to listOf("unrelated")),
            ),
        )
    }

    @Test
    fun theUseCaseRegexFindsDeclarationsAndNotCallSites() {
        val source = """
            package com.xwab.app.core.catalog

            internal class ObserveSomethingUseCase(private val repo: Repo)

            class PublicUseCase

            fun caller() = ObserveSomethingUseCase(repo)
        """.trimIndent()

        assertEquals(
            listOf("ObserveSomethingUseCase", "PublicUseCase"),
            FeatureFirstRules.USE_CASE_DECLARATION.findAll(source).map { it.groupValues[1] }.toList(),
        )
    }

    // Rule 3's other half — which module a source file belongs to.

    /**
     * The reason this function exists. Rule 3 used to take the first directory under `core/`, which
     * now names a grouping directory: every use case in the sound group would have been reported
     * as `:core:sound`, a container project no module can depend on, and the rule would have gone
     * on passing while pointing at it.
     */
    @Test
    fun aSourceInAGroupedModuleBelongsToTheModuleAndNotToTheGroup() {
        assertEquals(
            ":core:sound:catalog",
            FeatureFirstRules.owningModule(
                "core/sound/catalog/src/commonMain/kotlin/com/xwab/app/core/catalog/Music.kt",
                listOf(":core:sound", ":core:sound:catalog", ":core:sound:manifest"),
            ),
        )
    }

    @Test
    fun aSourceInAModuleThatIsNotGroupedIsFound() {
        assertEquals(
            ":core:testing",
            FeatureFirstRules.owningModule(
                "core/testing/src/commonMain/kotlin/com/xwab/app/core/testing/FakeCatalog.kt",
                listOf(":core:sound:catalog", ":core:testing"),
            ),
        )
    }

    @Test
    fun aSourceOutsideEveryModuleBelongsToNone() {
        assertNull(
            FeatureFirstRules.owningModule(
                "gradle/libs.versions.toml",
                listOf(":core:sound", ":core:sound:catalog", ":core:testing"),
            ),
        )
    }

    /**
     * A file sitting directly in a group directory belongs to the container project, which is the
     * honest answer: Gradle really does make `:core:sound` a project. It never comes up in rule 3,
     * because a group directory holds no Kotlin sources — only the modules below it do.
     */
    @Test
    fun aFileDirectlyInAGroupDirectoryBelongsToItsContainerProject() {
        assertEquals(
            ":core:sound",
            FeatureFirstRules.owningModule(
                "core/sound/README.md",
                listOf(":core:sound", ":core:sound:catalog"),
            ),
        )
    }

    /** A module directory is not inside itself; only what is under it belongs to it. */
    @Test
    fun aModulesOwnDirectoryIsNotASourceInIt() {
        assertNull(
            FeatureFirstRules.owningModule(
                "core/sound/catalog",
                listOf(":core:sound:catalog"),
            ),
        )
    }

    // The shape the real build has.

    @Test
    fun theGraphThisRepositoryActuallyDeclaresIsClean() {
        val graph = mapOf(
            // The three grouping directories. Gradle makes a project out of each because a module
            // below them is included; none has a build file and nothing is ever declared on them.
            ":core:sound" to emptyList<String>(),
            ":core:story" to emptyList<String>(),
            ":core:playback" to emptyList<String>(),
            ":core:network" to emptyList<String>(),
            ":core:sound:catalog" to emptyList<String>(),
            ":core:sound:manifest" to listOf(":core:sound:catalog"),
            ":core:sound:delivery" to listOf(":core:sound:manifest", ":core:network"),
            ":core:story:catalog" to emptyList<String>(),
            ":core:story:manifest" to listOf(":core:story:catalog"),
            ":core:sound:favorites" to emptyList<String>(),
            ":core:playback:engine" to emptyList<String>(),
            ":core:playback:session" to listOf(
                ":core:sound:catalog", ":core:sound:delivery",
                ":core:story:catalog", ":core:story:manifest",
                ":core:playback:engine",
            ),
            ":core:testing" to
                listOf(":core:sound:catalog", ":core:sound:favorites", ":core:playback:session"),
            ":core:designsystem" to emptyList<String>(),
            ":feature:browse:impl" to listOf(
                ":core:sound:catalog", ":core:testing",
                ":feature:browse:api",
            ),
            ":feature:browse:api" to emptyList<String>(),
            ":feature:favorites:impl" to listOf(
                ":core:sound:catalog", ":core:sound:favorites", ":core:playback:session", ":core:testing",
                ":feature:favorites:api",
            ),
            ":feature:favorites:api" to emptyList<String>(),
            ":feature:category:impl" to listOf(
                ":core:sound:catalog", ":core:sound:favorites", ":core:playback:session", ":core:testing",
                ":feature:category:api",
            ),
            ":feature:category:api" to emptyList<String>(),
            ":feature:sounds:impl" to listOf(
                ":core:sound:catalog", ":core:sound:favorites", ":core:playback:session", ":core:testing",
                ":feature:sounds:api",
            ),
            ":feature:sounds:api" to emptyList<String>(),
            // The story slice reads two capabilities where a sound screen reads three: there is no
            // favorites port for stories, and the manifest that knows where one streams from is off
            // limits to every feature.
            ":feature:story:impl" to listOf(
                ":core:story:catalog", ":core:playback:session", ":core:testing",
                ":feature:story:api",
            ),
            ":feature:story:api" to emptyList<String>(),
            // The composition root binds every capability and assembles feature implementations.
            // It also sees the feature APIs in test source sets so it can prove route restore.
            ":shared" to listOf(
                ":core:sound:catalog", ":core:sound:manifest", ":core:sound:delivery", ":core:sound:favorites",
                ":core:story:catalog", ":core:story:manifest",
                ":core:playback:session", ":core:playback:engine", ":core:network",
                ":core:designsystem", ":core:testing",
                ":feature:browse:impl", ":feature:browse:api",
                ":feature:favorites:impl", ":feature:favorites:api",
                ":feature:category:impl", ":feature:category:api",
                ":feature:sounds:impl", ":feature:sounds:api",
                ":feature:story:impl", ":feature:story:api",
            ),
            ":androidApp" to listOf(":shared"),
        )

        // What each module re-exports. `:core:playback:session` declares delivery and the engine
        // and re-exports neither, which is the only reason three screens can depend on it.
        val apiEdges = mapOf(
            ":core:sound:manifest" to listOf(":core:sound:catalog"),
            ":core:sound:delivery" to listOf(":core:sound:catalog"),
            ":core:sound:favorites" to listOf(":core:sound:catalog"),
            ":core:story:manifest" to listOf(":core:story:catalog"),
            ":core:playback:session" to emptyList(),
            ":core:testing" to
                listOf(":core:sound:catalog", ":core:sound:favorites", ":core:playback:session"),
        )

        assertEquals(emptyList(), FeatureFirstRules.staleRuleViolations(graph.keys))
        assertEquals(emptyList(), FeatureFirstRules.dependencyViolations(graph, apiEdges))
    }
}
