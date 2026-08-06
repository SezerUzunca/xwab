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
 * Runs the four rules that keep feature slices independent. All of them are easy to break by
 * accident and none of them fail to compile, which is why they are checked rather than written
 * down.
 *
 * 1. A core module may not depend on a feature. Dependencies point one way.
 * 2. A feature may depend on another feature's `:navigation` module and nothing else of it.
 *    That module is the whole contract: routes in, no implementation.
 * 3. A use case in a core module must serve more than one feature. A screen-specific one belongs
 *    to that screen's module, otherwise screen logic leaks into shared capabilities.
 * 4. A feature may not declare a module in [FeatureFirstRules.MODULES_OFF_LIMITS_TO_FEATURES].
 *    Fetching audio, driving a platform player and reading the shipped manifest are things done on
 *    a screen's behalf; a screen reaching any of them directly bypasses the port that exists for it.
 *
 * The rules themselves live in [FeatureFirstRules], where they are unit-tested from both sides.
 * This task is only their plumbing: it collects the dependency graph and the sources rule 3 reads.
 *
 * Rule 4 replaces one that scanned feature sources for the strings `AudioContentResolver` and
 * `AudioFileStore`. That version existed because the catalog and delivery shared a module, so
 * delivery was on every feature's classpath and the graph could not tell a legitimate dependency
 * from an illegitimate reach through it. Splitting capabilities apart until each boundary was a
 * real edge is what made the check possible — and an edge survives a rename, which a quoted class
 * name did not.
 */
abstract class CheckArchitectureTask : DefaultTask() {

    /** Module path to the paths of the projects it depends on, across every configuration. */
    @get:Input
    abstract val moduleDependencies: MapProperty<String, List<String>>

    /** The repository root; rule 3 reads Kotlin sources under it. */
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @TaskAction
    fun check() {
        val graph = moduleDependencies.get()
        val violations = FeatureFirstRules.staleRuleViolations(graph.keys) +
            FeatureFirstRules.dependencyViolations(graph) +
            leakedUseCaseViolations(repositoryRoot.get().asFile, graph.keys)

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

    /**
     * Reads what rule 3 needs off the file system, then hands it to [FeatureFirstRules].
     *
     * @param modulePaths every module in the build, which is how a source file under a grouped
     *   core module — `core/sound/catalog`, not `core/catalog` — is attributed to the module that
     *   actually declares it rather than to the group directory above it.
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

    /** Kotlin sources under [dir], skipping Gradle output so generated code is never read. */
    private fun kotlinSourcesIn(dir: File): List<File> = dir.walkTopDown()
        .onEnter { it.name != "build" }
        .filter { it.isFile && it.extension == "kt" }
        .toList()
}
