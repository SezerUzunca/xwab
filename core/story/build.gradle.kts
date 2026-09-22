plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.story" }

    sourceSets {
        commonMain.dependencies {
            // The session owns the resolver contract; stories supply metadata and private stream
            // addresses. This module has no cache or transport implementation.
            implementation(projects.core.session)
            // StoryPort publishes Flow; its implementation owns the manifest.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
