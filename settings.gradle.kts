rootProject.name = "XWAB"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// iOS binaries can only be produced on macOS. Avoid configuring their KLIB
// dependency graphs during Android development on other hosts. Use
// -PenableIos=true on macOS to explicitly enable them when needed.
gradle.extra["enableIos"] = startParameter.projectProperties["enableIos"]?.toBoolean()
    ?: System.getProperty("os.name").contains("Mac", ignoreCase = true)

pluginManagement {
    // Convention plugins (`xwab.*`) that every module applies instead of repeating build config.
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }
}

include(":androidApp")

// Core modules are grouped by the content they serve rather than listed flat. `sound`, `story` and
// `playback` are directories with no build file of their own — Gradle creates a container project
// for each, and nothing is ever declared on it. A module's Gradle path is its directory path, so
// `core/sound/manifest` is `:core:sound:manifest`, and the architecture rules read that path.
include(":core:sound:catalog")
include(":core:sound:manifest")
include(":core:sound:delivery")
include(":core:sound:favorites")
include(":core:story:catalog")
include(":core:story:manifest")
include(":core:playback:session")
include(":core:playback:engine")

// Crosscutting: used by every slice, tied to no content type, so grouped under none of them.
include(":core:network")
include(":core:designsystem")
include(":core:navigation")
include(":core:testing")
include(":shared")

// Feature modules are discovered, not listed: `tools/new-feature.ps1` writes the directories and
// they are part of the build on the next sync. A feature is a directory under `feature/` with a
// build file, optionally holding a `navigation/` API module beside it.
rootDir.resolve("feature").listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { featureDir ->
        include(":feature:${featureDir.name}")
        if (featureDir.resolve("navigation/build.gradle.kts").exists()) {
            include(":feature:${featureDir.name}:navigation")
        }
    }
