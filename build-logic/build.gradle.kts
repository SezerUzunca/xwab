import org.gradle.plugin.use.PluginDependency
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlinJvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

/**
 * A convention plugin cannot declare a plugin *version* — it can only apply a plugin that is
 * already on its own classpath. Gradle publishes every plugin under a marker artifact named after
 * its id, so a catalog `[plugins]` entry maps onto a normal dependency coordinate.
 */
fun pluginMarker(plugin: Provider<PluginDependency>): String = plugin.get().run {
    "$pluginId:$pluginId.gradle.plugin:${version.requiredVersion}"
}

dependencies {
    implementation(pluginMarker(libs.plugins.kotlinMultiplatform))
    implementation(pluginMarker(libs.plugins.androidMultiplatformLibrary))
    implementation(pluginMarker(libs.plugins.composeMultiplatform))
    implementation(pluginMarker(libs.plugins.composeCompiler))
    implementation(pluginMarker(libs.plugins.kotlinxSerialization))
    implementation(pluginMarker(libs.plugins.metro))

    // `FeatureFirstRules` is plain Kotlin over a dependency map, so its tests need nothing from
    // Gradle. Left on the default JUnit 4 runner `kotlin-test` picks for the JVM.
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "xwab.kmp.library"
            implementationClass = "com.xwab.convention.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "xwab.kmp.compose"
            implementationClass = "com.xwab.convention.KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "xwab.kmp.feature"
            implementationClass = "com.xwab.convention.KmpFeatureConventionPlugin"
        }
        register("kmpFeatureApi") {
            id = "xwab.kmp.feature.api"
            implementationClass = "com.xwab.convention.KmpFeatureApiConventionPlugin"
        }
        register("architecture") {
            id = "xwab.architecture"
            implementationClass = "com.xwab.convention.ArchitectureConventionPlugin"
        }
    }
}
