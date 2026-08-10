package com.xwab.app.feature.category.di

import com.xwab.app.feature.category.CategoryViewModel
import com.xwab.app.feature.category.domain.ObserveCategoryContentUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Objects only. What this feature *shows* is in `CategoryEntry.kt`. */
internal val categoryModule = module {
    // The screen's own use case is bound here; the ports it reads come from the core modules.
    factory { ObserveCategoryContentUseCase(get(), get(), get()) }

    viewModel { parameters ->
        CategoryViewModel(
            categoryId = parameters.get(),
            observeCategoryContentUseCase = get(),
            favoritesRepository = get(),
            playbackCoordinator = get(),
        )
    }
}
