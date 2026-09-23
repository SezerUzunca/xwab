import dev.zacsweers.metro.gradle.DiagnosticSeverity
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * The composition root configures its own targets rather than applying `xwab.kmp.compose`.
 *
 * It is the only module that produces an iOS framework binary, and the only one that declares the
 * application graph. It owns application wiring and the app shell; feature UI lives in the feature
 * modules it assembles.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Declared here rather than inherited: this module skips `xwab.kmp.library`, which is what
    // applies Metro everywhere else, and the application graph is generated in this module.
    alias(libs.plugins.metro)
    // What `xwab.kmp.library` would otherwise have applied: this module's own project
    // dependencies, reported for `checkArchitecture`.
    id("xwab.architecture.module")
    // Also applied by `xwab.kmp.library`: detekt, with this module's baseline.
    id("xwab.detekt")
}

// Mirrors what `xwab.kmp.library` configures for every other module, so the module that merges the
// contributions is not the one module Metro is configured differently in.
metro {
    generateContributionProviders.set(true)
    nonPublicContributionSeverity.set(DiagnosticSeverity.ERROR)
}

compose.resources {
    packageOfResClass = "xwab.shared.generated.resources"
}

kotlin {
    if (gradle.extra["enableIos"] as Boolean) {
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
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        // Compose resources ship as Android assets, so the tab labels need this even though no
        // host test reads one.
        androidResources {
            enable = true
        }
        withHostTest { }
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

            implementation(libs.compose.runtime)
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
            implementation(libs.kotlin.test)
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
