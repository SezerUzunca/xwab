plugins {
    // No Compose here: nothing in this module renders, and since every track is fetched over
    // HTTPS there are no bundled MP3 files needing the Compose Resources pipeline either.
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.audiodelivery" }

    sourceSets {
        commonMain.dependencies {
            // `AudioContentResolver.resolve` takes a `TrackId`, so this module's public API is
            // partly the catalog's — `api`, or a consumer would have to declare catalog itself to
            // call it. Today the one consumer happens to; a second would not have been so lucky.
            api(projects.core.sound.catalog)
            // Where the bytes come from is the manifest's answer. `implementation` on purpose: no
            // manifest type appears in this module's API, so none of it reaches a consumer.
            implementation(projects.core.sound.manifest)
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
