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
        mavenCentral()
    }
}

include(":androidApp")

// UI support is outside core: it is not an application capability port.
include(":designsystem")
// The app shell, which sees feature api modules only, and the composition root above it, the one
// module that sees every implementation and declares the graphs that collect them.
include(":shared")
include(":composition")

// Each capability and feature is one cohesive module. Installed core modules are also wired
// automatically into the composition root's Metro classpath; their own architecture.properties defines the
// permitted dependencies and public ports. Adding/removing a module needs no central core list.
//
// A feature is a pair, `feature/<name>/api` and `feature/<name>/impl`: a directory without a build
// script is searched one level further, so the pair is found the same way a flat module is.
//
// Test fakes are split the same way, one module per port they stand in for, so a test compiles
// against only the capabilities it reads. They sit outside core for the reason UI support does.
fun modulesIn(dir: File, path: String, depth: Int): List<String> = when {
    dir.resolve("build.gradle.kts").isFile -> listOf(path)
    depth == 0 -> emptyList()
    else -> dir.listFiles()
        ?.filter { it.isDirectory && it.name != "build" }
        ?.sortedBy { it.name }
        ?.flatMap { modulesIn(it, "$path:${it.name}", depth - 1) }
        .orEmpty()
}
val discoveredModules = listOf("core", "feature", "testing").associateWith { group ->
    rootDir.resolve(group).listFiles()
        ?.filter { it.isDirectory && it.name != "build" }
        ?.sortedBy { it.name }
        ?.flatMap { moduleDir -> modulesIn(moduleDir, ":$group:${moduleDir.name}", depth = 1) }
        .orEmpty()
}
discoveredModules.values.flatten().forEach { include(it) }

// Published once, here, rather than looked up again by the projects that need them. Reading them
// off `rootProject.subprojects` from inside another project reaches into state that project does
// not own, which Gradle's isolated projects mode refuses.
//
// `:composition` puts every installed capability and feature on Metro's classpath; `:shared`, the
// shell, takes only the feature api modules.
gradle.extra["coreModules"] = discoveredModules.getValue("core")
gradle.extra["featureModules"] = discoveredModules.getValue("feature")
gradle.extra["featureApiModules"] = discoveredModules.getValue("feature").filter { it.endsWith(":api") }

// `checkArchitecture` reads the dependencies each of these reports about itself. Every project
// with a build script is one; the directories that only group modules (`core/`, `feature/`,
// `testing/`) configure nothing and have nothing to report.
fun ProjectDescriptor.withDescendants(): List<ProjectDescriptor> =
    listOf(this) + children.flatMap { it.withDescendants() }
gradle.extra["architectureModules"] = rootProject.children
    .flatMap { it.withDescendants() }
    .filter { it.projectDir.resolve("build.gradle.kts").isFile }
    .map { it.path }
    .sorted()
