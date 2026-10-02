package com.xwab.app.di

import kotlin.test.Test

class IosAppGraphTest {
    @Test
    fun everyScreensViewModelResolvesFromTheRealGraph() = assertEveryViewModelResolves(createAppGraph())
}
