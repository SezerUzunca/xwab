plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.catalogmanifest" }

    sourceSets {
        commonMain.dependencies {
            // The types the manifest is written in, and the repository interface it implements.
            api(projects.core.sound.catalog)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
