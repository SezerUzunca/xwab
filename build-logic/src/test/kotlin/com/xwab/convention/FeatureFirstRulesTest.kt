package com.xwab.convention

import kotlin.test.Test
import kotlin.test.assertEquals
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
            mapOf(":core:catalog" to listOf(":feature:home")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("may not depend on a feature"), violations.single())
    }

    @Test
    fun aFeatureDependingOnACoreModuleIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(":feature:home" to listOf(":core:catalog", ":core:favorites")),
            ),
        )
    }

    // Rule 2 — a feature sees only another feature's navigation module.

    @Test
    fun aFeatureReachingAnotherFeaturesImplementationIsAViolation() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":feature:home" to listOf(":feature:player")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains(":feature:player:navigation"), violations.single())
    }

    @Test
    fun aFeatureReachingAnotherFeaturesNavigationModuleIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(":feature:home" to listOf(":feature:player:navigation")),
            ),
        )
    }

    /** A feature's own navigation module is part of the same slice, not a cross-feature edge. */
    @Test
    fun aFeatureDependingOnItsOwnSubmodulesIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.dependencyViolations(
                mapOf(":feature:home" to listOf(":feature:home:navigation")),
            ),
        )
    }

    // Rule 4 — what a screen may not declare.

    @Test
    fun aFeatureDeclaringAnyOffLimitsModuleIsAViolation() {
        FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES.keys.forEach { offLimits ->
            val violations = FeatureFirstRules.dependencyViolations(
                mapOf(":feature:player" to listOf(offLimits)),
            )

            assertEquals(1, violations.size, "expected exactly one violation for $offLimits")
            assertTrue(
                violations.single().startsWith(":feature:player depends on $offLimits."),
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
                    ":core:playback-session" to offLimits,
                    ":shared" to offLimits,
                    ":androidApp" to offLimits,
                ),
            ),
        )
    }

    @Test
    fun aFeaturesNavigationModuleIsHeldToRuleFourToo() {
        val violations = FeatureFirstRules.dependencyViolations(
            mapOf(":feature:player:navigation" to listOf(":core:audio-delivery")),
        )

        assertEquals(1, violations.size)
    }

    // Rule 4's own upkeep.

    @Test
    fun aRuleNamingAModuleThisBuildDoesNotHaveIsItselfAViolation() {
        val violations = FeatureFirstRules.staleRuleViolations(setOf(":core:catalog", ":feature:home"))

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
            useCases = listOf("ObserveHomeContentUseCase" to ":core:catalog"),
            sourcesByFeature = mapOf(
                "home" to listOf("val x = ObserveHomeContentUseCase(get())"),
                "player" to listOf("nothing to see here"),
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("only feature:home uses it"), violations.single())
    }

    @Test
    fun aCoreUseCaseTwoFeaturesUseIsFine() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.leakedUseCaseViolations(
                useCases = listOf("ObserveTrackUseCase" to ":core:catalog"),
                sourcesByFeature = mapOf(
                    "home" to listOf("ObserveTrackUseCase()"),
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
                useCases = listOf("ObserveNothingUseCase" to ":core:catalog"),
                sourcesByFeature = mapOf("home" to listOf("unrelated")),
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

    // The shape the real build has.

    @Test
    fun theGraphThisRepositoryActuallyDeclaresIsClean() {
        val graph = mapOf(
            ":core:catalog" to emptyList<String>(),
            ":core:catalog-manifest" to listOf(":core:catalog"),
            ":core:audio-delivery" to listOf(":core:catalog-manifest"),
            ":core:favorites" to emptyList<String>(),
            ":core:playback-engine" to emptyList<String>(),
            ":core:playback-session" to
                listOf(":core:catalog", ":core:audio-delivery", ":core:playback-engine"),
            ":core:testing" to listOf(":core:catalog", ":core:favorites", ":core:playback-session"),
            ":core:designsystem" to emptyList<String>(),
            ":core:navigation" to emptyList<String>(),
            ":feature:home" to listOf(
                ":core:catalog", ":core:favorites", ":core:playback-session", ":core:testing",
                ":core:designsystem", ":core:navigation",
                ":feature:home:navigation", ":feature:category:navigation", ":feature:player:navigation",
            ),
            ":feature:home:navigation" to listOf(":core:navigation"),
            ":feature:category" to listOf(
                ":core:catalog", ":core:favorites", ":core:playback-session", ":core:testing",
                ":feature:category:navigation", ":feature:player:navigation",
            ),
            ":feature:category:navigation" to listOf(":core:navigation"),
            ":feature:player" to listOf(
                ":core:catalog", ":core:favorites", ":core:playback-session", ":core:testing",
                ":feature:player:navigation",
            ),
            ":feature:player:navigation" to listOf(":core:navigation"),
            ":shared" to listOf(
                ":core:catalog", ":core:catalog-manifest", ":core:audio-delivery", ":core:favorites",
                ":core:playback-session", ":core:playback-engine", ":core:navigation",
                ":core:designsystem", ":feature:home", ":feature:category", ":feature:player",
            ),
            ":androidApp" to listOf(":shared"),
        )

        assertEquals(emptyList(), FeatureFirstRules.staleRuleViolations(graph.keys))
        assertEquals(emptyList(), FeatureFirstRules.dependencyViolations(graph))
    }
}
