plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.category" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.sound.catalog)
            implementation(projects.core.sound.favorites)
            implementation(projects.core.playback.session)
        }
        commonTest.dependencies {
            implementation(projects.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
