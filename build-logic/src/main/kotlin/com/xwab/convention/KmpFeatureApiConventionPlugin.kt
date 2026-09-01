package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.kmp.feature.api` — a feature's public API module: the serializable configs the
 * composition root uses to place its screen(s) in the navigation tree.
 *
 * Deliberately Compose-free, navigation-library-free and implementation-free. Feature modules do
 * not depend on one another; `:shared` consumes these contracts while wiring intent callbacks to
 * destinations. The module name mirrors Now in Android's `feature:<name>:api` layout.
 */
class KmpFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpLibraryConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            kotlinMultiplatform {
                dependenciesOf("commonMain") {
                    api(libs.library("kotlinx-serialization-core"))
                }
            }
        }
    }
}
