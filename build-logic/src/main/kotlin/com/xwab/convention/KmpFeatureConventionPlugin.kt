package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.kmp.feature` — everything a feature slice needs: [KmpComposeConventionPlugin] plus the
 * shared core modules and the Compose/Koin surface every screen in this app uses.
 *
 * A feature's own build file is then only its namespace and the `:navigation` modules it routes
 * to. A feature must never depend on another feature's implementation module — `checkArchitecture`
 * fails the build if one does.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpComposeConventionPlugin::class.java)

            kotlinMultiplatform {
                dependenciesOf("commonMain") {
                    implementation(project(":core:model"))
                    implementation(project(":core:data"))
                    implementation(project(":core:playback"))
                    implementation(project(":core:designsystem"))
                    implementation(project(":core:navigation"))
                    implementation(libs.library("compose-foundation"))
                    implementation(libs.library("compose-material3"))
                    implementation(libs.library("compose-ui"))
                    implementation(libs.library("compose-components-resources"))
                    implementation(libs.library("compose-uiToolingPreview"))
                    implementation(libs.library("androidx-lifecycle-viewmodelCompose"))
                    implementation(libs.library("koin-compose-viewmodel"))
                    implementation(libs.library("koin-compose-navigation3"))
                }

                // Every feature joins the same shared ports, so each receives the common fakes.
                dependenciesOf("commonTest") {
                    implementation(project(":core:testing"))
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
