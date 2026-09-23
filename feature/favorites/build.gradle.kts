plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.favorites" }

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
    }
}
