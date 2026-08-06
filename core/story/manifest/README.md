# Story manifest

One capability: **which stories exist, and where each one's audio comes from.**

Today only the first half is answered, and by a hand-written placeholder list —
`storyManifest` in [StoryManifest.kt](src/commonMain/kotlin/com/xwab/app/core/storymanifest/StoryManifest.kt).
The entries are not recorded audio; they exist so the `StoryCatalogRepository` port in
[core:story:catalog](../catalog/README.md) has something to serve and the wiring around it can be
built and tested before a story feed exists.

`ManifestStoryCatalogRepository` implements that port. When a feed arrives, it is this module's
internals that change — a client, DTOs, a mapping — and the port, the DI entry point and every
caller stay as they are.

## No feature declares this module

`checkArchitecture` rule 4 fails the build on one that does, for the reason it names
`core:sound:manifest` on the sound side: this is where the physical source behind a story will
live, and a port that hands out a URL cannot sit on every screen's classpath. A screen lists
stories through `StoryCatalogRepository`; what is behind them is not its business.

## Story audio never enters the sound cache

`core:sound:delivery` resolves a `TrackId` to a URI and, on a cache miss, starts a background
download of the file. Stories are online-only: they stream and are not kept. So a story's source is
resolved here — not there — and the resolution port for it lands in this module beside the list,
once the feed contract settles the questions it depends on:

- the endpoint and the JSON shape
- whether stream URLs are permanent or signed, and how long a signed one lives
- whether story metadata may be shown offline from a cached snapshot
- timeout, retry and the error model

Until those are answered, a stream URL here would be a guess with tests written against it.

## What is deliberately absent

- **No HTTP client and no serialization dependency.** Adding either commits the build to a feed
  shape that does not exist yet.
- **No progress storage.** Resuming needs position, duration and seek from `core:playback:engine`,
  none of which it publishes; `core:story:progress` follows that, not this.
- **No playback wiring.** The session is already generic — it plays a `PlaybackItemId` and knows
  `PlaybackKind.STORY` — so what is missing is only the answer this module will hold: where a story
  streams from. When the feed lands, a `StoryPlaybackResolver` reads it and joins the resolver list
  in `core:playback:session`. No screen changes for it.
