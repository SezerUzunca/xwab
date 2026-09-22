# Playback session

One capability: **the single playback session the app runs.**

`PlaybackPort` is the port every screen steers playback through, and `PlaybackSummary` is the
engine-independent view they render. It is a live command/state boundary, not a persistence layer.

The session is content-independent: it plays a `PlaybackItemId`, which is a *kind* and a raw value.
What that item is, where its bytes come from and whether it should loop are answered through
`PlaybackItemResolver`, the consumer-owned port in this module's `.port` package. Each playable
content module implements that contract with an internal resolver under its stable playback kind:

```
core:session
   ├─ PlaybackPort             screen commands and session state
   └─ DefaultPlaybackAdapter   one item at a time; newest request wins
        ├─► core:session.port.PlaybackItemResolver
        │    └─ contributed map: playback kind → resolver
        └─► core:playback.port.PlaybackEnginePort
```

The session's only core dependency is playback. Adding a content kind requires its resolver
contribution and composition-root dependency; it requires no session change.
Removing the last contribution is supported: an optional map binding defaults to an empty map.
Requests for absent kinds publish `ItemNotFound`, including when a surviving platform service still
holds a source with that kind. Existing playback can still be observed or paused.

`play` takes an id, and the metadata is read beside the URI by the resolver. A screen handing over
a `Track` it happened to be holding could pair a stale title with a freshly resolved URI, and the
two authorities would never be compared.

`PlaybackItemResolver`, `ItemResolution` and `PlaybackPolicy` are public Kotlin types so content
modules can implement the session's contract. This module's `architecture.properties` lists them
under `adapterOnlyTypes`, and `checkArchitecture` rejects feature references to them. They are
visible at Kotlin compile time; the screen boundary is an architecture rule, not separate Gradle
classpath isolation. Features steer playback through `PlaybackPort`.

Playback is an `implementation` dependency. `checkArchitecture` also rejects feature dependencies
on network, delivery or the engine, including re-exported dependencies. Every cross-module
dependency uses a port package.

The content resolvers own metadata/source pairing and content-specific playback defaults. The
session applies those results and listener preferences; it never looks up a catalog or downloads
content itself. The screen-facing boundary remains `PlaybackPort`.

## What it decides

Things the engine cannot know on its own:

- **What looping means for this item.** The engine's own default is "no loop". A sleep sound repeats
  until the timer stops it, a story that repeats has not ended — so the default comes from the
  resolved item's `PlaybackPolicy`, while `DEFAULT_LOOPING` covers the moment before anything is
  loaded. An explicit choice by the listener is session-wide and outranks both.
- **A listener action invalidates an older source lookup.** Resolving is suspending, so a second
  tap while the first is still resolving must not load the item the listener moved on from. A
  lookup that is *cancelled* — a screen left mid-resolve — releases its claim in a `finally`, or the
  session would report a phantom item as wanted and preparing on every other screen.
- **The session is on an item from the tap, not from the load.** `requestedItemId` and `playIntent`
  are set before resolution starts, so a second tap on the same sound finds something to pause.
  Without that, two quick taps both read an idle session and the net effect was Play.
- **What a lookup could not produce.** A resolution comes back `Resolved`, `NotFound` or
  `Unavailable`, and the last two become a `PlaybackFailure` on screen rather than a tap that
  silently did nothing. An item of a kind no resolver answers for fails the same way, before the
  engine is asked to open anything.

## Ids the engine can hold

The engine identifies a source by plain string — it is a standalone library and has no idea what a
kind is. `PlaybackItemId` is written into that string with its kind in front:

```
PlaybackItemId("sound", "forest")  ->  "sound:forest"
PlaybackItemId("story", "forest")  ->  "story:forest"
```

`forest` is a plausible name for both. Without the prefix the session would compare a story request
against an attached sound of the same name, decide it was already holding it, and send `Play`.

Reading back preserves any nonblank kind and value. Bare IDs and empty halves are rejected; the
session never guesses a content kind. A removed kind remains readable in retained engine state,
but a new play request for it requires an installed resolver. Content modules must keep their
playback kinds stable because these IDs can outlive the app process on Android.

## Four fields that are easy to confuse

`PlaybackSummary` keeps them apart because during a switch they genuinely disagree — the listener
has asked for B while A is still the sound in the room:

| Field | Answers |
|---|---|
| `requestedItemId` | what the listener last asked for — set from the tap, before any lookup |
| `activeItemId` | what the engine is holding, and therefore what `isPlaying` is about |
| `playIntent` | whether playback is *wanted* |
| `isPlaying` | whether sound is coming out right now |

A screen highlights and acts on `requestedItemId` + `playIntent`. Collapsing the two ids into one
published "B is playing" for as long as B took to resolve; drawing `isPlaying` while branching on
the desired state is how a tap during buffering paused a sound the screen showed as stopped.

A screen that lists one kind of thing reads `requestedValueOf(kind)`, which is null when the session
is on something else — a story playing lights up no row in a list of sounds.

`PlaybackFailure` carries its own `itemId` for the same reason. A failed lookup releases the claim
that produced it, so by the time the failure is published the session has fallen back to whatever
came before. A screen asking "is this mine?" has to compare against the failure's item — matching
on the session's current one meant a resolution error reached no listener at all.
