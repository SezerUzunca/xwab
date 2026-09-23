plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.testing.sound" }

    sourceSets {
        commonMain.dependencies {
            // The sound catalog fake and the builders for its values. Favorites come along because
            // this module seeds them under the namespace sounds are saved in; the fake itself is
            // namespace-neutral and lives in `:testing:favorites`.
            api(projects.core.sound)
            api(projects.testing.favorites)
        }
    }
}
