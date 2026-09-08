plugins {
    // No Compose here: nothing in this module renders, and since every track is fetched over
    // HTTPS there are no bundled MP3 files needing the Compose Resources pipeline either.
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.sounddelivery" }

    sourceSets {
        commonMain.dependencies {
            // SoundContentPort exposes TrackId from the sound module.
            api(projects.core.sound)
            implementation(projects.core.network)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            implementation(libs.okio)
        }
        androidMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}
