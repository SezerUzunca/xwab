plugins {
    id("xwab.kmp.library")
    id("xwab.metro")
}

kotlin {
    android {
        namespace = "com.xwab.app.core.speech"
        // The Android graph takes a Context, and the voice is the device's text-to-speech engine;
        // both exist only on a device, so the graph's tests run there.
        withDeviceTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.speech.api)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            // Spoken words and rendered tracks are files this module writes and reads back.
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
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
