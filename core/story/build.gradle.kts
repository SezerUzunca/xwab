plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.story" }

    sourceSets {
        commonMain.dependencies {
            // No delivery: a story streams over HTTPS and nothing is kept, so this module reaches
            // the source manifest and stops there.
            implementation(projects.core.resolution)
            implementation(projects.core.sources)
            // StoryPort publishes Flow; its implementation owns the manifest.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
