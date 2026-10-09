package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.kmp.feature.api` — what a feature shows the app shell: its routes with their serializer
 * registration, the callback contracts the shell supplies, and any shell chrome contract.
 *
 * Nothing here can draw a screen or hold one's state. There is no Compose UI toolkit, no ViewModel
 * and no design system on this classpath, so a screen moved here does not compile. The Compose
 * compiler is applied for contracts the shell renders, such as the now-playing bar's, and a module
 * that needs a UI type in a signature (`Modifier`, `StringResource`) declares that one library.
 */
class KmpFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpLibraryConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            kotlinMultiplatform {
                dependenciesOf("commonMain") {
                    api(libs.library("navigation3-runtime"))
                    api(libs.library("kotlinx-serialization-core"))
                    implementation(libs.library("compose-runtime"))
                }
            }
        }
    }
}
