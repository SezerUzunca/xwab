plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.navigation" }

    sourceSets {
        commonMain.dependencies {
            // `ComponentContext` is part of `componentScope()`'s public signature, so it must
            // travel to every consumer rather than stop at this module.
            api(libs.decompose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
