plugins {
    // No Compose here: nothing in this module renders, and since every track is fetched over
    // HTTPS there are no bundled MP3 files needing the Compose Resources pipeline either.
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.audiocontent" }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            // The catalog repository publishes Flow.
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            api(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
