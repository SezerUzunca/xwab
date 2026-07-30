plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.category" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.category.navigation)
            // Screens this one routes to — their navigation API only, never their implementation.
            implementation(projects.feature.player.navigation)
        }
    }
}
