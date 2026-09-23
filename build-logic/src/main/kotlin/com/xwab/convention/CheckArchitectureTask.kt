package com.xwab.convention

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Runs the rules that keep feature slices independent and core capabilities port-only. They are
 * easy to break by
 * accident and none of them fail to compile, which is why they are checked rather than written
 * down.
 *
 * 1. A core module may not depend on a feature. Dependencies point one way.
 * 2. A feature may not depend on another feature module. Cross-feature navigation is application
 *    policy: an entry provider exposes an intent callback and `:shared` connects it to a route.
 * 3. A feature is exactly one `:feature:<name>` module; nested `api` / `impl` projects are invalid.
 * 4. A use case in a core module must serve more than one feature. A screen-specific one belongs
 *    to that screen's module, otherwise screen logic leaks into shared capabilities.
 * 5. Each core owns an architecture.properties file declaring its responsibility, feature access,
 *    exhaustive project dependency boundary and complete set of public callable interfaces.
 * 6. All shared production source sets may reference features only at the navigation/composition
 *    boundary (navigation contracts) or DI boundary (Dependencies classes).
 * 7. A core capability exposes declarations only from an explicit `port` package; everything else
 *    is internal or private.
 * 8. References crossing between core modules target only `port` packages.
 * 9. Core declares no repository/provider abstractions; a feature may own one if it truly needs it.
 * 10. Koin and physical `api` / `impl` source layouts may not return; Metro and cohesive modules
 *     are project-wide decisions.
 * 11. Features expose only navigation contracts and DI Dependencies classes; implementation
 *     declarations stay internal or private.
 * 12. Designsystem has no application project dependencies; core cannot
 *     depend on it or on the app shell.
 * 13. Loading/Ready state types stay inside feature modules. Whether a screen has content yet is
 *     that screen's own question, not a vocabulary every feature has to share.
 * 14. Every directory holding a build script is a module in the build.
 * 15. Every core and feature module is a direct dependency of `:shared`. Metro aggregates
 *     contributions from the compile classpath, so a capability the shell does not declare reaches
 *     no graph, and a feature it does not declare is in no app — neither fails to build.
 * 16. Every feature route declares an explicit `@SerialName`. The name is what a saved back stack
 *     holds, so left implicit it follows the package and moving the file breaks every restore.
 * 17. Every playback kind a content module registers a resolver under has a route in the app
 *     shell. Kinds are open strings, so the exhaustive `when` that used to guarantee this is gone.
 * 18. Core dependencies are acyclic, including production self dependencies. Test configurations
 *     are excluded from both dependency graphs.
 * 19. Public port contracts cannot reference their own implementation packages. Each flat core
 *     module owns only its matching package namespace.
 * 20. Optional adapterOnlyTypes stay out of feature code and public consumer port contracts;
 *     their owner's marked types may refer to one another in their own source file.
 * 21. A core module that implements another's adapterOnlyTypes may not reference that module's
 *     remaining publicInterfaces. Answering a capability's contract and calling it are different
 *     roles, and one module holding both puts the coordination back where it was moved from.
 * 22. Optional wireFormat pins each value a capability has written onto devices — playback kinds,
 *     favourites and cache namespaces. Renaming one compiles, passes and breaks installed copies,
 *     so the constant and its pin must change together. Any `*_NAMESPACE` / `*_KIND` constant must
 *     be pinned.
 *
 * The rules themselves live in [FeatureFirstRules], where they are unit-tested from both sides.
 * This task is only their plumbing: it reads the dependency report each module publishes about
 * itself, and the source/configuration files.
 *
 * Rule 5 is graph-based: adapter-only capabilities are represented by real dependency edges, so
 * enforcement survives implementation renames and follows re-exported dependencies as well.
 */
abstract class CheckArchitectureTask : DefaultTask() {

