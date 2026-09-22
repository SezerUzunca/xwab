# Story

`port/StoryPort.kt` is the module's only public port interface. It serves story metadata to features
and playback. HTTPS addresses live in an internal `StorySource` manifest and are read directly by
`StoryPlaybackResolver`. The resolver implements session's consumer-owned `PlaybackItemResolver`
port and contributes it under `STORY_PLAYBACK_KIND`. Screens receive only metadata and control
playback through the session.

Public contracts and models live in `com.xwab.app.core.story.port`. The implementation and
manifest files live separately in `com.xwab.app.core.story` and remain internal.
`checkArchitecture` enforces the public metadata port and cross-module access through `port`
packages. Session's resolver contracts are public at Kotlin compile time; its `adapterOnlyTypes`
architecture policy rejects feature usage. Screens play stories through `PlaybackPort`.

Story audio streams directly through the platform player. This module depends only on
`core:session`; it owns no network client, cache, database or platform storage implementation.
Its private source type contains only an item ID and HTTPS URL. It validates IDs and addresses,
and the resolver rejects duplicate source IDs. Tests check metadata/source completeness, missing
items and sources, stream metadata and the non-looping default. A module-local Metro graph verifies
the installed resolver can resolve every published story.

Metadata lives in [StoryManifest.kt](src/commonMain/kotlin/com/xwab/app/core/story/StoryManifest.kt).
Add the corresponding physical address in
[StorySourceManifest.kt](src/commonMain/kotlin/com/xwab/app/core/story/StorySourceManifest.kt) in the
same change. Keep `STORY_PLAYBACK_KIND` and story IDs stable because engine IDs and saved routes use
them. Adding or removing a story module changes only its own resolver contribution and consumers;
there is no shared source registry to update.
Recording sources and licensing are documented in [THIRD_PARTY_AUDIO.md](../../THIRD_PARTY_AUDIO.md).
