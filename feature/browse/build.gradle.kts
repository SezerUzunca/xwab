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
            implementation(projects.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
        // The one place the app runs a screen on Android rather than on the simulator. What it is
        // for is the host's own constraints — a lazy key written into a `Bundle` is the reason it
        // exists — so it lives beside the screen that hit one, not in every feature.
        getByName("androidHostTest").dependencies {
            implementation(libs.compose.uiTestJUnit4)
            implementation(libs.robolectric)
        }
    }
}
