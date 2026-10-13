plugins {
    id("xwab.kmp.library")
    id("xwab.metro")
}

kotlin {
    android {
        namespace = "com.xwab.app.core.shuffle"
        // The Android graph takes a Context, and the voice is the device's text-to-speech engine;
        // both exist only on a device, so the graph's tests run there.
        withDeviceTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.shuffle.api)
            // The session owns the resolver contract; this module answers it with a word track it
            // renders itself. It has no network, cache-download or engine dependency.
            implementation(projects.core.session.api)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            // The word cache and the rendered track are files this module writes and reads back.
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
