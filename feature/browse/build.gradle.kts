plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.browse" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.sound.catalog)
        }
        commonTest.dependencies {
            implementation(projects.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
