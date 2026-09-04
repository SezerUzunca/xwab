plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.storymanifest" }

    sourceSets {
        commonMain.dependencies {
            // The types the manifest is written in, and the repository interface it implements.
            api(projects.core.story.catalog)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
