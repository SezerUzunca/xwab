plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.domain" }

    sourceSets {
        commonMain.dependencies {
            // Deliberately framework-free: ports and use cases are plain Kotlin. No DI
            // container here — the composition root in `shared` owns the wiring.
            api(projects.core.model)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
