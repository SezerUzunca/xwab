package com.xwab.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

/**
 * The plumbing the `xwab.*` conventions share.
 *
 * These conventions are `Plugin<Project>` classes rather than precompiled `.gradle.kts` script
 * plugins because compiling those needs Gradle's `kotlin-dsl` plugin, which is published only to
 * plugins.gradle.org — a host this build never talks to. See `build-logic/settings.gradle.kts`.
 *
 * The cost is that the conventions cannot use the generated DSL accessors a build script gets, so
 * the helpers below stand in for them.
 */

internal val Project.libs: VersionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

/**
 * The flag the root `settings.gradle.kts` publishes: iOS binaries can only be produced on macOS,
 * so their KLIB dependency graphs are not even configured elsewhere.
 */
internal val Project.iosEnabled: Boolean
    get() = gradle.extensions.extraProperties.get("enableIos") as Boolean

/** Stands in for a build script's `kotlin { }` block. */
internal fun Project.kotlinMultiplatform(configure: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(KotlinMultiplatformExtension::class.java) { it.configure() }
}

/**
 * Stands in for `kotlin { android { } }`.
 *
 * AGP adds the Android target to the Kotlin extension under this name when its plugin is applied,
 * so the target is already there by the time a convention reaches for it.
 */
internal fun KotlinMultiplatformExtension.android(
    configure: KotlinMultiplatformAndroidLibraryTarget.() -> Unit,
) {
    val android = extensions.getByName("android") as KotlinMultiplatformAndroidLibraryTarget
    android.configure()
}

/** Stands in for `kotlin { sourceSets { commonMain.dependencies { } } }`. */
internal fun KotlinMultiplatformExtension.dependenciesOf(
    sourceSetName: String,
    configure: KotlinDependencyHandler.() -> Unit,
) {
    sourceSets.getByName(sourceSetName)
        .dependencies(Action<KotlinDependencyHandler> { it.configure() })
}
