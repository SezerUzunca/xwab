plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.favorites" }

    sourceSets {
        commonMain.dependencies {
            // `TrackId` is the catalog's, and it is in this module's public API.
            api(projects.core.sound.catalog)
            api(libs.androidx.datastore)
            api(libs.androidx.datastore.preferences)
            api(libs.koin.core)
            // The favorites repository publishes Flow.
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
