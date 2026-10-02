package com.xwab.convention

import dev.zacsweers.metro.gradle.DiagnosticSeverity
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.kmp.feature` — a complete feature slice: its Navigation 3 route and serializer, screen,
 * state and presentation logic all live in one module.
 *
 * Capability modules are deliberately **not** here. A feature declares the ones it reads in its own
 * build file, which lets `checkArchitecture` enforce adapter boundaries as dependency edges — a feature
 * may not declare `core:delivery` or `core:playback` — instead of scanning sources for
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
            // A feature contributes MetroX ViewModel entries. An assisted ViewModel registers its
            // nested `@AssistedFactory`, which Metro binds as itself rather than through a generated
            // provider, so core's error would reject every one, and Kotlin cannot suppress an error.
            // A warning can be: each such factory suppresses it where it is declared, and any other
            // non-public contribution in a feature, such as an internal binding container that would
            // never reach the app graph, is still reported.
            extensions.configure(MetroPluginExtension::class.java) { metro ->
                metro.nonPublicContributionSeverity.set(DiagnosticSeverity.WARN)
            }

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
                    // ViewModels contribute themselves to the app graph and entries resolve them.
                    implementation(libs.library("metrox-viewmodel"))
                    implementation(libs.library("metrox-viewmodel-compose"))
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
