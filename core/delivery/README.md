# Delivery

`:core:delivery` resolves HTTPS content to a local cache path or its remote URL. It depends only
on `:core:network`; it knows no sound/story IDs, manifests or playback engines. Its public types
live in `com.xwab.app.core.delivery.port`, and all platform and cache implementations are internal.

```kotlin
val uri = deliveryPort.resolve(
    DeliveryRequest(
        key = CacheKey(namespace = "documents", fileName = "guide-v1.pdf"),
        httpsUrl = "https://example.com/guide.pdf",
        acceptedContentTypes = setOf("application/pdf"),
        maxBytes = 10L * 1024 * 1024,
        headers = mapOf("User-Agent" to "MyApp/1.0"),
    ),
)
```

A cache hit returns an absolute local path. A miss returns HTTPS immediately and starts a
background download; it does not wait for a local file. Supported content types and the download
size ceiling are caller-owned. The defaults accept any content type and limit downloads to 25 MiB.
Change the cache filename when the bytes behind an item change. Cache access or prefetch startup
failures are logged and fall back to HTTPS; cancellation still propagates.

`observeCached(key)` checks the local file without resolving or downloading content. It rechecks
on collection and after cache completion or cleanup. Only nonempty final files count as cached;
staged downloads, missing files and filesystem read failures report false. Callers can use this
status to show confirmed offline availability instead of assuming a streamed item is downloaded.

Request headers are caller-owned too, because this module knows nothing about the host it fetches
from — and some hosts, Wikimedia among them, refuse a request that does not identify its client.
Such a refusal arrives as a 4xx, which is treated as a permanently unusable source, as is a 3xx:
the network client follows every other redirect, so one that arrives here is a redirect it refused,
such as HTTPS to cleartext. The effect
of omitting a required header is a cache that never fills while playback keeps streaming. `Accept`
is the exception: it is derived from `acceptedContentTypes`, and a request that also states it is
rejected rather than silently preferring one of the two.

Each namespace has its own directory, in-flight transfer keys and retry cooldowns. A caller may
supply `retainedFileNames` containing the complete inventory for that namespace, including the
requested file. After a successful download, completed files outside that inventory are removed
only within that namespace. Omit it to disable inventory cleanup. Namespace owners must keep
requests consistent with their current inventory.

Downloads use hidden staged files, response/body validation, flush and atomic promotion.
Cancellation and failure remove partial files. Retryable failures receive three attempts, followed
by a five-minute cooldown. Cache and network details remain hidden behind `DeliveryPort`.

Android uses `cacheDir/content/<namespace>`; iOS uses `Library/Caches/content/<namespace>`.

Each platform has its own graph (`AndroidDeliveryGraph`, `IosDeliveryGraph`). Metro builds the cache
location in it, on Android from the cache directory the adapter passes in and on iOS from the
system's file manager. A shared binding container wires the store, the prefetcher and the adapter,
and only `DeliveryPort` reaches the application graph. Features cannot depend on delivery; they play
content through `PlaybackPort`. Tests cover different content types, namespace isolation, inventory
cleanup, retry/cancellation, unsafe keys and configurable size limits. Both platform graphs run
one shared check that a prefetch reaches an open observer through the scoped store.
