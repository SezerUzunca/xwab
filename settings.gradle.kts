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

// Each content module owns its catalog, manifest and internal adapters.
// Delivery and favorites remain independent capabilities.
include(":core:sound")
include(":core:delivery")
include(":core:favorites")
include(":core:story")

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
