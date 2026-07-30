package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.kmp.compose` — [KmpLibraryConventionPlugin] plus Compose Multiplatform: the compiler
 * plugin, Android resource processing (Compose resources ship as Android assets) and the Compose
 * runtime.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpLibraryConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            kotlinMultiplatform {
                android {
                    androidResources { enable = true }
                }

                dependenciesOf("commonMain") {
                    implementation(libs.library("compose-runtime"))
                }
            }
        }
    }
}
