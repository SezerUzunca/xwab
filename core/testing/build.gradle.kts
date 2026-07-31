plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.testing" }

    sourceSets {
        commonMain.dependencies {
            // The fakes implement the catalog, favorites and playback ports, and build model objects.
            api(projects.core.audioContent)
            api(projects.core.preferences)
            api(projects.core.playback)
            api(projects.core.model)
        }
    }
}
