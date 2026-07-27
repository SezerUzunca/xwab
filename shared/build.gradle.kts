import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            implementation(projects.core.media)
            implementation(projects.core.preferences)
            implementation(projects.core.model)
            implementation(projects.core.domain)
            implementation(projects.core.navigation)
            implementation(projects.core.data)
            implementation(projects.core.ui)
            implementation(projects.feature.home)
            implementation(projects.feature.home.navigation)
            implementation(projects.feature.category)
            implementation(projects.feature.category.navigation)
            implementation(projects.feature.player)
            implementation(projects.feature.player.navigation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.koin.compose.navigation3)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.kotlinx.serialization.core)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
