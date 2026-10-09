plugins {
    id("xwab.kmp.feature.impl")
}

kotlin {
    android { namespace = "com.xwab.app.feature.story" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.story.api)
            implementation(projects.core.session.api)
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
