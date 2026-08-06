# Story catalog

One capability: **what a listener can be told.** `Story`, `StoryId`, `StoryCatalogRepository`, and
the port screens will read them through.

That is the whole module — three declarations and no data. It depends on nothing else in this
build, exactly like [core:sound:catalog](../../sound/catalog/README.md), the sound half of the same
idea.

The stories themselves live next door in [core:story:manifest](../manifest/README.md), which
implements `StoryCatalogRepository` and which no feature declares. The split is the one the sound
side already has: a module that knows where a story's audio physically comes from cannot sit on
every screen's classpath, or "a screen must not resolve a story itself" is a convention rather than
a fact. `checkArchitecture` rule 4 names that module, so a feature declaring it fails the build.

| Port | Answers | Declared by |
|---|---|---|
| `StoryCatalogRepository` (here) | *what is there to listen to?* | every feature that lists stories |

## What is not here, and why

- **No HTTP client, no DTOs, no JSON.** The list is a hand-written placeholder today and a feed
  later; both are `core:story:manifest`'s business, and neither changes this module.
- **No stream URL.** Story audio is online-only and never enters the sound cache in
  `core:sound:delivery`, whose resolver starts a background download on a cache miss. Where a
  story's bytes come from belongs beside the list, not beside the model.
- **No progress or position.** Resuming a story needs `positionMs`, `durationMs` and a seek command
  that `core:playback:engine` does not publish yet. Storing progress is `core:story:progress`'s
  job once the engine can report it — a module that does not exist because nothing could fill it.
- **No Koin module, no Compose, no ViewModel.** A port module binds nothing.

`StoryId` refuses a blank value where `TrackId` does not. A track id comes from a hand-written list
one module away; a story id will come from a feed, and a blank one there is a story that lists but
never opens.

## Status

There is no `feature:story` yet. The session is ready for one: `core:playback:session` plays a
`PlaybackItemId`, already knows `PlaybackKind.STORY`, and resolves each kind through an internal
resolver of its own. A story request today fails as `ItemNotFound`, because nothing can yet say
where a story streams from — that answer arrives with the feed, in
[core:story:manifest](../manifest/README.md), and adding it is one resolver and one line of DI.
