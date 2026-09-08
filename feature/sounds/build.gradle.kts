plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.sounds" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.sound)
            implementation(projects.core.favorites)
            implementation(projects.core.session)
        }
        commonTest.dependencies {
            implementation(projects.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
