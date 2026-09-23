# Sound

`port/SoundPort.kt` is the module's only port interface. It serves metadata to features and
playback. This module also owns its physical addresses, stable cache filenames and host headers in
an internal source manifest. `SoundSources` indexes validated download requests by `TrackId` and
attaches the complete current sound cache inventory. `SoundPlaybackResolver` reads that internal
catalog and calls `DeliveryPort`; it implements session's consumer-owned `PlaybackItemResolver`
port and contributes it to the application graph. Screens receive sound metadata, while the
playback session asks the contributed resolver for playable content. `:core:delivery` handles
byte transfer and cache storage through its own
content-neutral port. `ManifestSoundCatalogAdapter` implements the metadata port.

Public contracts and models live in `com.xwab.app.core.sound.port`. The implementation and
manifest files live separately in `com.xwab.app.core.sound` and remain internal.
`checkArchitecture` enforces the public metadata port and cross-module access through `port`
packages. This module depends on session and delivery. Session's resolver contracts are public in
Kotlin but require opting in to `@PlaybackResolverApi`, which `SoundPlaybackResolver.kt` does; its
`adapterOnlyTypes` architecture policy reserves them for adapters and rejects feature usage.
Delivery and favorites remain content-agnostic capabilities.

Add track metadata in
[CatalogManifest.kt](src/commonMain/kotlin/com/xwab/app/core/sound/CatalogManifest.kt), and add its
physical source in [SoundSourceManifest.kt](src/commonMain/kotlin/com/xwab/app/core/sound/SoundSourceManifest.kt)
in the same change. Local tests validate unique IDs, valid categories, positive durations, at least
four tracks per category, matching metadata/source IDs, unique cache filenames and required host
headers. Resolver tests cover missing tracks and sources, delivery failures, playback metadata,
looping defaults and the exact download headers, media types and retained cache inventory.

`SOUND_CACHE_NAMESPACE` is public in `port/SoundStorage.kt` for one reader, the composition root:
only the module that assembles the app knows which cache namespaces are still installed, and it
names them so that downloads a removed content type left behind can be swept at launch. The cache
requests themselves stay internal. Its value remains `sound`, pinned in `wireFormat`, because it
names persisted cache storage. `CacheKey` and `DeliveryRequest` validate download addresses, filenames and headers at
construction; this module validates unique source IDs and cache filenames and owns their versioning.
Playback kinds and favorites namespaces are separate contracts, even where their string values
currently match.

The catalog is local application data. This module has no HTTP client or cache implementation.
