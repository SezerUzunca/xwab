# Sound

`port/SoundPort.kt` is the module's only port interface. It serves metadata and confirmed offline
availability to features and playback. This module also owns its physical addresses, stable cache
filenames and host headers in an internal source manifest. `SoundSources` indexes validated download requests by `TrackId` and
attaches the complete current sound cache inventory. `SoundPlaybackResolver` reads that internal
catalog and calls `DeliveryPort`; it implements session's consumer-owned `PlaybackItemResolver`
port and contributes it to the application graph. Screens receive sound metadata, while the
playback session asks the contributed resolver for playable content. `:core:delivery` handles
byte transfer and cache storage through its own
content-neutral port. `ManifestSoundCatalogAdapter` implements the metadata port.

`observeOfflineReady(trackId)` maps the sound ID to its current versioned cache key internally and
observes delivery without starting a download. Unknown tracks, missing sources and unavailable
cache files report false. Playback metadata uses the same track name shown in the catalog.

Public contracts and models live in `com.xwab.app.core.sound.port`. The implementation and
manifest files live separately in `com.xwab.app.core.sound` and remain internal.
`checkArchitecture` enforces the public metadata port and cross-module access through `port`
packages. This module depends on session and delivery. Session's resolver contracts are public in
Kotlin but require opting in to `@PlaybackResolverApi`, which `SoundPlaybackResolver.kt` does; its
`adapterOnlyTypes` architecture policy reserves them for adapters and rejects feature usage.
Delivery and favorites remain content-agnostic capabilities.

`SoundGraph` keeps the module's Metro wiring internal. It provides the track, category and source
manifests with `@Provides`, and binds the injected catalog and resolver to their ports with `@Binds`.
Both implementations have one constructor with explicit dependencies; the catalog has no nullable
delivery/source fallback used only by tests. `SoundSources` and `SoundPlaybackResolver` are scoped
to `SoundScope`, so catalog observation and playback use one validated source index per graph.

The two application bridges share one `AppScope`-scoped `SoundGraphHolder`. The resolver bridge is
also scoped to `AppScope`: session's provider map constructs it on demand and reuses it on later
lookups. The module graph caches the resolver; each `resolve` call still consults delivery, so a
later download can change the URI from HTTPS to a cached local file. No resolved URI is cached here.
[Metro providers](https://github.com/ZacSweers/metro/blob/1.4.5/docs/provides.md),
[Metro scopes](https://github.com/ZacSweers/metro/blob/1.4.5/docs/scopes.md).

Add track metadata in
[CatalogManifest.kt](src/commonMain/kotlin/com/xwab/app/core/sound/CatalogManifest.kt), and add its
physical source in [SoundSourceManifest.kt](src/commonMain/kotlin/com/xwab/app/core/sound/SoundSourceManifest.kt)
in the same change. Local tests validate unique IDs, valid categories, positive durations, at least
four tracks per category, matching metadata/source IDs, unique cache filenames and required host
headers. Resolver tests cover missing tracks and sources, delivery failures, playback metadata,
looping defaults and the exact download headers, media types and retained cache inventory.

`SOUND_CACHE_NAMESPACE` is declared in `port/SoundStorage.kt`; no other module reads it at
present. The cache requests themselves stay internal. Its value stays `sound`, pinned in
`wireFormat`, because it names persisted cache storage. `CacheKey` and `DeliveryRequest` validate
download addresses, filenames and headers at construction; this module validates unique source IDs and cache filenames and owns their versioning.
Playback kinds and favorites namespaces are separate contracts, even where their string values
currently match.

The catalog is local application data. This module has no HTTP client or cache implementation.
