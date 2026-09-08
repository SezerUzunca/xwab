<#
.SYNOPSIS
    Writes a single-module feature skeleton.

.DESCRIPTION
    Creates feature/<name> with navigation, UI, state, ViewModel, Metro dependencies and tests in
    one Gradle module. Gradle discovers the module automatically.

    The feature is intentionally not self-registering. The script prints the explicit app-shell
    steps and requires choosing either a top-level destination or an existing feature intent that
    the composition root connects to it.

.PARAMETER Name
    Lower-case, dash-separated directory name, for example 'favorites' or 'sleep-timer'.

.EXAMPLE
    ./tools/new-feature.ps1 sleep-timer
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
    throw "feature/$Name already exists. Pick another name or remove the existing feature first."
}

$parts = $Name.Split('-')
$Pascal = ($parts | ForEach-Object { $_.Substring(0, 1).ToUpper() + $_.Substring(1) }) -join ''
$camel = $Pascal.Substring(0, 1).ToLower() + $Pascal.Substring(1)
$pkg = $parts -join ''

function Write-GeneratedFile {
    param([string]$Path, [string]$Content)

    $dir = Split-Path -Parent $Path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    if (-not $Content.EndsWith("`n")) { $Content += "`n" }

    [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "  created $($Path.Substring($repoRoot.Length + 1))"
}

$mainSrc = Join-Path $featureDir "src\commonMain\kotlin\com\xwab\app\feature\$pkg"
$testSrc = Join-Path $featureDir "src\commonTest\kotlin\com\xwab\app\feature\$pkg"
$navSrc = Join-Path $mainSrc "navigation"

Write-Host "Creating feature '$Name'..."

Write-GeneratedFile (Join-Path $featureDir "build.gradle.kts") @"
plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.${pkg}" }

    sourceSets {
        commonMain.dependencies {
            // Declare only the public core ports this feature consumes, for example:
            // implementation(projects.core.sound.catalog)
            // implementation(projects.core.sound.favorites)
            // implementation(projects.core.session)
        }
        commonTest.dependencies {
            implementation(projects.testing)
        }
    }
}
"@

Write-GeneratedFile (Join-Path $navSrc "${Pascal}Navigation.kt") @"
package com.xwab.app.feature.${pkg}.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data object ${Pascal}Route : NavKey

val ${camel}NavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(${Pascal}Route.serializer())
    }
}
"@

Write-GeneratedFile (Join-Path $mainSrc "${Pascal}State.kt") @"
package com.xwab.app.feature.${pkg}

internal data class ${Pascal}State(
    val title: String = "${Pascal}",
)
"@

Write-GeneratedFile (Join-Path $mainSrc "${Pascal}ViewModel.kt") @"
package com.xwab.app.feature.${pkg}

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ${Pascal}ViewModel : ViewModel() {
    val state: StateFlow<${Pascal}State> = MutableStateFlow(${Pascal}State()).asStateFlow()
}
"@

Write-GeneratedFile (Join-Path $mainSrc "${Pascal}Screen.kt") @"
package com.xwab.app.feature.${pkg}

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun ${Pascal}ScreenRoute(viewModel: ${Pascal}ViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ${Pascal}Screen(state = state)
}

@Composable
private fun ${Pascal}Screen(
    state: ${Pascal}State,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = state.title)
    }
}
"@

Write-GeneratedFile (Join-Path $mainSrc "di\${Pascal}Dependencies.kt") @"
package com.xwab.app.feature.${pkg}.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Public graph entry; the ports held by a real feature remain internal properties. */
@SingleIn(AppScope::class)
@Inject
class ${Pascal}Dependencies
"@

Write-GeneratedFile (Join-Path $navSrc "${Pascal}Entry.kt") @"
package com.xwab.app.feature.${pkg}.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.${pkg}.${Pascal}ScreenRoute
import com.xwab.app.feature.${pkg}.${Pascal}ViewModel
import com.xwab.app.feature.${pkg}.di.${Pascal}Dependencies

/** Converts this feature's route into its internal UI. */
fun EntryProviderScope<NavKey>.${camel}Entry(dependencies: ${Pascal}Dependencies) {
    entry<${Pascal}Route> {
        ${Pascal}ScreenRoute(viewModel = viewModel { ${Pascal}ViewModel() })
    }
}
"@

Write-GeneratedFile (Join-Path $testSrc "${Pascal}ViewModelTest.kt") @"
package com.xwab.app.feature.${pkg}

import kotlin.test.Test
import kotlin.test.assertEquals

class ${Pascal}ViewModelTest {
    @Test
    fun theScreenStartsFromItsInitialState() {
        assertEquals(${Pascal}State(), ${Pascal}ViewModel().state.value)
    }
}
"@

Write-Host ""
Write-Host "Done. Wire the feature in the app shell:" -ForegroundColor Green
Write-Host "  1. Add implementation(projects.feature.${camel}) to shared/build.gradle.kts."
Write-Host "  2. Expose ${camel}Dependencies from shared/.../di/AppGraph.kt."
Write-Host "  3. Register ${camel}Entry and ${camel}NavigationSerializers in AppNavigation.kt."
Write-Host "  4. Add ${Pascal}Route as a top-level route or connect it to an existing intent."
Write-Host ""
Write-Host "Then: ./gradlew :feature:${Name}:compileCommonMainKotlinMetadata checkArchitecture"
