plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.soundsource" }

    sourceSets {
        commonMain.dependencies {
            // Sound models used by the manifest and by the public source port.
            api(projects.core.sound.catalog)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
