plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.nowplaying.api" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.session)
            implementation(libs.compose.ui)
        }
    }
}
