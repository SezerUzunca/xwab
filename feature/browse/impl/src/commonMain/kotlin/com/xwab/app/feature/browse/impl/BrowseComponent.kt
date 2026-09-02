package com.xwab.app.feature.browse.impl

import com.arkivanov.decompose.ComponentContext
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.navigation.componentScope
import com.xwab.app.core.ui.state.Loadable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

interface BrowseComponent {
    val state: StateFlow<Loadable<BrowseState>>
    val onCategoryClick: (CategoryId) -> Unit
}

class DefaultBrowseComponent(
    componentContext: ComponentContext,
    musicCatalog: MusicCatalogRepository,
    override val onCategoryClick: (CategoryId) -> Unit,
) : BrowseComponent, ComponentContext by componentContext {

    override val state: StateFlow<Loadable<BrowseState>> = musicCatalog.observeCategories()
        .map { categories -> Loadable.Ready(BrowseState(categories)) as Loadable<BrowseState> }
        .stateIn(
            scope = componentScope(),
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loadable.Loading,
        )
}
