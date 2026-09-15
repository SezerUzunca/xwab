# Story

`port/StoryPort.kt` is the module's only port interface. It serves metadata to features and
playback. HTTPS stream addresses live in `:core:sources`, where screens cannot reach them.
`ManifestStoryCatalogAdapter.kt` implements metadata operations from the local manifest and is the
module's single internal Metro binding.

Public contracts and models live in `com.xwab.app.core.story.port`. The implementation and
manifest files live separately in `com.xwab.app.core.story` and remain internal.
`checkArchitecture` enforces the single port. Screens play stories through `PlaybackPort`.

Story audio streams directly through the platform player, without the sound cache. This module
has no network client, source address, database or platform storage implementation. A consistency
test in `:core:session`, the module that pairs a story with its address at runtime, ensures every
published story has a source before it can ship.

Metadata lives in [StoryManifest.kt](src/commonMain/kotlin/com/xwab/app/core/story/StoryManifest.kt).
Add the corresponding physical address in `:core:sources` in the same change.
Recording sources and licensing are documented in [THIRD_PARTY_AUDIO.md](../../THIRD_PARTY_AUDIO.md).
