/**
 * The composition root: the one module that sees every installed implementation. It declares the
 * application graphs and the navigation host's entry graph, constructs the Android launcher
 * Activity and the iOS view controller, and produces the iOS framework.
 *
 * Metro collects contributions from the compile classpath of the module that declares a graph, so
 * the graphs live here, beside every core and feature module, and the shell they feed (`:shared`)
 * keeps feature implementations off its own classpath. There is no UI here beyond handing the
 * shell's `App` what the graphs build.
 */
plugins {
    id("xwab.kmp.compose")
}

kotlin {
    if (gradle.extra["enableIos"] as Boolean) {
        // The convention creates both targets; this only adds the framework the iOS app links. The
        // name stays `Shared`, which the Xcode project and its Swift imports already use.
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    android { namespace = "com.xwab.app.composition" }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            // Metro discovers installed capabilities and features on this classpath: their ports,
            // ViewModels, entries and the now-playing bar. A new core or feature module owns its
            // contributions; no app-level list needs updating for each one. Settings discovers them
            // and publishes the lists, so this module never reads another project's state.
            listOf("coreModules", "featureModules").forEach { group ->
                @Suppress("UNCHECKED_CAST")
                val modules = gradle.extra[group] as List<String>
                modules.forEach { implementation(project(it)) }
            }
            implementation(libs.compose.ui)
            implementation(libs.navigation3.runtime)
            // The graph owns the ViewModel factory that feature entries read from composition.
            implementation(libs.metrox.viewmodel)
        }
        androidMain.dependencies {
            // The launcher Activity lives here, beside the graph that constructs it: Metro builds
            // app components through MetroX's AppComponentFactory (API 28+), and only the module
            // declaring the graph sees its contributions.
            implementation(libs.metrox.android)
            implementation(libs.androidx.activity.compose)
        }
    }
}
