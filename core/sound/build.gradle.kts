plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.sound" }

    sourceSets {
        commonMain.dependencies {
            // What playing a sound means is this module's business, so the resolver lives here and
            // reaches delivery and the source manifest itself. All three are `implementation`:
            // nothing this module publishes names a type from any of them, so the two that are off
            // limits to features stop here instead of travelling on to every screen.
            implementation(projects.core.resolution)
            implementation(projects.core.sources)
            implementation(projects.core.delivery)
            // SoundPort publishes Flow; its implementation owns the manifest.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
