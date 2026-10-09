plugins {
    id("xwab.kmp.library")
}

kotlin {
    android {
        namespace = "com.xwab.app.core.favorites"
        // The Android graph takes a Context, which only a device has; its tests run there.
        withDeviceTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.favorites.api)
            // `implementation`, not `api`: where favorites are written is this module's business.
            // Nothing it publishes names a DataStore type — the module's own graph provides the
            // store and only `FavoritesPort` reaches the app graph — so DataStore stops here
            // instead of landing on the compile classpath of every feature that reads a favourite.
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
        }
    }
}
