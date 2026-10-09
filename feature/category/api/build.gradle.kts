plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.category.api" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.sound)
        }
    }
}
