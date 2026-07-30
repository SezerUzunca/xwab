plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.home" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.home.navigation)
            // Screens this one routes to — their navigation API only, never their implementation.
            implementation(projects.feature.category.navigation)
            implementation(projects.feature.player.navigation)
        }
    }
}
