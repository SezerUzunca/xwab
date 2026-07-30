plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.player" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.player.navigation)
        }
    }
}
