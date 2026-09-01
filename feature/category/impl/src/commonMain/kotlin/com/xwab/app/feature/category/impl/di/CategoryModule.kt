package com.xwab.app.feature.category.impl.di

import com.xwab.app.feature.category.impl.domain.ObserveCategoryContentUseCase
import org.koin.dsl.module

/** Objects only. What this feature *shows* is wired in `:shared/composition`. */
val categoryModule = module {
    // The screen's own use case is bound here; the ports it reads come from the core modules.
    factory { ObserveCategoryContentUseCase(get(), get(), get()) }
}
