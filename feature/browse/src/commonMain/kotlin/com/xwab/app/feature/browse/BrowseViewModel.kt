package com.xwab.app.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.SoundPort
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class BrowseViewModel(
    soundPort: SoundPort,
) : ViewModel() {
    val state: StateFlow<BrowseUiState> = soundPort.observeCategories()
        .map { categories -> BrowseUiState.Ready(BrowseState(categories)) as BrowseUiState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BrowseUiState.Loading,
        )
}
