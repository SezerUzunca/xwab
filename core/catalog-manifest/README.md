# Catalog manifest

The shipped catalog data, and the two ports that read it.

- `CatalogEntry` and the manifest — track metadata paired with its permanent HTTPS source;
- `CACHE_FILE_NAME`, the shape of the name a track's audio caches under;
- `ManifestMusicCatalogRepository`, which serves the metadata half to screens through
  `MusicCatalogRepository` (declared in `core:catalog`);
- `AudioSourceCatalog` / `TrackSource`, the physical half, read by `core:audio-delivery`.

## Why this is not part of `core:catalog`

Because every feature depends on `core:catalog`, and a public `AudioSourceCatalog` there would have
been reachable from a screen — `get<AudioSourceCatalog>()` out of the container, and a track's URL
is yours. The split is what makes "a screen cannot reach the physical source" a fact about the
compile classpath rather than a convention.

Two things follow:

- `core:catalog` holds only what a screen may know: `Music`, `Category`, `MusicCatalogRepository`.
- This module is declared by `core:audio-delivery` and by the composition root, and by nothing else.
  `checkArchitecture` rule 4 fails the build if a feature declares it.

Adding a track is still one edit in one file —
[CatalogManifest.kt](src/commonMain/kotlin/com/xwab/app/core/catalogmanifest/CatalogManifest.kt) —
because metadata and source stayed together. Splitting *those* apart is the coupling this project
already dissolved once, and is not what happened here.

## Cache file names

A cache file name is `<track id>-v<version>.mp3`, both halves manifest-owned, which is why the
catalog is what *produces* it and delivery is what *consumes* it. `CatalogEntry` refuses an id that
cannot form a safe one, so a manifest typo fails in a test rather than on a device mid-tap.

Raising an entry's `version` retires the previously cached file: the name stops being one
`AudioSourceCatalog.cacheFileNames` refers to, and delivery's sweep clears it after the next
completed download.

Staging names and the sweep itself live in `core:audio-delivery` — those describe how bytes arrive,
which is not this module's business.

## Invariants

`CatalogManifestTest` asserts what no single entry can check on its own: unique ids, unique sources,
unique cache names, a category that exists, and at least three tracks per category.
