# Story

`port/StoryPort.kt` is the module's only port interface. It serves metadata to features and
playback, and supplies HTTPS streams to `:core:session`. `StoryPortImpl.kt` implements these
operations from the same local manifest rows and is the module's single internal Metro binding.

Public contracts and models live in `com.xwab.app.core.story.port`. The implementation and
manifest files live separately in `com.xwab.app.core.story` and remain internal.
`checkArchitecture` enforces the single port. Screens play stories through `PlaybackPort`.

Story audio streams directly through the platform player, without the sound cache. This module
has no network client, database or platform storage implementation. Entries and source values
validate HTTPS MP3 sources; an unknown ID returns no source.

Content lives in [StoryManifest.kt](src/commonMain/kotlin/com/xwab/app/core/story/StoryManifest.kt).
Recording sources and licensing are documented in [THIRD_PARTY_AUDIO.md](../../THIRD_PARTY_AUDIO.md).
