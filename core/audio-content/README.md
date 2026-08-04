# Audio content

One capability: **the app's sound catalog** — what a listener can pick, and the audio behind it.

This module owns:

- the catalog manifest — track metadata paired with its permanent HTTPS source;
- app-owned local copies of completed downloads;
- `MusicCatalogRepository`, the port screens read;
- `AudioContentResolver`, the port `core:playback` asks for a playable URI.

A third declaration leaves the module — `AudioFileStore`, the storage-and-transport port — but only
so the composition root can substitute it in a DI test. Remote URLs, cache file names, the store
itself and the prefetcher stay internal, so a screen holding `MusicCatalogRepository` cannot reach
the network or the file system.

The ports are not equals. Every feature has this module on its classpath for the catalog, so
`checkArchitecture` fails the build if a feature source so much as names `AudioContentResolver` or
`AudioFileStore` — resolving a track is playback's job.

Metadata and source live together on purpose: adding a track is one edit in one module. The former
split — metadata in `core:data`, resource names in the domain model, MP3s in `core:playback` — is
what made that a three-module change.

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
transfer, the promotion, the sweep — and it is written once, in `commonMain`. Each platform
contributes only what cannot be shared, behind two ports declared beside that class:
`AudioCacheDirectory` (six file-system primitives, plus the local URI and the backup exclusion iOS
needs) and `AudioTransport` (one call, over `HttpsURLConnection` or `NSURLSession`). The rules for
reading an HTTP answer — status, content type, size — live in `AudioDownloadPolicy`, likewise once.

This replaced two hand-written stores that duplicated all of the above. They had already drifted:
one compared the content type case-sensitively and the other did not, so a host answering
`Audio/MPEG` was cached on one platform and permanently refused on the other.

## Storage

Android stores files below `noBackupFilesDir/audio-content`; iOS uses
`Application Support/audio-content`, marked `NSURLIsExcludedFromBackupKey`. Neither platform puts
this audio in the user's cloud backup, because every byte of it is a repeatable download.

A completed download sweeps every cached file the catalog no longer refers to — a superseded
version and a track dropped from the manifest alike, since neither is a name anyone will ask for
again. A zero-length file left behind by a failed write is discarded on the next lookup. There is
no quota eviction beyond that: a fully cached catalog is roughly 36 MB, small enough that a quota
policy would cost more than it saves. If the catalog becomes server-driven or unbounded, quota and
explicit download management belong here rather than in the playback engine, and that is also the
point at which splitting delivery out from the catalog manifest starts to pay for itself.

A download that fails is attempted three times with backoff, then left alone for five minutes, so
browsing the catalog with no connection does not restart a burst of requests on every tap. A source
that answers with a 4xx, the wrong content type, or more bytes than the limit allows is not retried
at all: `UnusableAudioSourceException` separates "the network dropped" from "this URL will never
serve audio". A 5xx keeps its retries — it may well be temporary.
