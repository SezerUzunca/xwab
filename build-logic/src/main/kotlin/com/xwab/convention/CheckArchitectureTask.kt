package com.xwab.convention

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Holds the four rules that keep feature slices independent. All of them are easy to break by
 * accident and none of them fail to compile, which is why they are checked rather than written
 * down.
 *
 * 1. A core module may not depend on a feature. Dependencies point one way.
 * 2. A feature may depend on another feature's `:navigation` module and nothing else of it.
 *    That module is the whole contract: routes in, no implementation.
 * 3. A use case in a core module must serve more than one feature. A screen-specific one belongs
 *    to that screen's module, otherwise screen logic leaks into shared capabilities.
 * 4. A feature may not reach audio delivery. `core:audio-content` is on every feature's classpath
 *    for the catalog it serves; resolving a track to a URI or touching the audio file store is
 *    playback's job, and a screen doing it itself would bypass the session entirely.
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
        val root = repositoryRoot.get().asFile
        val violations = dependencyViolations(moduleDependencies.get()) +
            leakedUseCaseViolations(root) +
            audioDeliveryViolations(root)

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Feature-first rules broken (${violations.size}):")
                    violations.forEach { appendLine("  - $it") }
                }.trimEnd(),
            )
        }

        logger.lifecycle("Feature-first rules hold across ${moduleDependencies.get().size} modules.")
    }

    private fun dependencyViolations(graph: Map<String, List<String>>): List<String> {
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
            }
        }

        return violations.distinct().sorted()
    }

    private fun leakedUseCaseViolations(root: File): List<String> {
        val coreRoot = root.resolve("core")
        if (!coreRoot.isDirectory) return emptyList()

        val useCases = kotlinSourcesIn(coreRoot)
            .flatMap { file ->
                val module = ":core:${file.relativeTo(coreRoot).invariantSeparatorsPath.substringBefore('/')}"
                USE_CASE_DECLARATION.findAll(file.readText())
                    .map { match -> match.groupValues[1] to module }
                    .toList()
            }
            .distinct()
        if (useCases.isEmpty()) return emptyList()

        val featureDirs = root.resolve("feature").listFiles().orEmpty().filter { it.isDirectory }
        val sourcesByFeature = featureDirs.associate { dir ->
            dir.name to kotlinSourcesIn(dir).map { it.readText() }
        }

        return useCases.mapNotNull { (useCase, module) ->
            val users = sourcesByFeature.filterValues { sources -> sources.any { it.contains(useCase) } }.keys
            if (users.size != 1) return@mapNotNull null

            "$module declares $useCase, but only feature:${users.single()} uses it. " +
                "Move it into that feature's own `domain` package, or leave it here once a " +
                "second feature needs it."
        }.sorted()
    }

    /**
     * Rule 4 cannot be checked from the dependency graph: `core:audio-content` is a legitimate
     * feature dependency because it also serves the catalog. Only the source can say whether a
     * screen reached past that into delivery.
     */
    private fun audioDeliveryViolations(root: File): List<String> {
        val featureDirs = root.resolve("feature").listFiles().orEmpty().filter { it.isDirectory }

        return featureDirs.flatMap { dir ->
            val sources = kotlinSourcesIn(dir).map { it.readText() }
            DELIVERY_SYMBOLS.filter { symbol -> sources.any { it.contains(symbol) } }
                .map { symbol ->
                    "feature:${dir.name} references $symbol. Audio delivery belongs to " +
                        "core:audio-content and reaches a screen only through core:playback."
                }
        }.sorted()
    }

    /** Kotlin sources under [dir], skipping Gradle output so generated code is never read. */
    private fun kotlinSourcesIn(dir: File): List<File> = dir.walkTopDown()
        .onEnter { it.name != "build" }
        .filter { it.isFile && it.extension == "kt" }
        .toList()

    private fun featureOf(modulePath: String): String = modulePath.removePrefix(FEATURE_PREFIX).substringBefore(':')

    private companion object {
        const val CORE_PREFIX = ":core:"
        const val FEATURE_PREFIX = ":feature:"
        const val NAVIGATION_SUFFIX = ":navigation"
        val USE_CASE_DECLARATION = Regex("""^\s*(?:internal\s+|public\s+)?class\s+(\w+UseCase)\b""", RegexOption.MULTILINE)
        val DELIVERY_SYMBOLS = listOf("AudioContentResolver", "AudioFileStore")
    }
}
