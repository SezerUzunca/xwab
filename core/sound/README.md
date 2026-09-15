# Sound

`port/SoundPort.kt` is the module's only port interface. It serves metadata to features and
playback. HTTPS sources and stable cache filenames live in `:core:sources`, where screens cannot
reach them. `:core:delivery` never reads this module — rule 13 forbids a
reusable capability any dependency on app content — so the code that moves the bytes has never
heard of a track. `ManifestSoundCatalogAdapter.kt` implements metadata operations from the local
manifest and is the module's single internal Metro binding.

Public contracts and models live in `com.xwab.app.core.sound.port`. The implementation and
manifest files live separately in `com.xwab.app.core.sound` and remain internal.
`checkArchitecture` enforces the single port. Delivery and favorites are content-agnostic modules
of their own, outside this one, and are named in rule 13 to keep them that way.

Add track metadata in
[CatalogManifest.kt](src/commonMain/kotlin/com/xwab/app/core/sound/CatalogManifest.kt), and add its
physical source in `:core:sources` in the same change. Tests validate unique IDs, valid categories,
positive durations and at least four tracks per category. A consistency test in `:core:session`,
the module that pairs a track with its address at runtime, ensures every published track has a
source before it can ship.

The catalog is local application data. This module has no HTTP client or cache implementation.
