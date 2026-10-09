plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.story.api" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.story.api)
        }
    }
}