    /**
     * One [ModuleDependencyReport] per module: its production project dependencies, since test
     * fixtures do not change its ABI, and the `api` subset of them — the dependencies that do not
     * stop at the module declaring them. Rule 5 follows the second, so a forbidden module cannot
     * reach a screen by being re-exported from a module the screen is allowed to declare.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val dependencyReports: ConfigurableFileCollection

    /** The repository root; source-level rules read Kotlin files under it. */
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @TaskAction
    fun check() {
        val (graph, apiGraph) = ModuleDependencyReport.graphsOf(
            dependencyReports.files.sortedBy { it.path }.map { ModuleDependencyReport.parse(it.readText()) },
        )
        val root = repositoryRoot.get().asFile
        val coreSources = coreProductionSources(root, graph.keys)
        val policyResults = graph.keys.filter { it.startsWith(FeatureFirstRules.CORE_PREFIX) }
            .associateWith { module ->
                val file = root.resolve(module.removePrefix(":").replace(':', '/'))
                    .resolve("architecture.properties")
                parseCoreModulePolicy(module, file.takeIf(File::isFile)?.readText())
            }
        val policies = policyResults.mapNotNull { (module, result) ->
            result.policy?.let { module to it }
        }.toMap()
        val violations = policyResults.values.flatMap { it.violations } +
            FeatureFirstRules.staleRuleViolations(graph.keys) +
            FeatureFirstRules.corePolicyViolations(graph.keys, policies) +
            FeatureFirstRules.coreModuleShapeViolations(graph.keys) +
            FeatureFirstRules.corePackageOwnershipViolations(coreSources) +
            FeatureFirstRules.unregisteredModuleViolations(moduleDirectories(root), graph.keys) +
            FeatureFirstRules.unwiredModuleViolations(graph) +
            FeatureFirstRules.routeSerialNameViolations(productionSources(root, "feature")) +
            FeatureFirstRules.unroutedPlaybackKindViolations(
                coreSources = productionSources(root, "core"),
                compositionSources = productionSources(root, "shared"),
            ) +
            FeatureFirstRules.featureModuleShapeViolations(graph.keys) +
            FeatureFirstRules.legacySplitDirectoryViolations(legacySplitDirectories(root)) +
            FeatureFirstRules.koinUsageViolations(architectureTextSources(root)) +
            FeatureFirstRules.userAgentAgreementViolations(clientIdentitySources(root)) +
            FeatureFirstRules.dependencyViolations(graph, apiGraph, policies) +
            leakedUseCaseViolations(root, graph.keys) +
            FeatureFirstRules.sharedFeatureReferenceViolations(productionSources(root, "shared")) +
            FeatureFirstRules.featureVisibilityViolations(productionSources(root, "feature")) +
            FeatureFirstRules.featureStateViolations(nonFeatureProductionSources(root)) +
            FeatureFirstRules.lazyListKeyViolations(
                productionSources(root, "feature") + productionSources(root, "shared"),
            ) +
            FeatureFirstRules.corePortViolations(coreSources, policies) +
            FeatureFirstRules.adapterOnlyTypeViolations(coreSources, productionSources(root, "feature"), policies) +
            FeatureFirstRules.contributorPortViolations(coreSources, policies) +
            FeatureFirstRules.wireFormatViolations(coreSources, policies) +
            FeatureFirstRules.coreVisibilityViolations(coreSources, policies) +
            FeatureFirstRules.coreImportViolations(coreSources) +
            FeatureFirstRules.legacyCoreAbstractionViolations(coreSources)

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Feature-first rules broken (${violations.size}):")
                    violations.forEach { appendLine("  - $it") }
                }.trimEnd(),
            )
        }

        logger.lifecycle("Feature-first rules hold across ${graph.size} modules.")
    }

    /** Only production source sets participate; tests may expose fixtures without changing ABI. */
    private fun coreProductionSources(
        root: File,
        modulePaths: Set<String>,
    ): List<FeatureFirstRules.CoreSource> {
        val packageDeclaration = Regex("""^\s*package\s+([A-Za-z0-9_.]+)\s*$""", RegexOption.MULTILINE)
        return kotlinSourcesIn(root.resolve("core")).mapNotNull { file ->
            val path = file.relativeTo(root).invariantSeparatorsPath
            val sourceSet = path.substringAfter("/src/", missingDelimiterValue = "")
                .substringBefore('/')
            if (!sourceSet.endsWith("Main")) return@mapNotNull null

            val module = FeatureFirstRules.owningModule(path, modulePaths) ?: return@mapNotNull null
            val text = file.readText()
            val packageName = packageDeclaration.find(text)?.groupValues?.get(1)
                .orEmpty()
            FeatureFirstRules.CoreSource(path, module, packageName, text)
        }
    }

    /**
     * Reads what rule 4 needs off the file system, then hands it to [FeatureFirstRules].
     *
     * @param modulePaths every module in the build, used to attribute each source to its owner.
     */
    private fun leakedUseCaseViolations(root: File, modulePaths: Set<String>): List<String> {
        val coreRoot = root.resolve("core")
        if (!coreRoot.isDirectory) return emptyList()

        val useCases = kotlinSourcesIn(coreRoot)
            .flatMap { file ->
                val module = FeatureFirstRules.owningModule(
                    file.relativeTo(root).invariantSeparatorsPath,
                    modulePaths,
                ) ?: return@flatMap emptyList<Pair<String, String>>()

                FeatureFirstRules.USE_CASE_DECLARATION.findAll(file.readText())
                    .map { match -> match.groupValues[1] to module }
                    .toList()
            }
            .distinct()
        if (useCases.isEmpty()) return emptyList()

        val featureDirs = root.resolve("feature").listFiles().orEmpty().filter { it.isDirectory }
        val sourcesByFeature = featureDirs.associate { dir ->
            dir.name to kotlinSourcesIn(dir).map { it.readText() }
        }

        return FeatureFirstRules.leakedUseCaseViolations(useCases, sourcesByFeature)
    }

    /** Includes commonMain and every platform Main source set, excluding test fixtures. */
    private fun productionSources(root: File, directory: String): Map<String, String> =
        kotlinSourcesIn(root.resolve(directory))
            .filter { file ->
                file.relativeTo(root).invariantSeparatorsPath
                    .substringAfter("/src/", missingDelimiterValue = "")
                    .substringBefore('/')
                    .endsWith("Main")
            }
            .associate { file ->
                file.relativeTo(root).invariantSeparatorsPath to file.readText()
            }

    /**
     * Every production source outside `feature/` — the modules a screen's own state must not
     * move into. `androidApp` is included because it is a place a shared presentation type could
     * land without any other rule noticing.
     */
    private fun nonFeatureProductionSources(root: File): Map<String, String> =
        listOf("androidApp", "core", "designsystem", "shared", "testing")
            .fold(emptyMap<String, String>()) { sources, directory ->
                sources + productionSources(root, directory)
            }


    /**
     * Every directory that carries a build script, which is what a module looks like on disk.
     *
     * `build-logic` is skipped because it is an included build with a settings file of its own, and
     * the repository root because its script configures the build rather than a module.
     */
    private fun moduleDirectories(root: File): List<String> =
        root.walkTopDown()
            .onEnter { directory ->
                directory == root || directory.name !in setOf(
                    ".git",
                    ".gradle",
                    ".idea",
                    ".claude",
                    ".agents",
                    "build",
                    "build-logic",
                )
            }
            .filter { it.isDirectory && it != root && it.resolve("build.gradle.kts").isFile }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .toList()
    private fun legacySplitDirectories(root: File): List<String> =
        listOf(root.resolve("core"), root.resolve("feature")).flatMap { sourceRoot ->
            if (!sourceRoot.isDirectory) return@flatMap emptyList()
            sourceRoot.walkTopDown()
                .onEnter { it.name != "build" }
                .filter { it.isDirectory && it.name in setOf("api", "impl") }
                .map { it.relativeTo(root).invariantSeparatorsPath }
                .toList()
        }

    /**
     * Download identity plus the Android manifest and iOS bundle settings used by native players.
     */
    private fun clientIdentitySources(root: File): Map<String, String> =
        textSourcesIn(root, setOf("kt", "xml", "plist"))

    private fun architectureTextSources(root: File): Map<String, String> =
        textSourcesIn(root, setOf("kt", "kts", "toml"))

    /** Every file of the given kinds under the repository, minus the directories nothing authors. */
    private fun textSourcesIn(root: File, extensions: Set<String>): Map<String, String> =
        root.walkTopDown()
            .onEnter { directory ->
                directory == root || directory.name !in setOf(
                    ".git",
                    ".gradle",
                    ".idea",
                    ".claude",
                    ".agents",
                    "build",
                )
            }
            .filter { file -> file.isFile && file.extension in extensions }
            .associate { file ->
                file.relativeTo(root).invariantSeparatorsPath to file.readText()
            }

    /** Kotlin sources under [dir], skipping Gradle output so generated code is never read. */
    private fun kotlinSourcesIn(dir: File): List<File> = dir.walkTopDown()
        .onEnter { it.name != "build" }
        .filter { it.isFile && it.extension == "kt" }
        .toList()
}
