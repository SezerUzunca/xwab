plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.story" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.story.catalog)
            implementation(projects.core.playback.session)
        }
        commonTest.dependencies {
            implementation(projects.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
