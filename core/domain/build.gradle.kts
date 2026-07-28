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
        namespace = "com.xwab.app.core.domain"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // Deliberately framework-free: ports and use cases are plain Kotlin. No DI
            // container here — the composition root in `shared` owns the wiring.
            api(projects.core.model)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
