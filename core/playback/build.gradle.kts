import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    if (gradle.extra["enableIos"] as Boolean) {
        iosArm64()
        iosSimulatorArm64()
    }

    android {
        namespace = "com.xwab.app.core.playback"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.media)
            implementation(projects.core.model)
            implementation(libs.kotlinx.coroutines.core)
            // The module's only public surface is a Koin Module, which puts Koin's own
            // types in this module's API. Same reason as core:media.
            api(libs.koin.core)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
