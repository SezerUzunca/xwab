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
            // Metro discovers installed capabilities on this classpath. A new core module owns
            // its contributions; no app-level list needs updating for each adapter or content kind.
            // Settings discovers them and publishes the list, so this module never reads another
            // project's state to find them.
            @Suppress("UNCHECKED_CAST")
            val coreModules = gradle.extra["coreModules"] as List<String>
            coreModules.forEach { implementation(project(it)) }
            implementation(projects.designsystem)

            implementation(projects.feature.browse)
            implementation(projects.feature.favorites)
            implementation(projects.feature.category)
            implementation(projects.feature.sound)
            implementation(projects.feature.story)
            implementation(projects.feature.nowplaying)

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
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        val navigationTest = create("navigationTest") {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.compose.uiTest)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
            }
        }
        getByName("androidDeviceTest") {
            dependsOn(navigationTest)
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.runner)
                // Compose's older transitive Espresso uses InputManager reflection removed in API 37.
                implementation(libs.androidx.test.espressoCore)
            }
        }
        // The same navigation composition tests run on Android devices and iOS simulators.
        if (gradle.extra["enableIos"] as Boolean) {
            iosTest.get().dependsOn(navigationTest)
        }
    }
}
