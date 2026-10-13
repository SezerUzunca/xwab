plugins {
    id("xwab.kmp.library")
    id("xwab.metro")
}

kotlin {
    android { namespace = "com.xwab.app.core.shuffle" }

    sourceSets {
        commonMain.dependencies {
            // The contract this module implements.
            implementation(projects.core.shuffle.api)
            // The session owns the resolver contract; this module answers it with a track that
            // speech makes from the words it picks. It has no voice, file or network of its own.
            implementation(projects.core.session.api)
            implementation(projects.core.speech.api)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
