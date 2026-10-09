package com.xwab.convention

import dev.zacsweers.metro.gradle.DiagnosticSeverity
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension

/**
 * `xwab.kmp.feature.impl` — a feature's implementation: its screens, state, ViewModels, use cases
 * and the entry installer that puts them behind its routes.
 *
 * The routes and callback contracts live in the sibling `api` module, which this convention adds,
 * so an implementation always compiles against its own contract and never names another feature's.
 * Only the app shell depends on an implementation; `checkArchitecture` refuses any other edge.
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
class KmpFeatureImplConventionPlugin : Plugin<Project> {
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

            // The generated `Res` class keeps the package it had while the feature was one module,
            // `xwab.feature.<name>.generated.resources`. Compose would otherwise name it after this
            // project, and every screen's resource imports would move with the split.
            val feature = path.removePrefix(":feature:").substringBefore(':')
            extensions.getByType(ComposeExtension::class.java).extensions
                .configure(ResourcesExtension::class.java) { resources ->
                    resources.packageOfResClass = "xwab.feature.${feature.replace('-', '_')}.generated.resources"
                }

            kotlinMultiplatform {
                dependenciesOf("commonMain") {
                    implementation(project(path.substringBeforeLast(':') + ":api"))
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
