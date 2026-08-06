# Catalog

One capability: **what a listener can pick.** `Music`, `Category`, `TrackId`, and the port screens
read them through.

That is the whole module — four declarations and no data. It depends on nothing else in this
build, and every feature depends on it.

`TrackId` is a `@JvmInline value class`, so it erases to the `String` it holds and the safety is
free. It is unwrapped in exactly three places, each of them an edge where a string is genuinely the
format: the cache file name a track downloads under, a serialized navigation route, and
`core:playback-engine`, which is a standalone library that knows nothing about this catalog.

The shipped manifest, the HTTPS source behind each track, and the name it caches under live next
door in [core:catalog-manifest](../catalog-manifest/README.md), which implements
`MusicCatalogRepository` and which no feature declares. The split is deliberate: a port that hands
out a URL cannot sit in a module that is on every screen's classpath, or "a screen must not resolve
a track itself" is a convention rather than a fact.

So:

| Port | Answers | Declared by |
|---|---|---|
| `MusicCatalogRepository` (here) | *what is there to play?* | every feature |
| `AudioSourceCatalog` (next door) | *where do this track's bytes come from?* | `core:audio-delivery` |

An earlier arrangement had both halves in one module. That put delivery on every feature's
classpath and made the boundary a `checkArchitecture` rule that scanned source text for two class
names — a guard any rename would have silently defeated. It is a dependency edge now, and the rule
that holds it fails loudly if the module it names ever stops existing.
