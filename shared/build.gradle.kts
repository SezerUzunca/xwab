/**
 * The composition root. It owns application wiring and the app shell; feature UI lives in the
 * feature modules it assembles.
 *
 * Built on `xwab.kmp.compose` like every other Compose module, so targets, SDK levels, Metro, lint,
 * detekt and the architecture report come from one place. It used to spell all of that out itself
 * and drifted with it: moving to compileSdk 37.1 took three edits instead of one. What is its own
 * is what only it does — produce the iOS framework and declare the application graph.
 */
plugins {
    id("xwab.kmp.compose")
}

compose.resources {
    packageOfResClass = "xwab.shared.generated.resources"
}

kotlin {
    if (gradle.extra["enableIos"] as Boolean) {
        // The convention creates both targets; this only adds the framework the iOS app links.
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

    android {
        namespace = "com.xwab.app.shared"
        // Entry stores, movable chrome and recreation require a real Compose host.
        withDeviceTestBuilder { sourceSetTreeName = "test" }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            // Metro discovers installed capabilities and features on this classpath: their ViewModels,
            // entries and route serializers. A new core or feature module owns its contributions; no
            // app-level list needs updating for each one. Settings discovers them and publishes the
            // lists, so this module never reads another project's state to find them.
            listOf("coreModules", "featureModules").forEach { group ->
                @Suppress("UNCHECKED_CAST")
                val modules = gradle.extra[group] as List<String>
                modules.forEach { implementation(project(it)) }
            }
            implementation(projects.designsystem)

            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material3.adaptiveNavigation3)
            // Material's own bar/rail switch for the top-level destinations.
            implementation(libs.compose.material3.adaptiveNavigationSuite)
            // Navigation scene transitions.
            implementation(libs.compose.animation)
            // The tab icons are this module's own, not something it borrows from `:designsystem`.
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            // The graph owns the ViewModel factory that feature entries read from composition.
            implementation(libs.metrox.viewmodel)
            implementation(libs.metrox.viewmodel.compose)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // The launcher Activity lives here, beside the graph that constructs it: Metro builds
            // app components through MetroX's AppComponentFactory (API 28+), and only the module
            // declaring the graph sees its contributions.
            implementation(libs.metrox.android)
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        // Tests that need a real platform: a Compose host (entry stores, recreation, the adaptive
        // layout, platform Back) or its saved-state format (a Bundle on Android). Written once, run on
        // Android devices and iOS simulators; plain logic stays in commonTest.
        val composeTest = create("composeTest") {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.compose.uiTest)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                // The integration scenario replaces the downloaded catalog with this module's fake.
                implementation(projects.testing.sound)
            }
        }
        getByName("androidDeviceTest") {
            dependsOn(composeTest)
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.runner)
                // Compose's older transitive Espresso uses InputManager reflection removed in API 37.
                implementation(libs.androidx.test.espressoCore)
            }
        }
        if (gradle.extra["enableIos"] as Boolean) {
            iosTest.get().dependsOn(composeTest)
        }
    }
}
