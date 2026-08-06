plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.catalogmanifest" }

    sourceSets {
        commonMain.dependencies {
            // The types the manifest is written in, and the repository interface it implements.
            api(projects.core.catalog)
            api(libs.kotlinx.coroutines.core)
            // The DI entry point exposes Koin's Module type.
            api(libs.koin.core)
        }
    }
}
