# Nap

`port/Nap.kt` holds the module's only public port interface, `NapPort`, which serves fixed-length
naps to features: a nap's name, description, length and voice language. What each nap says lives in
`NapTrack.kt`, read only by `NapPlaybackResolver`, which implements session's consumer-owned
`PlaybackItemResolver` and contributes it under `NAP_PLAYBACK_KIND`. Screens play a nap through the
session.

There are three: ten, fifteen and twenty minutes. Published studies find short naps restore
alertness with little of the grogginess that follows waking from deep sleep, which longer naps
reach; a longer nap would also be a track of hundreds of megabytes, since tracks are uncompressed.

## How a nap becomes audio

The resolver asks [speech](../speech/README.md) for one track over soft brown noise: a settling-in
line held for the nap's whole length, so the wake-up starts exactly that long after play whatever
the voice's pace, then three rounds of wake-up lines, a closing line and a short fade. The request
is reusable, so speech makes each nap once per voice and only the first play waits. A nap plays once
and ends.

**It is not an alarm.** Nothing is measured and nothing is scheduled: the wake-up is the end of a
track the media player plays. A phone call, another app taking the audio, or a low media volume can
keep it from being heard, and the screen tells the listener to set their phone's own alarm too.
Measuring when the listener fell asleep, or moving the wake-up to suit it, is deliberately absent.

An unknown nap is `NotFound`; one speech could not voice is `Unavailable`, which the screen offers to
retry.

## Wiring

`NapGraph` is one common graph taking `SpeechPort` through its factory; an `AppScope`-scoped
`NapGraphHolder` builds it once, and two bridges hand its catalog and resolver to the application
graph. Public contracts and models live in `com.xwab.app.core.nap.port`; everything else stays
internal. This module depends on `core:session` and `core:speech`.

## Tests

Host tests check the shipped ids, lengths and kind, that the wake-up starts at the nap's length and
is short, the resolver's answers, and the production module graph with only speech replaced.

Keep `NAP_PLAYBACK_KIND` and nap ids stable: engine ids and saved state use them.
