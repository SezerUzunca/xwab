package com.xwab.convention

import dev.zacsweers.metro.gradle.DiagnosticSeverity
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Metro compiler configuration for modules that declare graphs, injections or contributions. */
class MetroConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.zacsweers.metro")
            extensions.configure(MetroPluginExtension::class.java) { metro ->
                // Generated public providers keep contributed adapters internal across module boundaries.
                metro.generateContributionProviders.set(true)
                metro.nonPublicContributionSeverity.set(DiagnosticSeverity.ERROR)
            }
        }
    }
}
