package com.xwab.app.feature.sound.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.sound.SoundDetailScreenRoute
import com.xwab.app.feature.sound.SoundViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

@ContributesTo(EntryProviderScope::class)
@BindingContainer
object SoundEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(
        callbacks: SoundEntryCallbacks,
    ): EntryProviderScope<NavKey>.() -> Unit = {
        entry<SoundRoute> { route ->
            SoundDetailScreenRoute(
                onBack = callbacks.onBack,
                // A route is a serialized wire format, so it carries the plain id and the wrapper goes
                // back on here — the one place this feature handles a bare track string.
                viewModel = assistedMetroViewModel<SoundViewModel, SoundViewModel.Factory> {
                    create(TrackId(route.trackId))
                },
            )
        }
    }
}
