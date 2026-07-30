plugins {
    id("xwab.kmp.compose")
}

kotlin {
    android { namespace = "com.xwab.app.core.data" }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.compose.components.resources)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kermit)
            api(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}

compose.resources {
    packageOfResClass = "com.xwab.app.core.data.generated.resources"
}
