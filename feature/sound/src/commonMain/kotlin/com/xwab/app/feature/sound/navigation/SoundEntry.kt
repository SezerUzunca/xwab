package com.xwab.app.feature.sound.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.sound.SoundDetailScreenRoute
import com.xwab.app.feature.sound.SoundViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.soundEntry(
    onBack: () -> Unit,
) {
    entry<SoundRoute> { route ->
        SoundDetailScreenRoute(
            onBack = onBack,
            // A route is a serialized wire format, so it carries the plain id and the wrapper goes
            // back on here — the one place this feature handles a bare track string.
            viewModel = assistedMetroViewModel<SoundViewModel, SoundViewModel.Factory> {
                create(TrackId(route.trackId))
            },
        )
    }
}
