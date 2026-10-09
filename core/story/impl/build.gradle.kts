plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.story" }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.story.api)
            // The session owns the resolver contract; stories supply metadata and private stream
            // addresses. This module has no cache or transport implementation.
            implementation(projects.core.session.api)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
