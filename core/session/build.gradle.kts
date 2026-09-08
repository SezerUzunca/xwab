plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.session" }

    sourceSets {
        commonMain.dependencies {
            // Resolve content through core ports and drive the internal platform engine.
            implementation(projects.core.delivery)
            implementation(projects.core.story)
            implementation(projects.core.playback)
            // Content dependencies stay internal: the session publishes PlaybackItemId.
            implementation(projects.core.sound)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
