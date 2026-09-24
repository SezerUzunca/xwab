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
    }

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
            // Chrome rather than a destination, so it is last: it has no route, and reaches the
            // screen as a NavDisplay scene decorator instead of an entry.
            implementation(projects.feature.nowplaying)

            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            // SharedTransitionLayout, for the now-playing scene decorator.
            implementation(libs.compose.animation)
            // The tab icons are this module's own, not something it borrows from `:designsystem`.
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        // The same simulator harness used by feature screen tests also exercises the real
        // navigation composition, including saved-state and ViewModel entry decorators.
        if (gradle.extra["enableIos"] as Boolean) {
            iosTest.dependencies {
                implementation(libs.compose.uiTest)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
            }
        }
    }
}
