plugins {
    id("xwab.kmp.library")
}

kotlin {
    android {
        namespace = "com.xwab.app.core.playback"
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
            // The HTTP data source the service hands ExoPlayer, so requests can identify the app.
            implementation(libs.androidx.media3.datasource)
            implementation(libs.androidx.media3.session)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
        }
    }
}
