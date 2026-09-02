plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.browse.impl" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.sound.catalog)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
