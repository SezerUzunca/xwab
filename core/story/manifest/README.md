# Story manifest

One capability: **which stories exist, and where each story streams from.**

It mirrors the catalog/source split in
[core:sound:manifest](../../sound/manifest/README.md):

| Port | Answers | Read by | Declared in |
|---|---|---|---|
| `StoryCatalogRepository` | What can be listened to? | screens and playback metadata resolution | `core:story:catalog` |
| `StoryStreamCatalog` | Which HTTPS source plays this story? | `core:playback:session` | this module |

Both ports are derived from one local `storyManifest` row, so metadata and audio cannot be added
in unrelated edits. Every shipped row has a required HTTPS MP3 source; an unknown id is the only
normal reason `StoryStreamCatalog.sourceFor` returns `null`.

## Boundary

No feature declares this module. `checkArchitecture` rule 4 rejects such a dependency because a
screen should list stories through `StoryCatalogRepository`, not retrieve physical media URLs.
The composition root and playback session are the intended consumers.

This module contains no Ktor client, feed DTO, JSON parser, refresh job, retry policy, logger, or
database. Its common code owns only manifest data, validation, repository mapping, and source
mapping. There are no Android/iOS source sets because none of those jobs is platform-specific.

## Direct Story streaming

Sound and Story share the same metadata/source separation but have different delivery behavior:

```text
sound: catalog -> manifest -> delivery -> cached file or HTTPS
story: catalog -> manifest -------------> HTTPS stream
```

Story audio is handed to the platform player and is not written to the Sound cache. Therefore this
module has no cache file name, Okio file store, or `core:network` dependency. HTTPS is validated in
both `StoryEntry` and `StoryStreamSource`, the two values on the direct-player path.

## Content and licensing

The manifest currently contains five original-English works by Kate Chopin and Stephen Crane,
read by Alan Davis Drake. The recording pages explicitly release the recordings worldwide and
include an unrestricted fallback grant where public-domain dedication is not legally available.
Sources and verification notes live in
[THIRD_PARTY_AUDIO.md](../../../THIRD_PARTY_AUDIO.md).

## Deliberately absent

- **No database:** the manifest is immutable application data and Story has no offline cache.
- **No remote catalog:** there is no server contract or endpoint to refresh.
- **No progress store:** the engine does not yet expose the position/seek contract it would need.
