package com.xwab.app.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration = {}): KoinApplication = startKoin {
    config()
    modules(appModules())
}
