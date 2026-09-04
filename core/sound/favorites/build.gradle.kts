plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.favorites" }

    sourceSets {
        commonMain.dependencies {
            // `TrackId` is the catalog's, and it is in this module's public API.
            api(projects.core.sound.catalog)
            // `implementation`, not `api`: where favorites are written is this module's business.
            // Nothing it publishes names a DataStore type any more — the platform halves bind
            // `FavoritesRepository` itself — so DataStore stops here instead of landing on the
            // compile classpath of every feature that reads a favourite.
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            // The favorites repository publishes Flow.
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
        }
    }
}
