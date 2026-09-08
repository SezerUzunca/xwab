plugins {
    id("xwab.kmp.library")
}

kotlin {
    android {
        namespace = "com.xwab.app.core.playbackengine"
        // The only module with instrumentation tests: the Media3 service and the
        // MediaController handshake can only be exercised on a device.
        withDeviceTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.session)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
        }
    }
}
