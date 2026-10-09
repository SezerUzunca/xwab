/**
 * The playback capability's contract: its port package, and nothing that implements it. Consumers
 * compile against this module; only the composition root installs the implementation.
 */
plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.playback.api" }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
    }
}
