plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.story.catalog" }

    sourceSets {
        commonMain.dependencies {
            // StoryCatalogPort publishes Flow. This module declares the public contract only;
            // `core:story:manifest` contributes its internal adapter.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
