# Guide

`port/Guide.kt` holds the module's only public port interface, `GuidePort`, which serves spoken
relaxation guides to features: a guide's name, description, rough length and voice language. Each
guide's script lives in an internal `GuideScript` manifest, read only by `GuidePlaybackResolver`,
which implements session's consumer-owned `PlaybackItemResolver` and contributes it under
`GUIDE_PLAYBACK_KIND`. Screens play a guide through the session and never see its script.

The guides were written for this app: a muscle-relaxation guide that tenses and releases one group
of muscles at a time, gently and skipping anything that hurts, and three imagery guides — a beach,
a forest path, a cabin in the rain. They draw on published research into progressive muscle
relaxation and guided imagery; they promise no sleep, and the screen says they are not a treatment.

## How a guide becomes audio

The resolver asks [speech](../speech/README.md) for a track: three seconds of the bed alone, then
each line followed by its pause — how long a muscle is held and rests, or how long a picture is
given — and forty-five seconds of the bed after the last line, fading out. The bed is soft brown
noise well under the voice: it masks a day's household and street sounds and keeps the pauses from
sounding like a dropped connection. The request is reusable, since a guide's words never change, so
speech makes the track once per voice and only the first play waits. A guide plays once and ends.

An unknown guide is `NotFound`; one speech could not voice is `Unavailable`, which the screen offers
to retry.

## Wiring

`GuideGraph` is one common graph taking `SpeechPort` through its factory; an `AppScope`-scoped
`GuideGraphHolder` builds it once, and two bridges hand its catalog and resolver to the application
graph. Public contracts and models live in `com.xwab.app.core.guide.port`; everything else stays
internal. This module depends on `core:session` and `core:speech`.

## Tests

Host tests check that every guide has its script, that the shipped ids stay put, that each guide
runs roughly as long as it says, the request the resolver sends and its answers, and the production
module graph with only speech replaced.

Keep `GUIDE_PLAYBACK_KIND` and guide ids stable: engine ids and saved state use them. Changing a
script changes its fingerprint, so devices speak the new version on its next play.
