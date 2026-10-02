package com.xwab.app.di

import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test

class AndroidAppGraphTest {
    @Test
    fun everyScreensViewModelResolvesFromTheRealGraph() =
        assertEveryViewModelResolves(createAppGraph(ApplicationProvider.getApplicationContext()))
}
