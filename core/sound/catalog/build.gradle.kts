plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.sound.catalog" }

    sourceSets {
        commonMain.dependencies {
            // SoundCatalogPort publishes Flow. This module declares the public contract only;
            // `core:sound:manifest` contributes its internal adapter.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
