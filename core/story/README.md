# Story

`port/StoryPort.kt` is the module's only public port interface. It serves story metadata to features
and playback. HTTPS addresses live in an internal `StorySource` manifest and are read directly by
`StoryPlaybackResolver`. The resolver implements session's consumer-owned `PlaybackItemResolver`
port and contributes it under `STORY_PLAYBACK_KIND`. Screens receive only metadata and control
playback through the session.

Public contracts and models live in `com.xwab.app.core.story.port`. The implementation and
manifest files live separately in `com.xwab.app.core.story` and remain internal.
`checkArchitecture` enforces the public metadata port and cross-module access through `port`
packages. Session's resolver contracts are public in Kotlin but require opting in to
`@PlaybackResolverApi`, which `StoryPlaybackResolver.kt` does; its `adapterOnlyTypes` architecture
policy rejects feature usage. Screens play stories through `PlaybackPort`.

Story audio streams directly through the platform player. This module depends only on
`core:session`; it owns no network client, cache, database or platform storage implementation.
Its private source type contains only an item ID and HTTPS URL. It validates IDs and addresses,
and the resolver rejects duplicate source IDs. Tests check metadata/source completeness, missing
items and sources, stream metadata and the non-looping default. Graph tests cover the production
module graph and the contributed provider map, including resolution of every published story.

`StoryGraph` provides metadata and source manifests through `@Provides`, then binds the injected
catalog and resolver to their ports with `@Binds`. Both implementations use one constructor with
explicit dependencies. The manifests are distinct `List<Story>` and `List<StorySource>` bindings,
so no qualifier or separate binding container is needed for this graph.

The catalog and resolver are scoped to `StoryScope`. The resolver's validated source index is built
once per module graph. The two application bridges share an `AppScope`-scoped `StoryGraphHolder`,
and the resolver bridge is also scoped to `AppScope`: session's provider map constructs it on
demand and reuses it on later lookups. All graphs and implementation classes remain internal;
the application graph receives only the catalog port and session's resolver contract.
[Metro providers](https://github.com/ZacSweers/metro/blob/1.4.5/docs/provides.md),
[Metro scopes](https://github.com/ZacSweers/metro/blob/1.4.5/docs/scopes.md).

Metadata lives in [StoryManifest.kt](src/commonMain/kotlin/com/xwab/app/core/story/StoryManifest.kt).
Add the corresponding physical address in
[StorySourceManifest.kt](src/commonMain/kotlin/com/xwab/app/core/story/StorySourceManifest.kt) in the
same change. Keep `STORY_PLAYBACK_KIND` and story IDs stable because engine IDs and saved routes use
them. Adding or removing a story module changes only its own resolver contribution and consumers;
there is no shared source registry to update.
Recording sources and licensing are documented in [THIRD_PARTY_AUDIO.md](../../THIRD_PARTY_AUDIO.md).
