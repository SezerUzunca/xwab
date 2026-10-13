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
already holds, the resolver asks `ShuffleTrackRenderer` for a track:

1. The renderer draws `wordsPerTrack` words from the deck's list, without repeating one, in a new
   order every time.
2. It opens the device's own voice for the deck's language — Android's `TextToSpeech`, iOS's
   `AVSpeechSynthesizer` — through the internal `WordVoice` each platform graph binds. Each word is
   spoken once per voice into its own WAV file under `words/<voice>/` and kept; a later render reuses
   it, and a different voice speaks the words again.
3. `writeShuffleTrack` lays the words out one per slot of `wordIntervalSeconds`: half a second of
   quiet, the word evened out to the same moderate peak with short fades, then silence until the
   next slot. It writes one mono 16-bit WAV file under `tracks/`, deleting the deck's earlier one.
4. The resolver returns the file's absolute path, which both platform players open as a local file,
   with a looping policy: the track repeats until the sleep timer stops it.

The first play of a deck speaks every word and takes a few seconds; the session reports the item as
preparing meanwhile. A device without a voice for the deck's language, or a file that could not be
written, is `Unavailable`, which the screen offers to retry. Everything lives under the platform's
cache directory (`shuffle/`), so the system may clear it: all of it can be spoken again.

Android 11 and later hide other apps' services unless the app declares which it looks for, so this
module's manifest declares the `TTS_SERVICE` intent; without it no engine is found.

## Wiring

`ShuffleBindings` holds what every platform shares: the manifests, the catalog, the renderer, the
resolver, the file system, the renderer's dispatcher, the `Random` that picks the words and the
logger. `AndroidShuffleGraph` (from the `Context`) and `IosShuffleGraph` bind the platform's voice
and cache location. One `AppScope`-scoped holder per platform builds the graph once, and two bridges
hand its catalog and resolver to the application graph, as sound and story do.

Public contracts and models live in `com.xwab.app.core.shuffle.port`; everything else stays internal
in `com.xwab.app.core.shuffle`. This module depends only on `core:session`. It owns no network
client and no download cache.

## Tests

Host tests cover the WAV reader and writer against hand-written bytes, the track layout, word
caching and track replacement over a fake file system, the resolver's three answers, the manifest
rules and the module bindings over the real file system with a stand-in voice. On a device,
`AndroidShuffleGraphTest` builds the production graph, has the device's engine speak a word, and
renders the shipped deck; the two speaking tests are skipped on an image without a text-to-speech
engine. `IosShuffleGraphTest` builds the iOS graph on the simulator without speaking, since CI
cannot rely on the simulator's synthesizer.

Keep `SHUFFLE_PLAYBACK_KIND` and deck ids stable: engine ids and saved state use them. The word
list is written for this app; see [ShuffleManifest.kt](impl/src/commonMain/kotlin/com/xwab/app/core/shuffle/ShuffleManifest.kt)
for what a word must and must not be.
