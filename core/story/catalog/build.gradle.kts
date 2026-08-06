plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.story" }

    sourceSets {
        commonMain.dependencies {
            // The story repository publishes Flow. No Koin here: this module declares a port and
            // binds nothing — `core:story:manifest` owns the implementation and its DI module.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
