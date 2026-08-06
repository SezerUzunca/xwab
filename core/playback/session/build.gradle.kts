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
            implementation(projects.core.sound.delivery)
            implementation(projects.core.playback.engine)
            // Read by the internal sound resolver only. Nothing this module publishes names a
            // catalog type — the session speaks `PlaybackItemId` — so this does not travel, and a
            // screen that wants `TrackId` declares the catalog itself, as all three already do.
            implementation(projects.core.sound.catalog)
            api(libs.kotlinx.coroutines.core)
            // The DI entry point exposes Koin's Module type.
            api(libs.koin.core)
        }
    }
}
