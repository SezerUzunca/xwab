import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Reports this module's project dependencies for `checkArchitecture`, as every module does.
    id("xwab.architecture.module")
    // detekt, as every module applies it.
    id("xwab.detekt")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.xwab.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.xwab.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            // AGP 9.3's optimization DSL, which replaces `isMinifyEnabled` + `isShrinkResources`
            // and brings the default Android keep rules with it, so no `proguardFiles` line is
            // needed. Anything this app turns out to need goes in `src/main/keepRules/*.keep`.
            //
            // R8 runs in full mode by default, which assumes code is not reached reflectively.
            // Every library here either ships its own consumer rules — media3-exoplayer,
            // media3-datasource, datastore, navigation3-runtime and okhttp each carry a
            // `proguard.txt`, kotlinx-serialization a `META-INF/proguard` file and
            // kotlinx-coroutines-android an R8 one — or resolves at compile time, as Metro does.
            // ExoPlayer is designed to be shrunk rather than kept, so it asks for no rules at all.
            //
            // The one library that carries nothing is `ktor-client-okhttp`, which is found through
            // a `META-INF/services` entry rather than by any reference R8 can see. R8 reads those
            // files itself, so no rule is written here on the guess that it doesn't: whether
            // `OkHttpEngineContainer` survives is checked in the built release APK instead. Adding
            // a keep rule that R8 did not need would cost the optimization it does make there.
            optimization {
                enable = true
            }
        }
    }
    lint {
        // Lint this module's project dependencies as well, so the one CI lint run reads every
        // module the app ships rather than the two activity classes that live here. It only
        // reaches a KMP library that applies `com.android.lint`, which `xwab.kmp.library` and
        // `:shared` do; the findings land in this module's report, which CI already prints.
        checkDependencies = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
