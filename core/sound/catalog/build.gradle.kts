plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.catalog" }

    sourceSets {
        commonMain.dependencies {
            // The catalog repository publishes Flow. No Koin here: this module declares a port and
            // binds nothing — `core:sound:manifest` owns the implementation and its DI module.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
