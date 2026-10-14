plugins {
    id("xwab.kmp.library")
    id("xwab.metro")
}

kotlin {
    android { namespace = "com.xwab.app.core.nap" }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.nap.api)
            // The session owns the resolver contract; this module answers it with a track speech
            // makes from what each nap says. It has no voice, file, clock or alarm of its own.
            implementation(projects.core.session.api)
            implementation(projects.core.speech.api)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
