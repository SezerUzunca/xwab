package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.kmp.feature.navigation` — a feature's navigation API module: the routes other features
 * are allowed to see, plus the serializers that let the back stack survive process death.
 *
 * Deliberately Compose-free and implementation-free. This is the only part of a feature that
 * another feature may depend on, which `checkArchitecture` enforces.
 */
class KmpFeatureNavigationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpLibraryConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            kotlinMultiplatform {
                dependenciesOf("commonMain") {
                    api(project(":core:navigation"))
                    api(libs.library("kotlinx-serialization-core"))
                }
            }
        }
    }
}
