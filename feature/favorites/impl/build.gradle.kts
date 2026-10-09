plugins {
    id("xwab.kmp.feature.impl")
}

kotlin {
    android { namespace = "com.xwab.app.feature.favorites" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.sound.api)
            implementation(projects.core.favorites.api)
            implementation(projects.core.session.api)
        }
        commonTest.dependencies {
            implementation(projects.testing.sound)
            implementation(projects.testing.session)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
