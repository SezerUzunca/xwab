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

// UI support is outside core: it is not an application capability port.
include(":designsystem")
include(":shared")

// Each capability and feature is one cohesive module. Installed core modules are also wired
// automatically into shared's Metro classpath; their own architecture.properties defines the
// permitted dependencies and public ports. Adding/removing a module needs no central core list.
//
// Test fakes are split the same way, one module per port they stand in for, so a test compiles
// against only the capabilities it reads. They sit outside core for the reason UI support does.
val discoveredModules = listOf("core", "feature", "testing").associateWith { group ->
    rootDir.resolve(group).listFiles()
        ?.filter { it.isDirectory && it.resolve("build.gradle.kts").isFile }
        ?.sortedBy { it.name }
        ?.map { moduleDir -> ":$group:${moduleDir.name}" }
        .orEmpty()
}
discoveredModules.values.flatten().forEach { include(it) }

// Published once, here, rather than looked up again by the projects that need them. Reading them
// off `rootProject.subprojects` from inside another project reaches into state that project does
// not own, which Gradle's isolated projects mode refuses.
//
// `:shared` puts every installed capability on Metro's classpath.
gradle.extra["coreModules"] = discoveredModules.getValue("core")

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
