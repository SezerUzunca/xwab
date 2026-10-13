plugins {
    id("xwab.kmp.library")
    id("xwab.metro")
}

kotlin {
    android {
        namespace = "com.xwab.app.core.notes"
        // The Android graph takes a Context, which only a device has; its tests run there.
        withDeviceTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.notes.api)
            // `implementation`, not `api`: nothing this module publishes names a DataStore type.
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kermit.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
        }
    }
}
