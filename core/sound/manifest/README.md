# Sound manifest

The shipped catalog data, and the two ports that read it.

- `CatalogEntry` and the manifest — track metadata paired with its permanent HTTPS source;
- `CatalogEntry.cacheFileName`, the validated name a track's audio caches under;
- `ManifestSoundCatalogAdapter`, which serves the metadata half to screens through
  `SoundCatalogPort` (declared in `core:sound:catalog`);
- `SoundSourcePort` / `TrackSource`, the physical half, read by `core:sound:delivery`.

The catalog is intentionally local. Sound networking is limited to downloading the selected
track's bytes in `core:sound:delivery`; this module has no HTTP client, feed DTOs, refresh lifecycle
or retry policy.

## Why this is not part of `core:sound:catalog`

Because every sound feature depends on `core:sound:catalog`, placing `SoundSourcePort` there would
put the physical-source contract on each screen's compile classpath. Keeping it here makes "a
screen cannot reach the physical source" a fact about module dependencies rather than a convention.

Two things follow:

- `core:sound:catalog` holds only what a screen may know: `Music`, `Category`, `SoundCatalogPort`.
- This module is declared by `core:sound:delivery` and by the composition root, and by nothing else.
  `checkArchitecture` fails the build if a feature declares it.

Adding a track is still one edit in one file —
[CatalogManifest.kt](src/commonMain/kotlin/com/xwab/app/core/soundsource/CatalogManifest.kt) —
because metadata and source stayed together. Splitting *those* apart is the coupling this project
already dissolved once, and is not what happened here.

## Cache file names

A cache file name is `<track id>-v<version>.mp3`, both halves manifest-owned, which is why the
catalog is what *produces* it and delivery is what *consumes* it. `CatalogEntry` refuses an id that
cannot form a safe one, so a manifest typo fails in a test rather than on a device mid-tap.

Raising an entry's `version` retires the previously cached file: the name stops being one
`SoundSourcePort.cacheFileNames` refers to, and delivery's sweep clears it after the next
completed download.

Staging names and the sweep itself live in `core:sound:delivery` — those describe how bytes arrive,
which is not this module's business.

## Invariants

`CatalogManifestTest` asserts what no single entry can check on its own: unique ids, unique sources,
unique cache names, a category that exists, and at least four tracks per category.
