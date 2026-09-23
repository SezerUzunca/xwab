package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.architecture` — registers `checkArchitecture` on the root project.
 * [CheckArchitectureTask] runs the rules; [FeatureFirstRules] holds them.
 *
 * The dependency graph arrives as reports, one per module, resolved like any other dependency —
 * see [ModuleArchitectureReportPlugin]. This project never reads another project's configurations,
 * which is what keeps the check possible under Gradle's isolated projects mode. Which modules exist
 * is settings' answer, published as `architectureModules` in `settings.gradle.kts`.
 */
class ArchitectureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            @Suppress("UNCHECKED_CAST")
            val modules = gradle.extensions.extraProperties.get(ARCHITECTURE_MODULES) as List<String>

            val declared = configurations.dependencyScope("architectureModules")
            modules.forEach { module ->
                dependencies.add(declared.name, dependencies.project(mapOf("path" to module)))
            }
            val reports = configurations.resolvable("architectureDependencyReports") { configuration ->
                configuration.extendsFrom(declared.get())
                configuration.attributes { it.architectureReport(objects) }
            }

            val checkArchitecture =
                tasks.register("checkArchitecture", CheckArchitectureTask::class.java) { task ->
                    task.group = "verification"
                    task.description =
                        "Fails when a dependency, package import or shared core use case breaks " +
                            "the feature-first rules."
                    task.repositoryRoot.set(layout.projectDirectory)
                    task.dependencyReports.from(reports)
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

    private companion object {
        /** Set by `settings.gradle.kts`: every project in the build that has a build script. */
        const val ARCHITECTURE_MODULES = "architectureModules"
    }
}
