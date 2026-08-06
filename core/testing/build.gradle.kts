plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.testing" }

    sourceSets {
        commonMain.dependencies {
            // The fakes implement the three ports every screen reads, and build catalog values.
            api(projects.core.sound.catalog)
            api(projects.core.sound.favorites)
            api(projects.core.playback.session)
        }
    }
}
