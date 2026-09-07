# Sound catalog

One capability: **what a listener can pick.** `SoundCatalogPort` is the public boundary; `Music`,
`Category` and their typed ids are public values in the same `sound.port` contract package.

That is the whole module — a port, its model values and no data. It depends on nothing else in this
build, and every sound feature depends on it.

`TrackId` is a `@JvmInline value class`, so it erases to the `String` it holds and the safety is
free — while it stays in a typed position. It is unwrapped in exactly four places, each an edge
where a string is genuinely the format: the cache file name a track downloads under, a serialized
navigation route, a Compose lazy-list key (those go into a `Bundle`, which cannot hold a boxed value
class), and the `PlaybackItemId` a screen hands the playback session, which pairs the raw value with
a kind because a story may share it.

`Music` and `TrackId` check their own invariants — a blank id, a blank name, a duration that is not
positive — the same way `Story` and `StoryId` do. The two content types are held to one standard;
[core:story:catalog](../../story/catalog/README.md) lists it.

The shipped manifest, the HTTPS source behind each track, and the name it caches under live next
door in [core:sound:manifest](../manifest/README.md), which implements
`SoundCatalogPort` and which no feature declares. The split is deliberate: a port that hands
out a URL cannot sit in a module that is on every screen's classpath, or "a screen must not resolve
a track itself" is a convention rather than a fact.

So:

| Port | Answers | Consumed by |
|---|---|---|
| `SoundCatalogPort` (here) | *what is there to play?* | every sound feature |
| `SoundSourcePort` (next door) | *where do this track's bytes come from?* | `core:sound:delivery` |

An earlier arrangement had both halves in one module. That put delivery on every feature's
classpath and made the boundary a `checkArchitecture` rule that scanned source text for two class
names — a guard any rename would have silently defeated. It is a dependency edge now, and the rule
that holds it fails loudly if the module it names ever stops existing.
