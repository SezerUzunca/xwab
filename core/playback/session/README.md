# Playback session

One capability: **the single playback session the app runs.**

`PlaybackCoordinator` is the port every screen steers playback through, and `PlaybackSummary` is
the engine-independent view they render. Not a repository — there is nothing here to store or
query, only a live session to drive.

The session is content-independent: it plays a `PlaybackItemId`, which is a *kind* and a raw value.
What that item is, where its bytes come from and whether it should loop are answered by an internal
resolver, one per kind:

```
core:playback:session
   │
   ├─ PlaybackCoordinator          the only thing a screen can reach
   ├─ DefaultPlaybackCoordinator   one item at a time; newest request wins
   ├─ SoundPlaybackResolver        internal
   │    ├─► core:sound:catalog     what the track is called, for the media session to publish
   │    └─► core:sound:delivery    a local file if it is cached, HTTPS if it is not
   └─ StoryPlaybackResolver        internal
        ├─► core:story:catalog     the story's title and narrator
        └─► core:story:manifest    the address it streams from — no cache, ever
   ────► core:playback:engine      the platform player that opens whatever was resolved
```

The two resolvers are the same two steps: metadata, then a source. The sound one has a cache behind
it and the story one does not, and that is the only difference between the kinds that is meant to
last.

`play` takes an id, and the metadata is read beside the URI by the resolver. A screen handing over
a `Music` it happened to be holding could pair a stale title with a freshly resolved URI, and the
two authorities would never be compared.

The catalog, delivery and the engine are all `implementation` dependencies, and nothing this module
publishes names a type from any of them — so no screen can resolve an item to a URI or touch
`AudioPlayerState`. `checkArchitecture` rule 4 keeps it that way by failing the build on a feature
that adds one of those dependencies, or that reaches one through a module which re-exports it.

The resolvers are `internal` for the same reason the modules behind them are off limits: a resolver
hands back a URI. A public one could be pulled out of the Koin container by any screen, and the
boundary would be a comment again.

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
PlaybackItemId(SOUND, "forest")  ->  "sound:forest"
PlaybackItemId(STORY, "forest")  ->  "story:forest"
```

`forest` is a plausible name for both. Without the prefix the session would compare a story request
against an attached sound of the same name, decide it was already holding it, and send `Play`.

Reading back, an id with no known prefix is a sound. That is the upgrade path, not a guess: on
Android the media service outlives the app, so a session started by a build that wrote bare track
ids can still be attached when this one reconnects to it.

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
