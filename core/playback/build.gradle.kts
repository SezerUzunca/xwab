plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.playback" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.playbackEngine)
            implementation(projects.core.model)
            implementation(libs.kotlinx.coroutines.core)
            // The module's only public surface is a Koin Module, which puts Koin's own
            // types in this module's API. Same reason as core:playback-engine.
            api(libs.koin.core)
        }
    }
}
