plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.category" }

    sourceSets {
        commonMain.dependencies {
            // The capabilities this screen reads; see `feature/home/build.gradle.kts`.
            implementation(projects.core.sound.catalog)
            implementation(projects.core.sound.favorites)
            implementation(projects.core.playback.session)

            implementation(projects.feature.category.navigation)
            // Screens this one routes to — their navigation API only, never their implementation.
            implementation(projects.feature.sounds.navigation)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
