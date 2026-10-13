# Rehearsal

`port/Rehearsal.kt` holds the module's only public port interface, `RehearsalPort`, which keeps the
listener's rewritten nightmare for imagery rehearsal: one script at a time, on this device and
nowhere else, played through the session as an item of `REHEARSAL_PLAYBACK_KIND`. Imagery
rehearsal works on one dream until it changes, so there is no list.

The technique is the listener's: they write down a recurring nightmare's new version, changed
however they like, and go through it in their mind while awake, every day. The module only keeps
the words and has the device's voice read them back, framed by a few lines written for this app.
The screen says it is not a treatment and points to a professional for nightmares after trauma.

## Storage and identity

The script is a note under the module's own key, `rehearsal`, through [notes](../notes/README.md);
`RehearsalTest` pins the key, since renaming it strands every script. Saving trims the text and a
blank one removes it; a script is at most `REHEARSAL_SCRIPT_MAX_LENGTH` characters.

`RehearsalScript.id` is a 64-bit FNV-1a hash of the text. Each wording is its own playback item
because the session resumes an item it already holds instead of resolving it again: with one id for
every wording, a script changed after a play would resume the old track. The hash is the same on
every platform and run, as a saved engine source id needs.

## How a script becomes audio

`RehearsalPlaybackResolver` resolves only the stored wording's id; an older one is `NotFound`, as a
removed guide is. It asks [speech](../speech/README.md) for a track: three seconds of the bed alone,
four settling-in lines, the listener's script a sentence at a time with six seconds after each to
picture it, then forty-five seconds to go through it again silently and three closing lines that end
by asking the listener to open their eyes, since rehearsal is practised awake. The bed is brown
noise, softer than a guide's. The request is reusable and always named `own-dream`, so speech makes
a track once per wording and voice, and a new wording replaces the old file. One that speech could
not voice is `Unavailable`, which the screen offers to retry. A rehearsal plays once and ends.

## Wiring

`RehearsalGraph` is one common graph taking `NotesPort` and `SpeechPort` through its factory; an
`AppScope`-scoped `RehearsalGraphHolder` builds it once, and two bridges hand its port and resolver
to the application graph. Public contracts and models live in `com.xwab.app.core.rehearsal.port`;
everything else stays internal. This module depends on `core:notes`, `core:session` and
`core:speech`.

## Tests

Host tests cover saving, trimming, removing and a refused write over a fake note store, the pinned
key and kind, the hash's stability, sentence splitting, the request the resolver sends and its
answers, and the production module graph with notes and speech replaced.
