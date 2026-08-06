# Playback session

One capability: **the single playback session the app runs.**

`PlaybackCoordinator` is the port every screen steers playback through, and `PlaybackSummary` is
the engine-independent view they render. Not a repository — there is nothing here to store or
query, only a live session to drive.

This module is where the three halves of playing a sound meet, and it is the only place they do:

```
core:playback:session
   ├─► core:sound:catalog     what the track is called, for the media session to publish
   ├─► core:sound:delivery    the URI its bytes can be read from
   └─► core:playback:engine   the platform player that opens it
```

`play` takes a track *id*, and reads the metadata from the catalog itself. A screen handing over a
`Music` it happened to be holding could pair a stale title with a freshly resolved URI, and the two
authorities would never be compared.

Delivery and the engine are `implementation` dependencies, and no feature declares either — so no
screen can resolve a track to a URI or touch `AudioPlayerState`. `checkArchitecture` rule 4 keeps it
that way by failing the build on a feature that adds either dependency.

`PlaybackSummary` lives here rather than in a shared model module for the same reason the ports do —
it is this capability's projection of the engine's state, and mapping to it inside
`DefaultPlaybackCoordinator` is what keeps `AudioPlayerState`, `PlaybackPhase` and `LoopMode` from
reaching a ViewModel.

## What it decides

Things the engine cannot know on its own:

- **Sleep sounds loop.** The engine's own default is "no loop". `DEFAULT_LOOPING` lives here and is
  applied in one place — both to what the next load is handed and to what `PlaybackSummary`
  publishes — so a loop turned off before the first play is obeyed and shown consistently.
- **A listener action invalidates an older source lookup.** Resolving is suspending, so a second
  tap while the first is still resolving must not load the sound the listener moved on from. A
  lookup that is *cancelled* — a screen left mid-resolve — releases its claim in a `finally`, or the
  session would report a phantom track as wanted and preparing on every other screen.
- **The session is on a track from the tap, not from the load.** `PlaybackSummary.trackId` and
  `playIntent` are set before resolution starts, so a second tap on the same sound finds something
  to pause. Without that, two quick taps both read an idle session and the net effect was Play.
- **What a lookup could not produce.** `AudioSourceResolution` comes back as `Resolved`, `NotFound`
  or `Unavailable`, and the last two become a `PlaybackFailure` on screen rather than a tap that
  silently did nothing.

## Four fields that are easy to confuse

`PlaybackSummary` keeps them apart because during a switch they genuinely disagree — the listener
has asked for B while A is still the sound in the room:

| Field | Answers |
|---|---|
| `requestedTrackId` | what the listener last asked for — set from the tap, before any lookup |
| `activeTrackId` | what the engine is holding, and therefore what `isPlaying` is about |
| `playIntent` | whether playback is *wanted* |
| `isPlaying` | whether sound is coming out right now |

A screen highlights and acts on `requestedTrackId` + `playIntent`. Collapsing the two ids into one
published "B is playing" for as long as B took to resolve; drawing `isPlaying` while branching on
the desired state is how a tap during buffering paused a sound the screen showed as stopped.

`PlaybackFailure` carries its own `trackId` for the same reason. A failed lookup releases the claim
that produced it, so by the time the failure is published the session has fallen back to whatever
came before. A screen asking "is this mine?" has to compare against the failure's track — matching
on the session's current one meant a resolution error reached no listener at all.
