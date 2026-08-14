package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 * `xwab.architecture` — registers `checkArchitecture` on the root project.
 * [CheckArchitectureTask] runs the rules; [FeatureFirstRules] holds them.
 */
class ArchitectureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val checkArchitecture =
                tasks.register("checkArchitecture", CheckArchitectureTask::class.java) { task ->
                    task.group = "verification"
                    task.description =
                        "Fails when a dependency, package import or shared core use case breaks " +
                            "the feature-first rules."
                    task.repositoryRoot.set(layout.projectDirectory)
                }

            // The dependency graph is only complete once every module has been configured.
            gradle.projectsEvaluated {
                val graph = rootProject.subprojects.associate { module ->
                    module.path to module.projectDependencies { true }
                }
                // Collected separately rather than filtered out of the graph, because the two
                // answer different questions: what a module declares, and what it re-exports.
                val apiGraph = rootProject.subprojects.associate { module ->
                    module.path to module.projectDependencies(FeatureFirstRules::isApiConfiguration)
                }

                checkArchitecture.configure { task ->
                    task.moduleDependencies.set(graph)
                    task.moduleApiDependencies.set(apiGraph)
                }
            }

            // A broken rule should surface the way a broken test does — and so should a rule that
            // has stopped rejecting anything, which is what `build-logic`'s own tests cover.
            // Included builds run nothing on their own, so the dependency has to be spelled out.
            tasks.register("check") { task ->
                task.group = "verification"
                task.dependsOn(checkArchitecture)
                task.dependsOn(gradle.includedBuild("build-logic").task(":test"))
            }
        }
    }

    /** The paths of the projects this module depends on, in configurations matching [include]. */
    private fun Project.projectDependencies(include: (String) -> Boolean): List<String> {
        val paths = sortedSetOf<String>()
        configurations.forEach { configuration ->
            if (!include(configuration.name)) return@forEach
            configuration.dependencies
                .withType(ProjectDependency::class.java)
                .forEach { dependency -> paths += dependency.path }
        }
        return paths.toList()
    }
}
