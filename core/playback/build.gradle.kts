plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.playback" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.playbackEngine)
            api(projects.core.model)
            api(libs.kotlinx.coroutines.core)
            // The DI entry point exposes Koin's Module type.
            api(libs.koin.core)
        }
    }
}
