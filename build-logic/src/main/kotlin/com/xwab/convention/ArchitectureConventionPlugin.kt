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
                        "Fails when a module dependency or a shared core use case breaks the " +
                            "feature-first rules."
                    task.repositoryRoot.set(layout.projectDirectory)
                }

            // The dependency graph is only complete once every module has been configured.
            gradle.projectsEvaluated {
                val graph = rootProject.subprojects.associate { module ->
                    val projectDependencies = sortedSetOf<String>()
                    module.configurations.forEach { configuration ->
                        configuration.dependencies
                            .withType(ProjectDependency::class.java)
                            .forEach { dependency -> projectDependencies += dependency.path }
                    }
                    module.path to projectDependencies.toList()
                }

                checkArchitecture.configure { task -> task.moduleDependencies.set(graph) }
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
}
