# Story catalog

One capability: **what a listener may know about a story.** This module declares `Story`,
`StoryId`, and `StoryCatalogRepository`; it contains no catalog rows or physical sources.

The data lives in [core:story:manifest](../manifest/README.md), which implements the repository.
That mirrors [core:sound:catalog](../../sound/catalog/README.md): screens receive descriptive
metadata through a read port, while the module that knows the audio address stays off their
classpath.

| Port | Answers | Read by |
|---|---|---|
| `StoryCatalogRepository` | Which stories exist, and their title, author, narrator, description, and duration | `feature:story` and playback metadata resolution |

## What is not here

- **No HTTP client, DTO, or JSON.** The shipped catalog is a local manifest.
- **No stream URL.** Physical sources belong to `core:story:manifest` and are read only by the
  playback session.
- **No download or cache API.** Story playback is currently online-only. Sound caching remains in
  `core:sound:delivery` and is not reused as a Story responsibility.
- **No playback progress.** That needs position and seek support from the playback engine before a
  progress store has useful data to persist.
- **No DI or UI.** This module declares types and a port; composition and presentation live
  elsewhere.

## Invariants

`StoryId` rejects blank ids. `Story` requires a title, author, description, positive duration, a
non-blank narrator when present, and HTTPS artwork when present. Manifest-level tests add unique
ids and unique playable sources.

## Status

The catalog contains five real English-language literary stories, and `feature:story` lists and
plays every shipped row through `PlaybackCoordinator`. There is still no progress bar or resume:
`core:playback:engine` publishes no position and accepts no seek, so a story is played and paused
the way a sound is. Recording sources and their public-domain grants are listed in
[THIRD_PARTY_AUDIO.md](../../../THIRD_PARTY_AUDIO.md).
