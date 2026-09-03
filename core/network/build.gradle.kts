plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.network" }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // Ktor selects the only engine present on this target when HttpClient() is created.
            implementation(libs.ktor.client.okhttp)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.core)
        }
        if (gradle.extra["enableIos"] as Boolean) {
            iosMain.dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
