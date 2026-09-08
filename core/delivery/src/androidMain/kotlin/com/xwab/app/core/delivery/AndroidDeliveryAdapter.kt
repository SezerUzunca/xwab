package com.xwab.app.core.delivery

import android.content.Context
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.delivery.port.DeliveryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidDeliveryAdapter(
    context: Context,
    networkPort: NetworkPort,
) : DeliveryPort by createDeliveryAdapter(
    root = context.cacheDir.resolve("content").absolutePath.toPath(),
    networkPort = networkPort,
    // Where sound-only delivery cached its tracks, before any of this was namespaced.
    legacyRoots = listOf(context.cacheDir.resolve("audio-content").absolutePath.toPath()),
)
