package com.xwab.app.feature.browse

import com.xwab.app.core.sound.port.Category

/** Loading and content states owned by this feature. */
internal sealed interface BrowseUiState {
    data object Loading : BrowseUiState

    data class Ready(val value: BrowseState) : BrowseUiState
}

/** Content available in [BrowseUiState.Ready]. */
internal data class BrowseState(
    val categories: List<Category> = emptyList(),
)
