# Sound

`port/SoundPort.kt` is the module's only port interface. It serves metadata to features and
playback, and supplies HTTPS sources and stable cache filenames to `:core:session`, which turns
them into a `DeliveryRequest`. `:core:delivery` never reads this module — rule 13 forbids a
reusable capability any dependency on app content — so the code that moves the bytes has never
heard of a track. `SoundPortImpl.kt` implements all of these operations from the same manifest
entries and is the module's single internal Metro binding.

Public contracts and models live in `com.xwab.app.core.sound.port`. The implementation and
manifest files live separately in `com.xwab.app.core.sound` and remain internal.
`checkArchitecture` enforces the single port. Delivery and favorites are content-agnostic modules
of their own, outside this one, and are named in rule 13 to keep them that way.

Add tracks in [CatalogManifest.kt](src/commonMain/kotlin/com/xwab/app/core/sound/CatalogManifest.kt).
Cache names use `<track id>-v<version>.mp3`. Raising the version retires the old cache name: the
session sends the current inventory with every request, and delivery sweeps whatever is no longer
in it after a completed download. Tests validate unique IDs, sources and
cache names, valid categories and at least four tracks per category.

The catalog is local application data. This module has no HTTP client or cache implementation.
