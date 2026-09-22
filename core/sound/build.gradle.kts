plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.sound" }

    sourceSets {
        commonMain.dependencies {
            // This module owns its physical sources and contributes its playback resolver.
            // Delivery contracts stay implementation-only so features see metadata, never URLs.
            implementation(projects.core.session)
            implementation(projects.core.delivery)
            // SoundPort publishes Flow; its implementation owns the manifest.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
