plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.testing" }

    sourceSets {
        commonMain.dependencies {
            // The fakes implement the three ports every screen reads, and build catalog values.
            api(projects.core.sound)
            api(projects.core.favorites)
            api(projects.core.session)
        }
    }
}
