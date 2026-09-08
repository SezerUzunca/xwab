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

// Content capabilities are grouped by the content they serve rather than listed flat. `sound` and
// `story` are directories with no build file of their own — Gradle creates a container project for
// each, and nothing is ever declared on it. A module's Gradle path is its directory path, so
// `core/sound/manifest` is `:core:sound:manifest`, and the architecture rules read that path.
include(":core:sound:catalog")
include(":core:sound:manifest")
include(":core:sound:delivery")
include(":core:sound:favorites")
include(":core:story:catalog")
include(":core:story:manifest")

// Playback is two flat modules rather than a content group, because the halves are not the same
// kind of thing. `:core:playback` is a standalone audio library that names no module of this app;
// `:core:session` is the one playback session the app runs, and the only half a feature may reach.
include(":core:playback")
include(":core:session")

// Crosscutting transport capability, tied to no content type.
include(":core:network")
// UI and test support are outside core because they are not application capability ports.
include(":designsystem")
include(":testing")
include(":shared")

// A feature is one cohesive module. Its Navigation 3 route is the only public contract; screen,
// state and presentation logic remain internal in the same module.
rootDir.resolve("feature").listFiles()
    ?.filter {
        it.isDirectory && it.resolve("build.gradle.kts").exists()
    }
    ?.sortedBy { it.name }
    ?.forEach { featureDir ->
        include(":feature:${featureDir.name}")
    }
