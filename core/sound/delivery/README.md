# Sound delivery

One capability: **getting a track's bytes to the player.** Which URI plays now, what is worth
fetching for later, and the app-owned copies in between.

This module owns:

- `AudioContentResolver`, the port `core:playback:session` asks for a playable URI;
- `AudioFileStore` and the whole of the caching flow behind it — staging, promotion, the sweep;
- the `androidMain` / `iosMain` adapters, the only code here that touches a file system or a socket.

It does **not** own the catalog. Which tracks exist and where their bytes come from is
`core:sound:manifest`'s answer, read through `AudioSourceCatalog`. Nothing here holds a copy of
the manifest.

## Nobody but playback declares this module

`core:sound:delivery` appears in exactly two build files: `core:playback:session`, which assembles a
session out of it and the engine, and `shared`, which binds it into the container. No feature
declares it, so no screen can name `AudioContentResolver` or `AudioFileStore`.

That is held by `checkArchitecture` rule 4, which fails the build if a feature adds the dependency —
a graph edge, not a promise. The rule it replaced grepped feature sources for those two class names,
because the catalog and delivery shared a module and the graph could not tell a legitimate
dependency from a reach through it. An edge survives a rename; a quoted class name did not, so the
rule now also fails if the module paths it names stop matching real modules.

`AudioFileStore` stays public for one reason: the composition root substitutes it in a DI test. The
resolver's implementation, the prefetcher, the cache directory and the transport are all internal.

## Packages

Three, and the dependencies between them run one way only:

```
cache  ←  resolution
  ↑
platform            di → all three
```

- `cache` — `AudioFileStore` and the flow behind it: staging, promotion, the sweep, and the rules
  for reading an HTTP answer.
- `resolution` — which URI plays now, and what is worth fetching for later.
- `platform` — the `androidMain`/`iosMain` adapters. They implement ports declared in `cache` and
  know nothing of the catalog.

## Nothing ships inside the app

Every track is fetched over HTTPS the first time it is played; the APK and the iOS bundle carry no
audio at all. Resolution is therefore two steps:

1. a completed app-owned local file;
2. the remote HTTPS stream.

On a cache miss the HTTPS URI is returned immediately so playback starts without waiting, while one
background, single-flight download writes to a `.part` file. Only a complete validated response is
renamed to the final MP3 name, so a half-written file is never served. Later playback of that track
resolves to `file://`.

Downloads are strictly on demand: a track is fetched because a listener picked it, never ahead of
time. The consequence is deliberate — the copy cached during a play only pays off from the *next*
play onwards, because the session already running keeps streaming the source it was handed. A first
launch without a connection therefore has nothing to play.

Resolution is split in two. `LocalFirstAudioContentResolver` answers "what can be played right
now"; `AudioPrefetcher` decides what is worth fetching and how hard to retry. They change for
different reasons, and the fetching half is the only one with a lifecycle to close.

## One cache flow, two platforms

`CachingAudioFileStore` holds the whole of the caching behaviour — the cache hit, the staged
transfer, the promotion, the sweep of names the catalog no longer refers to — and it is written
once, in `commonMain`. Each platform contributes only what cannot be shared, behind two ports
declared beside that class: `AudioCacheDirectory` (six file-system primitives, plus the local URI
and the backup exclusion iOS needs) and `AudioTransport` (one call, over `HttpsURLConnection` or
`NSURLSession`). The rules for reading an HTTP answer — status, content type, size — live in
`AudioDownloadPolicy`, likewise once.

This replaced two hand-written stores that duplicated all of the above. They had already drifted:
one compared the content type case-sensitively and the other did not, so a host answering
`Audio/MPEG` was cached on one platform and permanently refused on the other.

## Storage

Android stores files below `noBackupFilesDir/audio-content`; iOS uses
`Application Support/audio-content`, marked `NSURLIsExcludedFromBackupKey`. Neither platform puts
this audio in the user's cloud backup, because every byte of it is a repeatable download. The
directory name predates this module's own and is left alone on purpose — renaming it would strand
whatever an installed copy of the app has already cached.

A completed download sweeps every cached file the catalog no longer refers to — a superseded
version and a track dropped from the manifest alike, since neither is a name anyone will ask for
again. The set to keep comes from `AudioSourceCatalog.cacheFileNames`, so the catalog stays the one
place that decides which tracks exist. A zero-length file left behind by a failed write is discarded
on the next lookup. There is no quota eviction beyond that: a fully cached catalog is roughly 36 MB,
small enough that a quota policy would cost more than it saves. If the catalog becomes server-driven
or unbounded, quota and explicit download management belong here.

A download that fails is attempted three times with backoff, then left alone for five minutes, so
browsing the catalog with no connection does not restart a burst of requests on every tap. A source
that answers with a 4xx, the wrong content type, or more bytes than the limit allows is not retried
at all: `UnusableAudioSourceException` separates "the network dropped" from "this URL will never
serve audio". A 5xx keeps its retries — it may well be temporary.
