plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.browse.api" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.sound.api)
            implementation(libs.compose.components.resources)
        }
    }
}
