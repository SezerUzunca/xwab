<#
.SYNOPSIS
    Writes the skeleton of a new feature slice.

.DESCRIPTION
    Creates feature/<name>/api and feature/<name>/impl in the same shape as Now in Android. The
    API owns the serializable Config the composition root places in the navigation tree; the
    implementation owns UI, state, the Decompose component and DI. Gradle discovers both modules
    automatically.

    The generated feature is intentionally not self-registering. The script prints the explicit
    app composition steps and requires choosing either a top-level destination or an existing
    feature intent that the composition root will connect to it.

.PARAMETER Name
    Lower-case, dash-separated directory name, for example 'favorites' or 'sleep-timer'.

.EXAMPLE
    ./tools/new-feature.ps1 favorites
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidatePattern('^[a-z][a-z0-9]*(-[a-z0-9]+)*$')]
    [string]$Name
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$featureDir = Join-Path $repoRoot "feature\$Name"

if (Test-Path $featureDir) {
    throw "feature/$Name already exists. Pick another name or delete it first."
}

$parts = $Name.Split('-')
$Pascal = ($parts | ForEach-Object { $_.Substring(0, 1).ToUpper() + $_.Substring(1) }) -join ''
$camel = $Pascal.Substring(0, 1).ToLower() + $Pascal.Substring(1)
$pkg = $parts -join ''

function Write-GeneratedFile {
    param([string]$Path, [string]$Content)

    $dir = Split-Path -Parent $Path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }

    # A here-string drops the newline before its terminator; every file here should end with one.
    if (-not $Content.EndsWith("`n")) { $Content += "`n" }

    # No BOM: the Kotlin and Gradle files here are read by tools that expect plain UTF-8.
    [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "  created $($Path.Substring($repoRoot.Length + 1))"
}

$mainSrc = Join-Path $featureDir "impl\src\commonMain\kotlin\com\xwab\app\feature\$pkg\impl"
$testSrc = Join-Path $featureDir "impl\src\commonTest\kotlin\com\xwab\app\feature\$pkg\impl"
$navSrc = Join-Path $featureDir "api\src\commonMain\kotlin\com\xwab\app\feature\$pkg\api\navigation"

Write-Host "Creating feature '$Name'..."

Write-GeneratedFile (Join-Path $featureDir "impl\build.gradle.kts") @"
plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.${pkg}.impl" }

    sourceSets {
        commonMain.dependencies {
            // The capabilities this screen reads. `xwab.kmp.feature` deliberately hands out none
            // of them, so declare what you actually use:
            //     implementation(projects.core.sound.catalog)
            //     implementation(projects.core.sound.favorites)
            //     implementation(projects.core.playback.session)
            //
            // Delivery, the playback engine and the shipped manifest are not on the menu —
            // `checkArchitecture` rule 4 refuses a feature that declares any of them.

            implementation(projects.feature.${camel}.api)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
"@

Write-GeneratedFile (Join-Path $featureDir "api\build.gradle.kts") @"
plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.${pkg}.api" }
}
"@

Write-GeneratedFile (Join-Path $navSrc "${Pascal}Config.kt") @"
package com.xwab.app.feature.${pkg}.api.navigation

import kotlinx.serialization.Serializable

/** What this screen needs to show. Add fields for whatever it takes as input. */
@Serializable
data object ${Pascal}Config
"@

Write-GeneratedFile (Join-Path $mainSrc "${Pascal}State.kt") @"
package com.xwab.app.feature.${pkg}.impl

data class ${Pascal}State(
    val title: String = "${Pascal}",
)
"@

Write-GeneratedFile (Join-Path $mainSrc "${Pascal}Component.kt") @"
package com.xwab.app.feature.${pkg}.impl

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ${Pascal}Component {
    val state: StateFlow<${Pascal}State>
    val onBack: () -> Unit
}

class Default${Pascal}Component(
    componentContext: ComponentContext,
    override val onBack: () -> Unit,
) : ${Pascal}Component, ComponentContext by componentContext {
    // Use componentScope() (from :core:navigation) instead of viewModelScope for anything hot.
    override val state: StateFlow<${Pascal}State> = MutableStateFlow(${Pascal}State()).asStateFlow()
}
"@

Write-GeneratedFile (Join-Path $mainSrc "${Pascal}Screen.kt") @"
package com.xwab.app.feature.${pkg}.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ${Pascal}ScreenRoute(component: ${Pascal}Component) {
    val state by component.state.collectAsStateWithLifecycle()

    ${Pascal}Screen(state = state, onBack = component.onBack)
}

@Composable
internal fun ${Pascal}Screen(
    state: ${Pascal}State,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = state.title)
    }
}
"@

Write-GeneratedFile (Join-Path $mainSrc "di\${Pascal}Module.kt") @"
package com.xwab.app.feature.${pkg}.impl.di

import org.koin.dsl.module

/**
 * Objects only — the component is never Koin-managed. It is constructed directly by whichever
 * ``composition/*.kt`` factory in :shared places this feature in the navigation tree.
 */
val ${camel}Module = module {
    // This screen's own use cases are bound here too, never in a shared core module:
    //     factory { Observe${Pascal}ContentUseCase(get(), get()) }
}
"@

Write-GeneratedFile (Join-Path $testSrc "${Pascal}ComponentTest.kt") @"
package com.xwab.app.feature.${pkg}.impl

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The port fakes from core:testing are on this module's test classpath — the generated build file
 * declares it. Add the capability modules this screen reads beside it in commonMain.
 */
class ${Pascal}ComponentTest {
    @Test
    fun theScreenStartsFromItsInitialState() {
        val component = Default${Pascal}Component(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            onBack = {},
        )

        assertEquals(${Pascal}State(), component.state.value)
    }
}
"@

Write-Host ""
Write-Host "Done. The app composition must now wire this feature explicitly:" -ForegroundColor Green
Write-Host "  1. shared/build.gradle.kts, commonMain.dependencies:"
Write-Host "         implementation(projects.feature.${camel}.api)"
Write-Host "         implementation(projects.feature.${camel}.impl)"
Write-Host "  2. shared/src/commonMain/kotlin/com/xwab/app/di/AppModules.kt:"
Write-Host "         add ${camel}Module to featureModules (and import it)"
Write-Host "  3. Make the screen reachable; choose exactly one:" -ForegroundColor Yellow
Write-Host "         TOP LEVEL: give it its own tab — a new *TabConfig.kt in :shared/navigation,"
Write-Host "             an AppTab entry, and a childStack in DefaultAppComponent"
Write-Host "         NESTED: add ${Pascal}Config as a case in the pushing tab's *TabConfig.kt in"
Write-Host "             :shared/navigation, then a matching branch in that tab's child factory in"
Write-Host "             :shared/composition/AppComponent.kt that builds Default${Pascal}Component"
Write-Host "             (via koin.get() for its dependencies) and wraps it in a *TabChild case"
Write-Host "     Registration alone does not put a nested feature on screen."
Write-Host ""
Write-Host "Then: ./gradlew :feature:${Name}:impl:compileCommonMainKotlinMetadata checkArchitecture"
