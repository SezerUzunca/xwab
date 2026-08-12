plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.category.impl" }

    sourceSets {
        commonMain.dependencies {
            // Feature-owned capabilities are explicit; delivery and engine adapters stay hidden.
            implementation(projects.core.sound.catalog)
            implementation(projects.core.sound.favorites)
            implementation(projects.core.playback.session)
            implementation(projects.feature.category.api)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
