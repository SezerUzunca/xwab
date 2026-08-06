package com.xwab.convention

/**
 * The feature-first rules, as pure functions over what the build already knows.
 *
 * Separated from [CheckArchitectureTask] so they can be tested without a Gradle build: a rule that
 * only ever runs against a repository which satisfies it has never been shown to *reject*
 * anything, and that is exactly how the source-scanning version of rule 4 could have rotted
 * unnoticed. [FeatureFirstRulesTest] drives every rule from both sides.
 *
 * The task keeps what genuinely needs the file system — walking sources for rule 3 — and hands the
 * results here.
 */
internal object FeatureFirstRules {
    const val CORE_PREFIX = ":core:"
    const val FEATURE_PREFIX = ":feature:"
    const val NAVIGATION_SUFFIX = ":navigation"

    val USE_CASE_DECLARATION =
        Regex("""^\s*(?:internal\s+|public\s+)?class\s+(\w+UseCase)\b""", RegexOption.MULTILINE)

    /**
     * Modules a feature may not declare, and the reason each is off limits.
     *
     * [staleRuleViolations] fails the build if any of these names stops matching a real module, so
     * the rule cannot quietly protect nothing after a rename.
     */
    val MODULES_OFF_LIMITS_TO_FEATURES = mapOf(
        ":core:sound:delivery" to
            "resolving a track to a URI and caching its bytes are the session's business; a " +
                "screen steers playback through PlaybackCoordinator",
        ":core:playback:engine" to
            "the engine's own state model is not a screen's to read; PlaybackSummary is",
        ":core:sound:manifest" to
            "a screen reads the catalog through MusicCatalogRepository in :core:sound:catalog; " +
                "the shipped manifest and the physical source behind each track are not its " +
                "business",
        ":core:story:manifest" to
            "a screen reads stories through StoryCatalogRepository in :core:story:catalog; the " +
                "story list and the source each story streams from are not its business",
    )

    /**
     * The one way rule 4 could rot: [MODULES_OFF_LIMITS_TO_FEATURES] names modules, and a renamed
     * module would leave the rule quietly matching nothing at all — which is exactly how the
     * source-scanning version it replaced would have failed. So the names are checked against the
     * graph, and a rule that has stopped applying is itself a violation.
     */
    fun staleRuleViolations(modules: Set<String>): List<String> =
        (MODULES_OFF_LIMITS_TO_FEATURES.keys - modules).sorted().map { missing ->
            "Rule 4 names $missing, which is not a module in this build. Update " +
                "FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES, or the rule protects nothing."
        }

    /** Rules 1, 2 and 4, all of which are readable straight off the dependency graph. */
    fun dependencyViolations(graph: Map<String, List<String>>): List<String> {
        val violations = mutableListOf<String>()

        graph.forEach { (module, dependencies) ->
            dependencies.forEach { dependency ->
                if (module.startsWith(CORE_PREFIX) && dependency.startsWith(FEATURE_PREFIX)) {
                    violations += "$module depends on $dependency. A core module may not depend on a feature."
                }

                val isCrossFeature = module.startsWith(FEATURE_PREFIX) &&
                    dependency.startsWith(FEATURE_PREFIX) &&
                    featureOf(module) != featureOf(dependency)

                if (isCrossFeature && !dependency.endsWith(NAVIGATION_SUFFIX)) {
                    violations += "$module depends on $dependency. Depend on " +
                        "$dependency$NAVIGATION_SUFFIX instead: a feature's navigation module is " +
                        "the only part of it another feature may see."
                }

                MODULES_OFF_LIMITS_TO_FEATURES[dependency]?.let { reason ->
                    if (module.startsWith(FEATURE_PREFIX)) {
                        violations += "$module depends on $dependency. A feature may not: $reason."
                    }
                }
            }
        }

        return violations.distinct().sorted()
    }

    /**
     * Rule 3, over sources the task has already read.
     *
     * @param useCases every `*UseCase` class declared under `core/`, paired with the module that
     *   declares it.
     * @param sourcesByFeature feature directory name to the text of every Kotlin source under it.
     */
    fun leakedUseCaseViolations(
        useCases: List<Pair<String, String>>,
        sourcesByFeature: Map<String, List<String>>,
    ): List<String> = useCases.mapNotNull { (useCase, module) ->
        val users = sourcesByFeature.filterValues { sources -> sources.any { it.contains(useCase) } }.keys
        if (users.size != 1) return@mapNotNull null

        "$module declares $useCase, but only feature:${users.single()} uses it. " +
            "Move it into that feature's own `domain` package, or leave it here once a " +
            "second feature needs it."
    }.sorted()

    /** `:feature:home` and `:feature:home:navigation` are both the `home` feature. */
    fun featureOf(modulePath: String): String =
        modulePath.removePrefix(FEATURE_PREFIX).substringBefore(':')

    /**
     * Which module a source file belongs to, given every module path in the build.
     *
     * Rule 3 used to read this off the first directory under `core/`, which was right only while
     * every core module sat directly there. Core modules are grouped now — `core/sound/catalog` is
     * `:core:sound:catalog` — and that shortcut would have attributed every use case in the group
     * to `:core:sound`, a container project that declares nothing. It would not have *failed*: the
     * rule would have kept reporting success while naming a module that cannot be depended on.
     *
     * So the owner is the longest module path that the file actually sits inside, and a file under
     * no module at all belongs to none. A file lying directly in a group directory answers with
     * the container project, which is what it is in; rule 3 never sees one, because Kotlin sources
     * live in the modules below a group and never in the group itself.
     *
     * @param relativeSourcePath a source file's path from the repository root, `/`-separated.
     * @param modulePaths the Gradle paths of every module in the build.
     */
    fun owningModule(relativeSourcePath: String, modulePaths: Collection<String>): String? =
        modulePaths
            .filter { relativeSourcePath.startsWith("${directoryOf(it)}/") }
            .maxByOrNull { it.length }

    /** `:core:sound:catalog` lives in `core/sound/catalog`: a Gradle path is a directory path. */
    private fun directoryOf(modulePath: String): String =
        modulePath.removePrefix(":").replace(':', '/')
}
