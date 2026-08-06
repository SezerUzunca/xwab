plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.testing" }

    sourceSets {
        commonMain.dependencies {
            // The fakes implement the three ports every screen reads, and build catalog values.
            api(projects.core.catalog)
            api(projects.core.favorites)
            api(projects.core.playbackSession)
        }
    }
}
