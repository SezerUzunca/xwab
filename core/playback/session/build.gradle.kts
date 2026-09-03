plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.playbacksession" }

    sourceSets {
        commonMain.dependencies {
            // Where each kind of item comes from, and the player that opens it. This is the only
            // place these are declared outside the composition root: no feature depends on any of
            // them, so no screen can resolve an item to a URL or reach the engine's state model.
            implementation(projects.core.sound.delivery)
            implementation(projects.core.story.manifest)
            implementation(projects.core.playback.engine)
            // The two catalogs, read by the internal resolvers for titles and narrators. Nothing
            // this module publishes names a type from either — the session speaks `PlaybackItemId`
            // — so neither travels, and a screen that wants `TrackId` declares the catalog itself.
            implementation(projects.core.sound.catalog)
            implementation(projects.core.story.catalog)
            api(libs.kotlinx.coroutines.core)
            // The DI entry point exposes Koin's Module type.
        }
    }
}
