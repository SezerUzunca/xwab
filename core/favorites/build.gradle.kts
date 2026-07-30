plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.favorites" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            api(libs.koin.core)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kermit)
        }
    }
}
