package com.xwab.app.core.network.di

import com.xwab.app.core.network.KtorNetworkClient
import com.xwab.app.core.network.NetworkClient
import com.xwab.app.core.network.createNetworkHttpClient
import io.ktor.client.HttpClient
import org.koin.dsl.module
import org.koin.dsl.onClose

/** One engine and connection pool for the application lifetime. */
val networkModule = module {
    single<HttpClient> { createNetworkHttpClient() } onClose { it?.close() }
    single<NetworkClient> { KtorNetworkClient(get()) }
}
