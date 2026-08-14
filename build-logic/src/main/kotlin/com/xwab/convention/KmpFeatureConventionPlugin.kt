package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.kmp.feature` — what every screen in this app is built out of regardless of what it shows:
 * [KmpComposeConventionPlugin], the design system, navigation, and the Compose/Koin surface.
 *
 * Capability modules are deliberately **not** here. A feature declares the ones it reads in its own
 * build file, which is what lets `checkArchitecture` state rule 4 as a dependency edge — a feature
 * may not declare `core:sound:delivery` or `core:playback:engine` — instead of scanning sources for
 * class names. Handing every core module to every feature is what made that impossible before.
 * This is also why `core:testing` is declared per feature: a slice that reads two capabilities has
 * no business compiling against fakes for a third.
 *
 * A feature must never depend on another feature module either — again rule-checked rather than
 * prevented, since nothing stops a build file from declaring one.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpComposeConventionPlugin::class.java)

            kotlinMultiplatform {
                dependenciesOf("commonMain") {
                    implementation(project(":core:designsystem"))
                    implementation(libs.library("navigation3-runtime"))
                    implementation(libs.library("compose-foundation"))
                    implementation(libs.library("compose-material3"))
                    implementation(libs.library("compose-ui"))
                    implementation(libs.library("compose-components-resources"))
                    implementation(libs.library("compose-uiToolingPreview"))
                    implementation(libs.library("androidx-lifecycle-viewmodelCompose"))
                    implementation(libs.library("koin-compose-viewmodel"))
                }
            }

            // Renders `@Preview` composables in the IDE; runtime-only, never on the compile
            // classpath. Spelled as a coordinate because a convention plugin has no generated
            // `androidRuntimeClasspath` accessor.
            val uiTooling = libs.library("compose-uiTooling").get()
            dependencies.add(
                "androidRuntimeClasspath",
                "${uiTooling.module}:${uiTooling.versionConstraint.requiredVersion}",
            )
        }
    }
}
