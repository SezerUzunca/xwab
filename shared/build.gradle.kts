import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * The composition root configures its own targets rather than applying `xwab.kmp.compose`.
 *
 * It is the only module that produces an iOS framework binary, and the only one whose host tests
 * need Android resources. It owns application wiring and the app shell; feature UI lives in the
 * feature modules it assembles.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Declared here rather than inherited: this module skips `xwab.kmp.library`, which is what
    // applies Metro everywhere else, and the application graph is generated in this module.
    alias(libs.plugins.metro)
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
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // The composition root sees every contributed core adapter. Features cannot reach the
            // delivery layer, engine, manifests or network directly; this module declares the graph.
            implementation(projects.core.sound.catalog)
            implementation(projects.core.sound.manifest)
            implementation(projects.core.sound.delivery)
            implementation(projects.core.story.catalog)
            implementation(projects.core.story.manifest)
            implementation(projects.core.sound.favorites)
            implementation(projects.core.session)
            implementation(projects.core.playback)
            implementation(projects.core.network)
            implementation(projects.designsystem)

            implementation(projects.feature.browse)
            implementation(projects.feature.favorites)
            implementation(projects.feature.category)
            implementation(projects.feature.sounds)
            implementation(projects.feature.story)

            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(projects.testing)
            // The root's navigation contract tests deliberately assert that every public
            // route can be restored and rendered.
            implementation(projects.feature.browse)
            implementation(projects.feature.favorites)
            implementation(projects.feature.category)
            implementation(projects.feature.sounds)
            implementation(projects.feature.story)
        }
    }
}
