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
            // Every capability module, because the composition root is what binds them. This is
            // also the only module outside `core:playback:session` that names delivery or the
            // engine; `checkArchitecture` rule 4 keeps features from joining that list.
            implementation(projects.core.sound.catalog)
            implementation(projects.core.sound.manifest)
            implementation(projects.core.sound.delivery)
            implementation(projects.core.story.catalog)
            implementation(projects.core.story.manifest)
            implementation(projects.core.sound.favorites)
            implementation(projects.core.playback.session)
            implementation(projects.core.playback.engine)
            implementation(projects.core.network)
            implementation(projects.core.navigation)
            implementation(projects.core.designsystem)
            implementation(projects.feature.category)
            implementation(projects.feature.player)
            implementation(projects.feature.story)
            // The home screen lives here rather than in a slice of its own, so this module routes
            // to two features and needs their navigation APIs — the only ones named here.
            implementation(projects.feature.category.navigation)
            implementation(projects.feature.player.navigation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            // Hosting a screen means hosting the Compose surface `xwab.kmp.feature` hands a slice.
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.koin.compose.viewmodel)
            // The composition root owns every Koin module the domain layer needs.
            implementation(libs.koin.core)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.kotlinx.serialization.core)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            // The home screen's use case is tested here now, against the same port fakes a feature
            // would have used.
            implementation(projects.core.testing)
            // Test-only: proves each feature really contributes the serializers for its routes.
            // Category and player are already on the main classpath; story is only needed here.
            implementation(projects.feature.story.navigation)
        }
    }
}

// Home's strings live here now, so this module generates a `Res` class of its own. Spelled out
// rather than left to the default, because the code imports the package by name.
compose.resources {
    packageOfResClass = "xwab.shared.generated.resources"
}
