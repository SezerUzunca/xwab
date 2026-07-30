plugins {
    id("xwab.kmp.compose")
}

kotlin {
    android { namespace = "com.xwab.app.core.navigation" }

    sourceSets {
        commonMain.dependencies {
            api(libs.navigation3.runtime)
            api(libs.compose.runtime)
            // `FeatureEntry` hands the composition root a Koin module and a serializers module,
            // so both are part of this module's API.
            api(libs.koin.core)
            api(libs.kotlinx.serialization.core)
        }
    }
}
