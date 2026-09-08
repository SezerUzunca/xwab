# Sound

`port/SoundPort.kt` is the module's only port interface. It serves metadata to features and
playback, and supplies HTTPS sources and stable cache filenames to `:core:delivery`.
`SoundPortImpl.kt` implements all of these operations from the same manifest entries and is
the module's single internal Metro binding.

Public contracts and models live in `com.xwab.app.core.sound.port`. The implementation and
manifest files live separately in `com.xwab.app.core.sound` and remain internal.
`checkArchitecture` enforces the single port. Delivery and favorites remain separate modules.

Add tracks in [CatalogManifest.kt](src/commonMain/kotlin/com/xwab/app/core/sound/CatalogManifest.kt).
Cache names use `<track id>-v<version>.mp3`. Raising the version retires the old cache name;
delivery sweeps retired files after a completed download. Tests validate unique IDs, sources and
cache names, valid categories and at least four tracks per category.

The catalog is local application data. This module has no HTTP client or cache implementation.
