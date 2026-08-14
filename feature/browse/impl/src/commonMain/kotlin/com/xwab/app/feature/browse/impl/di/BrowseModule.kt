package com.xwab.app.feature.browse.impl.di

import com.xwab.app.feature.browse.impl.BrowseViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val browseModule = module {
    viewModel { BrowseViewModel(musicCatalog = get()) }
}
