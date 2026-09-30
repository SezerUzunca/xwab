plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.nowplaying" }

    sourceSets {
        commonMain.dependencies {
            // One port. This feature has nothing to say about sounds or stories — the session
            // already names what it is on, which is the whole reason one bar serves every kind.
            implementation(projects.core.session)
        }
        commonTest.dependencies {
            implementation(projects.testing.session)
            implementation(libs.kotlinx.coroutines.test)
        }
        if (gradle.extra["enableIos"] as Boolean) {
            iosTest.dependencies {
                implementation(libs.compose.uiTest)
            }
        }
    }
}
