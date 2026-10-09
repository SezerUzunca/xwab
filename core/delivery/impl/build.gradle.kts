plugins {
    // Content storage and transport require no UI or bundled resource pipeline.
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.delivery" }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.delivery.api)
            implementation(projects.core.network.api)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
