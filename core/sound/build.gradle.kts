plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.sound" }

    sourceSets {
        commonMain.dependencies {
            // SoundPort publishes Flow; its implementation owns the manifest.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
