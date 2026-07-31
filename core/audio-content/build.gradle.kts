plugins {
    // Compose Multiplatform is applied for its resource pipeline only: the bundled MP3 files ship
    // as Compose Resources so one `Res.getUri` call works on both platforms.
    id("xwab.kmp.compose")
}

kotlin {
    android { namespace = "com.xwab.app.core.audiocontent" }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.compose.components.resources)
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

compose.resources {
    packageOfResClass = "com.xwab.app.core.audiocontent.generated.resources"
}
