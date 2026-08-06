plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.category" }

    sourceSets {
        commonMain.dependencies {
            // The capabilities this screen reads; see `feature/home/build.gradle.kts`.
            implementation(projects.core.catalog)
            implementation(projects.core.favorites)
            implementation(projects.core.playbackSession)

            implementation(projects.feature.category.navigation)
            // Screens this one routes to — their navigation API only, never their implementation.
            implementation(projects.feature.player.navigation)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
