<#
.SYNOPSIS
    Writes the skeleton of a new feature slice.

.DESCRIPTION
    Creates feature/<name> and feature/<name>/navigation with their build files, a route, a
    screen, a ViewModel, a state, a Koin module and the FeatureEntry the composition root
    registers. Gradle discovers the modules on its own (settings.gradle.kts scans feature/), so
    nothing else needs editing to make them build.

    Two lines in 'shared' then put the feature on screen; the script prints them when it is done.

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

$mainSrc = Join-Path $featureDir "src\commonMain\kotlin\com\xwab\app\feature\$pkg"
$testSrc = Join-Path $featureDir "src\commonTest\kotlin\com\xwab\app\feature\$pkg"
$navSrc = Join-Path $featureDir "navigation\src\commonMain\kotlin\com\xwab\app\feature\$pkg\navigation"

Write-Host "Creating feature '$Name'..."

Write-GeneratedFile (Join-Path $featureDir "build.gradle.kts") @"
plugins {
    id("xwab.kmp.feature")
}

kotlin {
    android { namespace = "com.xwab.app.feature.${pkg}" }

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

            implementation(projects.feature.${camel}.navigation)
            // Screens this one routes to go here, their navigation module only.
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
"@

Write-GeneratedFile (Join-Path $featureDir "navigation\build.gradle.kts") @"
plugins {
    id("xwab.kmp.feature.navigation")
}

kotlin {
    android { namespace = "com.xwab.app.feature.${pkg}.navigation" }
}
"@

Write-GeneratedFile (Join-Path $navSrc "${Pascal}Navigation.kt") @"
package com.xwab.app.feature.${pkg}.navigation

import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.navigation.Navigator
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

fun Navigator.navigateTo${Pascal}() {
    navigate(${Pascal}Route)
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
internal fun ${Pascal}ScreenRoute(
    onBack: () -> Unit,
    viewModel: ${Pascal}ViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ${Pascal}Screen(state = state, onBack = onBack)
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
package com.xwab.app.feature.${pkg}.di

import com.xwab.app.feature.${pkg}.${Pascal}ViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Objects only. What this feature shows is in ${Pascal}Entry.kt. */
internal val ${camel}Module = module {
    // This screen's own use cases are bound here too, never in a shared core module.
    viewModel { ${Pascal}ViewModel() }
}
"@

Write-GeneratedFile (Join-Path $mainSrc "${Pascal}Entry.kt") @"
@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.${pkg}

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.feature.${pkg}.navigation.${Pascal}Route
import org.koin.compose.viewmodel.koinViewModel

/**
 * Where this feature's routes turn into screens.
 *
 * Route another feature by declaring its navigation module in this one's build file and calling
 * the navigateToX extension it publishes — never by depending on its implementation.
 */
internal fun EntryProviderScope<NavKey>.${camel}Entry(navigator: Navigator) {
    entry<${Pascal}Route> {
        ${Pascal}ScreenRoute(
            onBack = navigator::goBack,
            viewModel = koinViewModel(),
        )
    }
}
"@

Write-GeneratedFile (Join-Path $mainSrc "${Pascal}Feature.kt") @"
package com.xwab.app.feature.${pkg}

import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.feature.${pkg}.di.${camel}Module
import com.xwab.app.feature.${pkg}.navigation.${camel}NavigationSerializers

/** The whole of this feature, as the composition root sees it. */
val ${camel}Feature = FeatureEntry(
    koinModule = ${camel}Module,
    entries = { ${camel}Entry(it) },
    serializers = ${camel}NavigationSerializers,
)
"@

Write-GeneratedFile (Join-Path $testSrc "${Pascal}ViewModelTest.kt") @"
package com.xwab.app.feature.${pkg}

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The port fakes from core:testing are on this module's test classpath — the generated build file
 * declares it. Add the capability modules this screen reads beside it in commonMain.
 */
class ${Pascal}ViewModelTest {
    @Test
    fun theScreenStartsFromItsInitialState() {
        assertEquals(${Pascal}State(), ${Pascal}ViewModel().state.value)
    }
}
"@

Write-Host ""
Write-Host "Done. Two lines left, both in shared:" -ForegroundColor Green
Write-Host "  1. shared/build.gradle.kts, commonMain.dependencies:"
Write-Host "         implementation(projects.feature.${camel})"
Write-Host "  2. shared/src/commonMain/kotlin/com/xwab/app/di/AppFeatures.kt:"
Write-Host "         add ${camel}Feature to the 'features' list (and import it)"
Write-Host ""
Write-Host "Then: ./gradlew :feature:${Name}:compileCommonMainKotlinMetadata checkArchitecture"
