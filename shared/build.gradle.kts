import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * The composition root configures its own targets rather than applying `xwab.kmp.compose`.
 *
 * It is the only module that produces an iOS framework binary, and the only one whose host tests
 * need Android resources. It owns application wiring and the app shell; feature UI lives in the
 * feature modules it assembles.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

compose.resources {
    packageOfResClass = "xwab.shared.generated.resources"
}

kotlin {
    if (gradle.extra["enableIos"] as Boolean) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    android {
        namespace = "com.xwab.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // The composition root binds every core adapter. Features cannot reach the delivery
            // layer, engine, manifests or network directly; the root can wire their Koin modules.
            implementation(projects.core.sound.catalog)
            implementation(projects.core.sound.manifest)
            implementation(projects.core.sound.delivery)
            implementation(projects.core.story.catalog)
            implementation(projects.core.story.manifest)
            implementation(projects.core.sound.favorites)
            implementation(projects.core.playback.session)
            implementation(projects.core.playback.engine)
            implementation(projects.core.network)
            implementation(projects.core.designsystem)

            // Browse, Favorites and Story publish no api Config of their own — AppTab and each
            // *TabConfig.Root already say everything those screens need as navigation input.
            implementation(projects.feature.browse.impl)
            implementation(projects.feature.favorites.impl)
            implementation(projects.feature.category.api)
            implementation(projects.feature.category.impl)
            implementation(projects.feature.sounds.api)
            implementation(projects.feature.sounds.impl)
            implementation(projects.feature.story.impl)

            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.koin.core)
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(projects.core.testing)
            // The root's navigation contract tests deliberately assert that every published
            // Config round-trips through serialization and resolves to the right component.
            implementation(projects.feature.category.api)
            implementation(projects.feature.sounds.api)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.koin.test)
        }
    }
}
