plugins {
    id("xwab.kmp.feature.impl")
}

kotlin {
    android { namespace = "com.xwab.app.feature.cantsleep" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.guide.api)
            implementation(projects.core.session.api)
        }
        commonTest.dependencies {
            implementation(projects.testing.session)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
