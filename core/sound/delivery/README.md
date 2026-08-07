# Sound delivery

One capability: **getting a track's bytes to the player.** This module decides which source can play
now, what should be fetched for later, and how app-owned audio copies are stored.

It owns:

- `AudioContentResolver`, the port used by `core:playback:session`;
- `AudioFileStore` and the common cache flow: lookup, staged write, durable flush, atomic promotion,
  cleanup and catalog sweep;
- the sound-specific HTTP response policy: accepted status, media type, declared/received size;
- the Android and iOS composition that selects the app's cache root.

It does not own Ktor or generic HTTP behavior. `core:network` owns that transport behind
`NetworkClient`. It also does not own the catalog; `core:sound:manifest` supplies source URLs and
cache names through `AudioSourceCatalog`.

## Dependency boundary

`core:playback:session` consumes delivery and `shared` installs its Koin modules. Features do not
depend on this module directly. The architecture check enforces that build-graph rule.

The common code has only two internal packages:

```text
cache <- resolution
  ^         ^
  +--- di --+
```

- `cache` contains `AudioFileStore`, `CachingAudioFileStore`, the sweep and audio response rules.
- `resolution` chooses the immediately playable source and schedules single-flight prefetch work.
- `di` supplies common bindings plus the small platform-specific cache-root adapters.

There is no platform `transport` or file-system implementation. Those duplicate adapters were
removed when storage moved to Okio.

## Download and cache flow

Every track is fetched over HTTPS the first time it is selected; no MP3 is bundled in the app.
Resolution returns a completed local file when one exists, otherwise it returns the remote HTTPS
URI immediately and starts one background download. A cache hit returns an absolute local path;
the platform playback adapter turns it into its native URL. Therefore the first play can stream while the
download benefits later plays.

`CachingAudioFileStore` writes the network chunks to `<name>.part`. It validates the response while
streaming, closes the buffered sink, calls `FileHandle.flush()`, and then uses
`FileSystem.atomicMove()` to expose the final MP3. Failures and cancellation remove the partial file
in a non-cancellable cleanup. Zero-byte and non-regular files are never served.

### What `flush()` guarantees, per platform

Okio documents `FileHandle.flush()` as *"pushes all buffered bytes to their final destination"* and
promises nothing about the storage device. What the implementations actually do differs, and the
difference matters here because a promoted file is trusted from then on:

| | `protectedFlush()` does | Effect |
|---|---|---|
| JVM / Android | `randomAccessFile.fd.sync()` | a real fsync: the bytes are on the device before the move |
| Unix / Apple | `fflush(file)` | the C buffer reaches the kernel; the device sync is the kernel's own schedule |

So on iOS a kernel panic or power loss in the window between a finished download and the kernel's
writeback can leave a promoted file whose bytes never landed. `find()` deletes a zero-length file,
which covers the common shape of that failure, but a partially written one would be served and
would fail in the player — and nothing removes it, so it fails every time after that.

The transports this replaced called `fsync` explicitly on both platforms. Restoring the iOS half
means one `expect`/`actual` capability in this module, because Okio's common API has no fsync. That
is a deliberate open question, not an oversight.

The module currently pins Okio 3.17.0. Storage uses the multiplatform `FileSystem.SYSTEM`; common
tests use `FakeFileSystem`, including staged promotion, cleanup, cancellation and unsafe-name
coverage. This follows the current [Okio FileSystem API](https://square.github.io/okio/3.x/okio/okio/-file-system/)
and [Okio FakeFileSystem API](https://square.github.io/okio/3.x/okio-fakefilesystem/okio-fakefilesystem/okio.fakefilesystem/-fake-file-system/).

`core:network` streams Ktor response chunks instead of buffering an entire audio file, consistent
with Ktor's current [streaming response guidance](https://ktor.io/docs/client-responses.html).

## Platform storage

Android stores files under `cacheDir/audio-content`; iOS uses `Library/Caches/audio-content`.
Both are OS-managed cache locations for content that can be downloaded again, so neither needs a
backup exclusion adapter. The operating system may purge them under storage pressure; the resolver
then falls back to the remote HTTPS source and fills the cache again.

Platform DI contributes one `AudioCacheRoot` value. Store construction, `FileSystem.SYSTEM`, the
I/O dispatcher, URI-independent file operations and all cache policy stay in `commonMain`. Okio
cannot choose an application's sandbox directory, so that root is the sole platform storage input.

## Retrying and cleanup

A failed download is attempted three times with backoff, then held for five minutes before another
attempt. A 4xx, unexpected media type or oversized body becomes `UnusableAudioSourceException` and
is not retried; 5xx and transport failures remain retryable.

After a successful promotion, files no longer referenced by `AudioSourceCatalog.cacheFileNames`
are removed. Partial names are handled by the transfer cleanup. There is no quota eviction today;
the bounded shipped catalog is small enough that version/catalog cleanup is sufficient.
