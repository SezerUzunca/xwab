plugins {
    id("xwab.kmp.compose")
}

kotlin {
    android { namespace = "com.xwab.app.designsystem" }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            // Drawn inside the components here, never part of their signatures, so it stays out of
            // every feature's compile classpath. The shell declares its own for the tab icons.
            implementation(libs.compose.material.icons.extended)
            api(libs.compose.components.resources)
        }
    }
}

// The IDE needs the rendering runtime when previewing components in this module.
val uiTooling = libs.compose.uiTooling.get()
dependencies.add(
    "androidRuntimeClasspath",
    "${uiTooling.module}:${uiTooling.versionConstraint.requiredVersion}",
)

compose.resources {
    publicResClass = true
}
