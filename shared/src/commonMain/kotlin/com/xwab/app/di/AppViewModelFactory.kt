package com.xwab.app.di

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

/**
 * Every feature's contributed ViewModel providers, read by `metroViewModel` from composition.
 *
 * A plain ViewModel is built from its provider alone, without its entry's creation extras, so it
 * cannot take a `SavedStateHandle`. One that needs its entry's saved state is assisted instead:
 * `assistedMetroViewModel`'s lambda receives the entry's extras, and `createSavedStateHandle()` on
 * them gives a handle that is saved and restored with that entry.
 */
@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class AppViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders:
        Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory()
