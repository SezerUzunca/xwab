package com.xwab.convention

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * `xwab.architecture.module` — publishes this module's own project dependencies for
 * `checkArchitecture` to read. See [ModuleDependencyReport] for why the module states them rather
 * than the check collecting them.
 *
 * Applied by `xwab.kmp.library`, so every capability, feature and support module reports without
 * remembering to. The two modules that configure themselves, `:shared` and `:androidApp`, apply it
 * by id. A module in the build that does not apply it fails `checkArchitecture` with Gradle's own
 * "no matching variant" error, naming the module — the check refuses to run with a module it
 * cannot see rather than passing without it.
 */
class ModuleArchitectureReportPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val report = tasks.register(ARCHITECTURE_REPORT_TASK, ArchitectureDependencyReportTask::class.java) { task ->
                task.modulePath.set(path)
                // Evaluated once configuration is complete, not when this plugin is applied: a
                // module declares its dependencies further down its own build script.
                task.projectDependencies.set(provider { projectDependencies(FeatureFirstRules::isProductionConfiguration) })
                task.apiProjectDependencies.set(
                    provider {
                        projectDependencies {
                            FeatureFirstRules.isProductionConfiguration(it) && FeatureFirstRules.isApiConfiguration(it)
                        }
                    },
                )
                task.report.set(layout.buildDirectory.file("architecture/dependencies.txt"))
            }

            configurations.consumable(ARCHITECTURE_REPORT_ELEMENTS) { configuration ->
                configuration.attributes { it.architectureReport(objects) }
                configuration.outgoing.artifact(report.flatMap { it.report })
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

internal const val ARCHITECTURE_REPORT_TASK = "architectureDependencyReport"
internal const val ARCHITECTURE_REPORT_ELEMENTS = "architectureDependencyReportElements"
private const val ARCHITECTURE_REPORT_VARIANT = "xwab-architecture-report"

/**
 * Both sides of the variant: a module's outgoing report and the root's request for it.
 *
 * A value no plugin in this build uses, on two attributes every Kotlin and Android variant already
 * carries. Those variants therefore mismatch outright instead of matching by omission, so a request
 * for a report can only ever select a report.
 */
internal fun AttributeContainer.architectureReport(objects: ObjectFactory) {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, ARCHITECTURE_REPORT_VARIANT))
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, ARCHITECTURE_REPORT_VARIANT))
}

/** Writes one module's [ModuleDependencyReport]. */
abstract class ArchitectureDependencyReportTask : DefaultTask() {
    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val projectDependencies: ListProperty<String>

    @get:Input
    abstract val apiProjectDependencies: ListProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun write() {
        val text = ModuleDependencyReport(
            module = modulePath.get(),
            dependencies = projectDependencies.get(),
            apiDependencies = apiProjectDependencies.get(),
        ).format()
        report.get().asFile.apply { parentFile.mkdirs() }.writeText(text)
    }
}
