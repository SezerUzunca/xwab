plugins {
    // No Compose here: nothing in this module renders, and since every track is fetched over
    // HTTPS there are no bundled MP3 files needing the Compose Resources pipeline either.
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.audiodelivery" }

    sourceSets {
        commonMain.dependencies {
            // Which tracks exist, and where their bytes come from, is the manifest's answer; this
            // module only fetches and stores them. `implementation` on purpose: no manifest type
            // appears in this module's own API, so nothing of it reaches a consumer transitively.
            implementation(projects.core.catalogManifest)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            // The DI entry points expose Koin's Module type.
            api(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
