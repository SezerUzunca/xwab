plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.storysource" }

    sourceSets {
        commonMain.dependencies {
            // Story models used by the manifest and by the public source port.
            api(projects.core.story.catalog)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
