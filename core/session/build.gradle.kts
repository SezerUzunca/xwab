plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.session" }

    sourceSets {
        commonMain.dependencies {
            // Two dependencies, and neither is a content type. The session drives the platform
            // engine and resolves whatever it was handed through the contract in `:core:resolution`
            // — content modules contribute their own resolvers into the map it reads, so adding or
            // removing a content type never reaches this module. `checkArchitecture` holds this
            // list to exactly these two.
            implementation(projects.core.resolution)
            implementation(projects.core.playback)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
