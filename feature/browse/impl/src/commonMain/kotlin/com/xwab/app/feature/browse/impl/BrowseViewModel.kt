package com.xwab.app.feature.browse.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.ui.state.Loadable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class BrowseViewModel(
    musicCatalog: MusicCatalogRepository,
) : ViewModel() {
    val state: StateFlow<Loadable<BrowseState>> = musicCatalog.observeCategories()
        .map { categories -> Loadable.Ready(BrowseState(categories)) as Loadable<BrowseState> }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loadable.Loading,
        )
}
