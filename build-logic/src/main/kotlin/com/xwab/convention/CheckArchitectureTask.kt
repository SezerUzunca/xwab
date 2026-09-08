package com.xwab.convention

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
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
 * 5. A feature may not declare — or reach through an `api` dependency — a module in
 *    [FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES].
 *    Fetching audio and driving a platform player are done on a screen's behalf. Content-port
 *    checks also enforce one cohesive port per sound/story module.
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
 *
 * The rules themselves live in [FeatureFirstRules], where they are unit-tested from both sides.
 * This task is only their plumbing: it collects the dependency graph and source/configuration files.
 *
 * Rule 5 is graph-based: adapter-only capabilities are represented by real dependency edges, so
 * enforcement survives implementation renames and follows re-exported dependencies as well.
 */
abstract class CheckArchitectureTask : DefaultTask() {

    /** Module path to the paths of the projects it depends on, across every configuration. */
    @get:Input
    abstract val moduleDependencies: MapProperty<String, List<String>>

    /**
     * The same, narrowed to `api` configurations: the dependencies that do not stop at the module
     * declaring them. Rule 5 follows these, so a forbidden module cannot reach a screen by being
     * re-exported from a module the screen is allowed to declare.
     */
    @get:Input
    abstract val moduleApiDependencies: MapProperty<String, List<String>>

    /** The repository root; source-level rules read Kotlin files under it. */
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @TaskAction
    fun check() {
        val graph = moduleDependencies.get()
        val root = repositoryRoot.get().asFile
        val coreSources = coreProductionSources(root, graph.keys)
        val violations = FeatureFirstRules.staleRuleViolations(graph.keys) +
            FeatureFirstRules.featureModuleShapeViolations(graph.keys) +
            FeatureFirstRules.legacySplitDirectoryViolations(legacySplitDirectories(root)) +
            FeatureFirstRules.koinUsageViolations(architectureTextSources(root)) +
            FeatureFirstRules.dependencyViolations(graph, moduleApiDependencies.get()) +
            leakedUseCaseViolations(root, graph.keys) +
            FeatureFirstRules.sharedFeatureReferenceViolations(productionSources(root, "shared")) +
            FeatureFirstRules.featureVisibilityViolations(productionSources(root, "feature")) +
            FeatureFirstRules.contentPortViolations(coreSources) +
            FeatureFirstRules.coreVisibilityViolations(coreSources) +
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

    private fun legacySplitDirectories(root: File): List<String> =
        listOf(root.resolve("core"), root.resolve("feature")).flatMap { sourceRoot ->
            if (!sourceRoot.isDirectory) return@flatMap emptyList()
            sourceRoot.walkTopDown()
                .onEnter { it.name != "build" }
                .filter { it.isDirectory && it.name in setOf("api", "impl") }
                .map { it.relativeTo(root).invariantSeparatorsPath }
                .toList()
        }

    private fun architectureTextSources(root: File): Map<String, String> =
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
            .filter { file ->
                file.isFile && file.extension in setOf("kt", "kts", "toml")
            }
            .associate { file ->
                file.relativeTo(root).invariantSeparatorsPath to file.readText()
            }

    /** Kotlin sources under [dir], skipping Gradle output so generated code is never read. */
    private fun kotlinSourcesIn(dir: File): List<File> = dir.walkTopDown()
        .onEnter { it.name != "build" }
        .filter { it.isFile && it.extension == "kt" }
        .toList()
}
