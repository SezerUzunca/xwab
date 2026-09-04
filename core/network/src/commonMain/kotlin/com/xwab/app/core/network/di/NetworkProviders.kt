package com.xwab.app.core.network.di

import com.xwab.app.core.network.KtorNetworkClient
import com.xwab.app.core.network.NetworkClient
import com.xwab.app.core.network.createNetworkHttpClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

/**
 * One engine and connection pool for the application lifetime.
 *
 * Contributed rather than listed: the app graph declares `AppScope` and Metro merges every
 * `@ContributesTo(AppScope::class)` in the build into it, so adding a capability no longer means
 * editing a module list at the root.
 *
 * The client is not closed on teardown the way the old Koin binding was. Nothing outlives the
 * process here, and a compile-time graph has no container to close.
 */
@ContributesTo(AppScope::class)
interface NetworkProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(): HttpClient = createNetworkHttpClient()

    @Provides
    @SingleIn(AppScope::class)
    fun provideNetworkClient(httpClient: HttpClient): NetworkClient = KtorNetworkClient(httpClient)
}
