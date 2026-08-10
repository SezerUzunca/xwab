plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.player" }

    sourceSets {
        commonMain.dependencies {
            // The capabilities this screen reads; see `feature/sounds/build.gradle.kts`.
            implementation(projects.core.sound.catalog)
            implementation(projects.core.sound.favorites)
            implementation(projects.core.playback.session)

            implementation(projects.feature.player.navigation)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
