package com.xwab.app.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.SoundCatalogPort
import com.xwab.app.designsystem.state.Loadable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class BrowseViewModel(
    soundCatalogPort: SoundCatalogPort,
) : ViewModel() {
    val state: StateFlow<Loadable<BrowseState>> = soundCatalogPort.observeCategories()
        .map { categories -> Loadable.Ready(BrowseState(categories)) as Loadable<BrowseState> }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loadable.Loading,
        )
}
