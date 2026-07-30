plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.testing" }

    sourceSets {
        commonMain.dependencies {
            // The fakes implement ports from data/playback and build model objects.
            api(projects.core.data)
            api(projects.core.playback)
            api(projects.core.model)
        }
    }
}
