plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.preferences" }

    sourceSets {
        commonMain.dependencies {
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
