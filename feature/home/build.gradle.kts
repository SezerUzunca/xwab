plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.home" }

    sourceSets {
        commonMain.dependencies {
            // The capabilities this screen reads. `xwab.kmp.feature` no longer hands every core
            // module to every feature, so what a screen can reach is exactly what it asks for here.
            // Delivery, the playback engine and the shipped manifest are not askable:
            // `checkArchitecture` rule 4 fails the build on a feature that declares any of them.
            implementation(projects.core.sound.catalog)
            implementation(projects.core.sound.favorites)
            implementation(projects.core.playback.session)

            implementation(projects.feature.home.navigation)
            // Screens this one routes to — their navigation API only, never their implementation.
            implementation(projects.feature.category.navigation)
            implementation(projects.feature.sounds.navigation)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
