<#
.SYNOPSIS
    Writes a feature skeleton: an api module and an impl module.

.DESCRIPTION
    Creates feature/<name>/api with the route, its serializer registration and the callback
    contract the app shell supplies, and feature/<name>/impl with the UI, state, a Metro-contributed
    ViewModel, the entry installer and tests. Gradle discovers both modules and adds them to the
    shell automatically.

    The shell compiles against the api module only; the impl module reaches the app through Metro,
    which collects its entry installer and ViewModel from the shell's classpath. The script prints
    the remaining app-shell steps and requires choosing either a top-level destination or an
    existing feature intent that the composition root connects to it.

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

$apiDir = Join-Path $featureDir "api"
$implDir = Join-Path $featureDir "impl"
$apiNavSrc = Join-Path $apiDir "src\commonMain\kotlin\com\xwab\app\feature\$pkg\navigation"
$mainSrc = Join-Path $implDir "src\commonMain\kotlin\com\xwab\app\feature\$pkg"
$testSrc = Join-Path $implDir "src\commonTest\kotlin\com\xwab\app\feature\$pkg"
$navSrc = Join-Path $mainSrc "navigation"

Write-Host "Creating feature '$Name'..."

Write-GeneratedFile (Join-Path $apiDir "build.gradle.kts") @"
plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.${pkg}.api" }

    // If a callback contract names a core type, publish that one port with `api`, for example:
    // sourceSets {
    //     commonMain.dependencies {
    //         api(projects.core.sound.api)
    //     }
    // }
}
"@

Write-GeneratedFile (Join-Path $implDir "build.gradle.kts") @"
plugins {
    id("xwab.kmp.feature.impl")
}

kotlin {
    android { namespace = "com.xwab.app.feature.${pkg}" }

    sourceSets {
        commonMain.dependencies {
            // Declare only the public core ports this feature consumes, for example:
            // implementation(projects.core.sound.api)
            // implementation(projects.core.favorites.api)
            // implementation(projects.core.session.api)
        }
        commonTest.dependencies {
            // Declare only the fakes for the ports declared above, for example:
            // implementation(projects.testing.sound)    // FakeSoundCatalog, track(), category()
            // implementation(projects.testing.favorites) // FakeFavorites
            // implementation(projects.testing.session)  // FakePlaybackPort
        }
    }
}
"@

Write-GeneratedFile (Join-Path $apiNavSrc "${Pascal}Navigation.kt") @"
package com.xwab.app.feature.${pkg}.navigation

import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Named explicitly, because from the first release on this is a wire format: a saved back stack is
 * written with it, and restoring one throws rather than falling back when a name no longer
 * resolves. Left implicit the name follows the package, so moving this file would break the saved
 * navigation of every installed copy.
 */
@Serializable
@SerialName("com.xwab.app.feature.${pkg}.navigation.${Pascal}Route")
data object ${Pascal}Route : NavKey

@ContributesTo(NavKey::class)
@BindingContainer
object ${Pascal}NavigationBindings {
    @Provides
    @IntoSet
    fun provideRouteSerializers(): SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(${Pascal}Route::class)
        }
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
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Constructor parameters are the core ports this screen reads; the app graph supplies them. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
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

Write-GeneratedFile (Join-Path $navSrc "${Pascal}Entry.kt") @"
package com.xwab.app.feature.${pkg}.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.${pkg}.${Pascal}ScreenRoute
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.metroViewModel

@ContributesTo(EntryProviderScope::class)
@BindingContainer
object ${Pascal}EntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(): EntryProviderScope<NavKey>.() -> Unit = {
        entry<${Pascal}Route> {
            ${Pascal}ScreenRoute(viewModel = metroViewModel())
        }
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
Write-Host "  1. Add ${Pascal}Route as a top-level route or connect it to an existing intent."
Write-Host "  2. If the feature gains outgoing intents, declare its callback contract in the api module's navigation package and provide it in shared's AppEntryCallbacks."
Write-Host "  3. Pin the route's serial name in FeatureSerializersTest.routeSerialNamesAreTheSavedWireFormat: it is a saved wire format."
Write-Host ""
Write-Host "Then: ./gradlew :feature:${Name}:impl:compileCommonMainKotlinMetadata checkArchitecture"
