plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.testing" }

    sourceSets {
        commonMain.dependencies {
            // The fakes implement the domain ports and build model objects, so both are API.
            api(projects.core.domain)
            api(projects.core.model)
        }
    }
}
