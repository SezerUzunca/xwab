plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.nowplaying" }

    sourceSets {
        commonMain.dependencies {
            // One port. This feature has nothing to say about sounds or stories — the session
            // already names what it is on, which is the whole reason this can be one screen.
            implementation(projects.core.session)
        }
        commonTest.dependencies {
            implementation(projects.testing.session)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
