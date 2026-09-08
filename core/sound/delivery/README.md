# Sound delivery

One capability: **getting a track's bytes to the player**.

`SoundContentPort` is this module's only public surface. `core:session` asks it for a
playable source; features cannot depend on this module. All cache, download, retry and platform
storage types are internal.

## Ports used

The internal delivery stack communicates with other core modules only through ports:

- `SoundSourcePort` supplies the HTTPS URL and stable cache filename for a `TrackId`.
- `NetworkPort` streams response bytes without exposing Ktor.
- `SoundContentPort` returns the local path or HTTPS URI that playback may open.

No manifest, Ktor, DataStore or playback-engine implementation type crosses these boundaries.

## Metro composition

Android and iOS each contribute an internal `SoundContentPort` adapter to `AppScope`. The platform
adapter chooses its cache directory and delegates to a common internal stack made from
`CachingAudioFileStore`, `BackgroundAudioPrefetcher` and `LocalFirstSoundContentAdapter`.

- Android: `cacheDir/audio-content`
- iOS: `Library/Caches/audio-content`

There is no public factory or provider object. Metro constructs the platform adapter and injects
`NetworkPort` plus `SoundSourcePort` at compile time.

## Download and cache flow

No MP3 ships in the app. On first selection, delivery returns the manifest's HTTPS URI immediately
and starts one background download. A later request returns the completed local file.

Downloads are written to `<name>.part`, validated while streaming, flushed, then atomically moved
to the final MP3 name. Failure or cancellation removes the partial file. Empty and non-regular
files are never served.

A failed retryable download is attempted three times with backoff, then held for five minutes
before another attempt. Client errors, unsupported media types and oversized bodies are treated as
unusable sources rather than transient transport failures.

After successful promotion, cached files no longer named by `SoundSourcePort.cacheFileNames` are
removed. The operating system may also purge this cache; delivery then falls back to HTTPS and
fills it again.

Storage uses Okio `FileSystem.SYSTEM`; common tests use `FakeFileSystem` to cover staged writes,
promotion, cancellation cleanup, invalid names and catalog sweeping.
