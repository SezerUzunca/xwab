# Shuffle

`port/ShufflePort.kt` is the module's only public port interface. It serves cognitive-shuffle decks
to features: a deck's name, description, voice language and the interval between its words. The
words themselves live in an internal `ShuffleWordList` manifest, read only by
`ShufflePlaybackResolver`. The resolver implements session's consumer-owned `PlaybackItemResolver`
port and contributes it under `SHUFFLE_PLAYBACK_KIND`. Screens play a deck through the session and
never see its words.

The technique is serial diverse imagining, also known as the cognitive shuffle: unrelated, concrete,
neutral words spoken one at a time, eight seconds apart, each pictured briefly and let go. The
research behind it is early (Beaudoin and colleagues; see the screen's note), and the app does not
present it as a treatment.

## How a deck becomes audio

There is no recording and no download. On a play that the session cannot resume from a track it
already holds, the resolver draws `wordsPerTrack` words from the deck's list, without repeating one,
in a new order every time, and asks [speech](../speech/README.md) for a track: for each word half a
second of quiet, then the word held to the rest of the interval. Speech voices it with the device's
own text-to-speech and returns a local file; the resolver hands that path to the session with a
looping policy, so the track repeats until the sleep timer stops it. The request is never reused:
the same track twice would not be a shuffle.

The first play of a deck speaks every word and takes a few seconds; the session reports the item as
preparing meanwhile. Speech answering that it could not voice the deck becomes `Unavailable`, which
the screen offers to retry.

## Wiring

`ShuffleGraph` is one common graph, like sound's: it provides the manifests and the `Random` that
picks the words, binds the catalog and the resolver, and takes `SpeechPort` through its factory.
An `AppScope`-scoped `ShuffleGraphHolder` builds it once, and two bridges hand its catalog and
resolver to the application graph.

Public contracts and models live in `com.xwab.app.core.shuffle.port`; everything else stays internal
in `com.xwab.app.core.shuffle`. This module depends on `core:session` and `core:speech`. It owns no
voice, file or network of its own.

## Tests

Host tests cover the resolver's answers and the request it sends (one interval a word, no repeats, a
new order each play), the manifest rules and the production module graph with only speech replaced.
The voice itself is tested in `core:speech`.

Keep `SHUFFLE_PLAYBACK_KIND` and deck ids stable: engine ids and saved state use them. The word
list is written for this app; see [ShuffleManifest.kt](impl/src/commonMain/kotlin/com/xwab/app/core/shuffle/ShuffleManifest.kt)
for what a word must and must not be.
