plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.browse" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.sound)
        }
        commonTest.dependencies {
            implementation(projects.testing.sound)
            implementation(libs.kotlinx.coroutines.test)
        }
        if (gradle.extra["enableIos"] as Boolean) {
            iosTest.dependencies {
                implementation(libs.compose.uiTest)
            }
        }
    }
}
