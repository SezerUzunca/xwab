package com.xwab.convention

import dev.zacsweers.metro.gradle.DiagnosticSeverity
import dev.zacsweers.metro.gradle.MetroPluginExtension
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
            // The KMP library plugin creates no lint tasks for its Android variant unless this one
            // is applied as well: AGP's `KmpTaskManager` checks `hasPlugin("com.android.lint")`.
            // Without it the app's `checkDependencies` finds nothing to read in this module, which
            // a probe proved — an API 26 call in a minSdk 24 library passed lint.
            pluginManager.apply("com.android.lint")
            // Every module states its own project dependencies for `checkArchitecture`, rather
            // than the root reading them out of this project.
            pluginManager.apply(ModuleArchitectureReportPlugin::class.java)
            // Kotlin static analysis, with this module's pre-existing findings in its baseline.
            pluginManager.apply(DetektConventionPlugin::class.java)
            // Compile-time DI for every module. Metro is inert without its annotations, and
            // applying it here means no module has to remember to.
            pluginManager.apply("dev.zacsweers.metro")
            extensions.configure(MetroPluginExtension::class.java) { metro ->
                // Metro generates a public provider for the bound port while the contributed
                // adapter itself stays internal to its module. This is the cross-module mode the
                // port boundary relies on; exposing implementation bindings would undo it.
                metro.generateContributionProviders.set(true)
                metro.nonPublicContributionSeverity.set(DiagnosticSeverity.ERROR)
            }

            kotlinMultiplatform {
                if (iosEnabled) {
                    iosArm64()
                    iosSimulatorArm64()
                }

                android {
                    // A minor SDK release (37.1) can only be stated through the spec DSL; the plain
                    // `compileSdk = 37` setter has no place for it.
                    compileSdk {
                        version = release(libs.version("android-compileSdk").toInt()) {
                            minorApiLevel = libs.version("android-compileSdkMinor").toInt()
                        }
                    }
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
