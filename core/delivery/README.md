# Delivery

`:core:delivery` resolves HTTPS content to a local cache path or its remote URL. It depends only
on `:core:network`; it knows no sound/story IDs, manifests or playback engines. Its public types
live in `com.xwab.app.core.delivery.port`, and all platform and cache implementations are internal.

```kotlin
val result = deliveryPort.resolve(
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
Change the cache filename when the bytes behind an item change.

Request headers are caller-owned too, because this module knows nothing about the host it fetches
from — and some hosts, Wikimedia among them, refuse a request that does not identify its client.
Such a refusal arrives as a 4xx, which is treated as a permanently unusable source, so the effect
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

Android uses `cacheDir/content/<namespace>`; iOS uses `Library/Caches/content/<namespace>`. The
previous flat sound cache at `audio-content/` is no longer consulted: it sits outside every
namespace directory, so no sweep would ever reach it again. The platform adapters name it as a
legacy root and the store deletes it once, on the first download after an upgrade; those tracks are
then fetched again on demand in the `sound` namespace. Favorites storage is unaffected.

The composition root includes delivery and network so Metro can supply their platform bindings.
Core modules consume `DeliveryPort`; the existing architecture rule still routes feature playback
through `PlaybackPort`. Tests cover different content types, namespace isolation, inventory
cleanup, retry/cancellation, unsafe keys and configurable size limits.
