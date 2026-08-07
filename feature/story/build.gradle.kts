plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.story" }

    sourceSets {
        commonMain.dependencies {
            // The capabilities this screen reads; see `feature/home/build.gradle.kts`.
            //
            // Two, not three: there is no `core:story:favorites`, and `core:story:manifest` — which
            // holds the address each story streams from — is off limits to every feature, the same
            // way the sound manifest is. `checkArchitecture` rule 4 fails the build on either.
            implementation(projects.core.story.catalog)
            implementation(projects.core.playback.session)

            implementation(projects.feature.story.navigation)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
