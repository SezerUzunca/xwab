package com.xwab.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * `xwab.kmp.library` — the baseline every library module in this build shares: KMP targets, the
 * macOS guard around the iOS ones, the SDK levels, the JVM target and `kotlin-test`.
 *
 * A module applying this declares only its own `namespace` and its own dependencies.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            // Compile-time DI for every module. Metro is inert without its annotations, and
            // applying it here means no module has to remember to.
            pluginManager.apply("dev.zacsweers.metro")

            kotlinMultiplatform {
                if (iosEnabled) {
                    iosArm64()
                    iosSimulatorArm64()
                }

                android {
                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()

                    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
                    withHostTest { }
                }

                dependenciesOf("commonTest") {
                    implementation(libs.library("kotlin-test"))
                }
            }
        }
    }
}
