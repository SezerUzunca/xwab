package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.kmp.feature` — a complete feature slice: its Navigation 3 route and serializer, screen,
 * state and presentation logic all live in one module.
 *
 * Capability modules are deliberately **not** here. A feature declares the ones it reads in its own
 * build file, which lets `checkArchitecture` enforce adapter boundaries as dependency edges — a feature
 * may not declare `core:sound:delivery` or `core:playback` — instead of scanning sources for
 * class names. Handing every core module to every feature is what made that impossible before.
 * Test support is declared per feature: a slice that reads two capabilities has no business
 * compiling against fakes for a third.
 *
 * A feature must never depend on another feature module either — again rule-checked rather than
 * prevented, since nothing stops a build file from declaring one.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpComposeConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            kotlinMultiplatform {
                dependenciesOf("commonMain") {
                    implementation(project(":designsystem"))
                    api(libs.library("navigation3-runtime"))
                    api(libs.library("kotlinx-serialization-core"))
                    implementation(libs.library("compose-foundation"))
                    implementation(libs.library("compose-material3"))
                    implementation(libs.library("compose-ui"))
                    implementation(libs.library("compose-components-resources"))
                    implementation(libs.library("compose-uiToolingPreview"))
                    implementation(libs.library("androidx-lifecycle-viewmodelCompose"))
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
