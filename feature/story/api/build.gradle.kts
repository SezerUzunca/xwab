plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.story.api" }

    // Every dependency here names a type in this module's public signatures, so it is part of the
    // contract and published with `api`.
    sourceSets {
        commonMain.dependencies {
            api(projects.core.story.api)
        }
    }
}
