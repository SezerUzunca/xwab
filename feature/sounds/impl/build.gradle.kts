plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.sounds.impl" }

    sourceSets {
        commonMain.dependencies {
            // The capabilities this screen reads; see `feature/category/impl/build.gradle.kts`.
            implementation(projects.core.sound.catalog)
            implementation(projects.core.sound.favorites)
            implementation(projects.core.playback.session)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
