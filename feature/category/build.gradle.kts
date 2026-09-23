plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.category" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.sound)
            implementation(projects.core.favorites)
            implementation(projects.core.session)
        }
        commonTest.dependencies {
            implementation(projects.testing.sound)
            implementation(projects.testing.session)
            implementation(libs.kotlinx.coroutines.test)
        }
        // Screen tests live in the iOS source set because the screens themselves are common code:
        // what the simulator draws is the same composable Android draws. Running them on the
        // Android side instead would mean a Robolectric runtime for host tests, or an emulator in
        // CI — a second test harness for the same assertions. Guarded because the iOS targets are
        // only configured on macOS, so this source set does not exist on an Android-only build.
        if (gradle.extra["enableIos"] as Boolean) {
            iosTest.dependencies {
                implementation(libs.compose.uiTest)
            }
        }
    }
}
