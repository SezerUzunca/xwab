plugins {
    id("xwab.kmp.compose")
}

kotlin {
    android { namespace = "com.xwab.app.core.navigation" }

    sourceSets {
        commonMain.dependencies {
            api(libs.navigation3.runtime)
            // `toEntries` is a public `@Composable` returning `NavEntry`, so both are API surface.
            api(libs.compose.runtime)
            // Used only inside `toEntries`, to decorate each tab's entries with a ViewModelStore.
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
        }
    }
}
