plugins {
    id("xwab.kmp.library")
    id("xwab.metro")
}

kotlin {
    android { namespace = "com.xwab.app.core.rehearsal" }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.rehearsal.api)
            // The listener's script is kept as a note under this module's own key, and voiced by
            // speech into a track the session's resolver contract hands to the player. It has no
            // file, voice or network of its own.
            implementation(projects.core.notes.api)
            implementation(projects.core.session.api)
            implementation(projects.core.speech.api)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
