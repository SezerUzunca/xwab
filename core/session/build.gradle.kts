plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.session" }

    sourceSets {
        commonMain.dependencies {
            // The session owns its resolver contract. Content modules implement it without a
            // dependency back from this module; only the platform engine is needed here.
            implementation(projects.core.playback)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
