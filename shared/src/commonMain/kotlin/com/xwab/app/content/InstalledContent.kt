package com.xwab.app.content

import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.sound.port.SOUND_CACHE_NAMESPACE
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The cache namespaces this build installs, and the only place that can know them.
 *
 * Capabilities install themselves — `shared/build.gradle.kts` discovers every `:core:` module and
 * Metro aggregates what they contribute — so no list is needed to *add* a content type. Removing
 * one is different: what it left on the device outlives the module, and nothing that is gone can
 * clean up after itself.
 *
 * Stated rather than derived, and deliberately so. Deleting a listener's downloads is not a
 * decision a discovery loop should make quietly: delete `:core:sound` and this line stops
 * compiling, which is the moment to decide what happens to what is already on their phone.
 *
 * A content type that caches nothing is simply absent here. Stories stream.
 */
internal val INSTALLED_CACHE_NAMESPACES: Set<String> = setOf(SOUND_CACHE_NAMESPACE)

/**
 * Removes downloads belonging to content this build no longer has.
 *
 * Delivery sweeps a namespace from inside a request for that namespace, which is exactly what a
 * removed content type stops producing — so its directory is never listed again and its audio sits
 * there, tens of megabytes of a capability the app does not offer any more. One listing at startup
 * settles it.
 *
 * Failures are the store's business and it logs them: a cache that will not be swept is not worth
 * failing a launch over, and the next start tries again.
 *
 * Public for the same reason a feature's dependency bag is. `AppGraph` is what the platform entry
 * points hold, so anything it hands out is part of this module's surface. The namespace list it
 * reads stays internal — that is policy, not contract.
 */
@SingleIn(AppScope::class)
@Inject
class ContentCacheMaintenance(private val delivery: DeliveryPort) {
    suspend fun sweepUninstalledContent() {
        delivery.retainOnly(INSTALLED_CACHE_NAMESPACES)
    }
}
