/**
 * The app shell: the app root, Navigation 3 policy, tabs and the layout around destinations. It
 * compiles against feature api modules only — routes, callback contracts and the now-playing bar's
 * contract. Feature implementations, and the application graph that collects them, belong to
 * `:composition`, which depends on this module and hands it what the graphs build.
 *
 * Built on `xwab.kmp.compose` like every other Compose module, so targets, SDK levels, Metro, lint,
 * detekt and the architecture report come from one place. It used to spell all of that out itself
 * and drifted with it: moving to compileSdk 37.1 took three edits instead of one.
 */
plugins {
    id("xwab.kmp.compose")
}

compose.resources {
    packageOfResClass = "xwab.shared.generated.resources"
}

kotlin {
    android {
        namespace = "com.xwab.app.shared"
        // Entry stores, movable chrome and recreation require a real Compose host.
        withDeviceTestBuilder { sourceSetTreeName = "test" }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            // Every feature's contract: routes and their serializers, collected by Metro into the
            // saved back stacks' serializers module, and the callback contracts the shell supplies.
            // Settings discovers them and publishes the list, so this module never reads another
            // project's state to find them. No implementation is on this classpath.
            @Suppress("UNCHECKED_CAST")
            (gradle.extra["featureApiModules"] as List<String>).forEach { implementation(project(it)) }
            // The capability types the shell itself names: playback item ids and each content
            // module's playback kind, which the shell maps to the screen an item opens.
            implementation(projects.core.session.api)
            implementation(projects.core.sound.api)
            implementation(projects.core.story.api)
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
            // The app root places the graph's ViewModel factory where feature entries read it.
            implementation(libs.metrox.viewmodel)
            implementation(libs.metrox.viewmodel.compose)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
            // The shell's tests draw and resolve real entries and build the real application graph,
            // so they run against the composition root and everything it installs. Metro generates a
            // test's dynamic graph from that test's own classpath, which is why the installed modules
            // are listed here as well. Test configurations are not production dependencies, so this
            // does not give the shell's own code a way to the implementations.
            implementation(projects.composition)
            listOf("coreModules", "featureModules").forEach { group ->
                @Suppress("UNCHECKED_CAST")
                val modules = gradle.extra[group] as List<String>
                modules.forEach { implementation(project(it)) }
            }
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
                // The integration test regenerates the Android application graph, which holds the
                // composition root's launcher Activity under MetroX's `@ActivityKey`; Metro needs that
                // map key on this compilation's classpath to read the contribution.
                implementation(libs.metrox.android)
                // Compose's older transitive Espresso uses InputManager reflection removed in API 37.
                implementation(libs.androidx.test.espressoCore)
            }
        }
        if (gradle.extra["enableIos"] as Boolean) {
            iosTest.get().dependsOn(composeTest)
        }
    }
}
