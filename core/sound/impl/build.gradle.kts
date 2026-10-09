plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.sound" }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.sound.api)
            // This module owns its physical sources and contributes its playback resolver.
            // Delivery contracts stay implementation-only so features see metadata, never URLs.
            implementation(projects.core.session.api)
            implementation(projects.core.delivery.api)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
