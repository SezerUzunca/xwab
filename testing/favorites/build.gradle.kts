plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.testing.favorites" }

    sourceSets {
        commonMain.dependencies {
            // One port, and no content type: favorites store whatever namespace a caller owns.
            api(projects.core.favorites)
        }
    }
}
