rootProject.name = "build-logic"

/*
 * This build resolves everything from Google's Maven and Maven Central; plugins.gradle.org is
 * never reached. Both repository blocks below are declared explicitly for that reason — without a
 * `pluginManagement` block Gradle defaults to the plugin portal alone.
 *
 * The same constraint is why the conventions here are plain `Plugin<Project>` classes rather than
 * precompiled `.gradle.kts` script plugins: compiling those needs Gradle's `kotlin-dsl` plugin,
 * which is published to the portal and nowhere else.
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    // The conventions read versions from the same catalog the modules use, so there is exactly
    // one place where a version is declared.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
