import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * The composition root configures its own targets rather than applying `xwab.kmp.compose`.
 *
 * It is the only module that produces an iOS framework binary, and the only one whose host tests
 * need Android resources — and `withHostTest` cannot be called a second time on top of the one
 * the convention plugin already declares. Everything else below matches what that plugin does.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    if (gradle.extra["enableIos"] as Boolean) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
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
            implementation(projects.core.playbackEngine)
            implementation(projects.core.preferences)
            implementation(projects.core.model)
            implementation(projects.core.navigation)
            implementation(projects.core.audioContent)
            implementation(projects.core.playback)
            implementation(projects.core.designsystem)
            implementation(projects.feature.home)
            implementation(projects.feature.category)
            implementation(projects.feature.player)
            // The one route the composition root names itself: the start destination. Every
            // other route reaches it through the feature's `FeatureEntry`.
            implementation(projects.feature.home.navigation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            // The composition root owns every Koin module the domain layer needs.
            implementation(libs.koin.core)
            implementation(libs.koin.compose.navigation3)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.kotlinx.serialization.core)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            // Test-only: proves each feature really contributes the serializers for its routes.
            implementation(projects.feature.category.navigation)
            implementation(projects.feature.player.navigation)
        }
    }
}
