plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.playbacksession" }

    sourceSets {
        commonMain.dependencies {
            // The two modules a session is assembled from, and the only place either is declared
            // outside the composition root. No feature depends on them, so no screen can resolve a
            // track to a URI or reach the engine's own state model.
            implementation(projects.core.audioDelivery)
            implementation(projects.core.playbackEngine)
            // `togglePlayback` takes a catalog track, and `PlaybackSummary` is published from here.
            api(projects.core.catalog)
            api(libs.kotlinx.coroutines.core)
            // The DI entry point exposes Koin's Module type.
            api(libs.koin.core)
        }
    }
}
