plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.nowplaying.api" }

    // Every dependency here names a type in this module's public signatures, so it is part of the
    // contract and published with `api`.
    sourceSets {
        commonMain.dependencies {
            api(projects.core.session.api)
            api(libs.compose.ui)
        }
    }
}
