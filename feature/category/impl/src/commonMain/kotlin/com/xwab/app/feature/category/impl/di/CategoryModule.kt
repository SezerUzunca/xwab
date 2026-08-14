package com.xwab.app.feature.category.impl.di

import com.xwab.app.feature.category.impl.CategoryViewModel
import com.xwab.app.feature.category.impl.domain.ObserveCategoryContentUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Objects only. What this feature *shows* is in `CategoryEntry.kt`. */
val categoryModule = module {
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
