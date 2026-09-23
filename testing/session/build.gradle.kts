plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.testing.session" }

    sourceSets {
        commonMain.dependencies {
            // The playback session is content-neutral, and so is its fake: a story or now-playing
            // test that declares this module compiles against no catalog at all.
            api(projects.core.session)
        }
    }
}
