plugins {
    // Content storage and transport require no UI or bundled resource pipeline.
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.delivery" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            implementation(libs.okio)
        }
        androidMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}
