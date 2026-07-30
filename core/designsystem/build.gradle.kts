plugins {
    id("xwab.kmp.compose")
}

kotlin {
    android { namespace = "com.xwab.app.core.ui" }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.material.icons.extended)
            api(libs.compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
}
