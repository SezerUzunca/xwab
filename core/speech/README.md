# Speech

`port/SpeechPort.kt` is the module's only public port interface. It speaks a content capability's
text with the device's own voice into a local audio track that both platform players open: shuffle
words and relaxation guides today. It is to spoken content what delivery is to downloaded content,
and like delivery it is not feature-accessible (`featureAccessible=false`): screens play the content
module's items through the session, never ask for speech.

## What a request is

A `SpeechRequest` names a `folder` (the calling module's playback kind) and a `name` (its item id),
a voice language, and segments in order: `Spoken(text, minSeconds)` — the text in the device's
voice, held to at least that long — and `Pause(seconds)`. An optional `NoiseBed` lays soft brown
noise under the whole track, rising and falling over its fade. `reuse` says whether a track already
made from the same segments with the same voice may be handed back as it is: true for fixed content,
false where every render should differ.

`render` answers `Rendered(path)` or `Unavailable(reason)` — no voice for the language, or no room
to write the track. Cancellation propagates.

## How a track is made

1. The platform's voice is opened for the language: Android's `TextToSpeech`, writing each
   utterance with `synthesizeToFile`, or iOS's `AVSpeechSynthesizer`, written to buffers with
   `writeUtterance`. Android 11 and later need the `TTS_SERVICE` query this module's manifest
   declares, or no engine is found.
2. Each piece of text is spoken once per voice into `utterances/<voice>/<fingerprint>.wav` and kept;
   a later track that says it again only reads it back, and a different voice speaks it anew. An
   unreadable kept utterance is deleted and spoken again next time.
3. `writeSpeechTrack` lays the segments out as one mono 16-bit WAV file at the first utterance's
   rate: every piece of speech brought to the same moderate peak with short fades, silences written
   a block at a time, the bed mixed in sample by sample when there is one.
4. The track goes to `tracks/<folder>/<name>.<version>.wav`. A reusable request's version is a
   fingerprint of the voice and the content, so the same request finds it again; any other gets a
   fresh name, because a player handed the address it already holds may keep the old audio. Earlier
   tracks of the same name are deleted. The separator is a character names cannot contain, so one
   name's tracks are never taken for another's.

Renders run one at a time. Everything lives under the platform's cache directory (`speech/`), so the
system may clear it: all of it can be spoken again.

## Wiring

`SpeechBindings` holds what every platform shares: the renderer, the file system, its dispatcher,
the `Random` behind fresh names and the bed, and the logger. `AndroidSpeechGraph` (from the
`Context`) and `IosSpeechGraph` bind the platform's voice and cache location, and one bridge per
platform hands `SpeechPort` to the application graph.

## Tests

Host tests cover the WAV reader and writer against hand-written bytes, the track layout and the bed,
utterance caching, reuse, track replacement and logging over a fake file system, and the bindings
over the real file system with a stand-in voice. On a device, `AndroidSpeechGraphTest` builds the
production graph, has the device's engine speak, and renders and reuses a track with a bed; the two
speaking tests are skipped on an image without a text-to-speech engine. `IosSpeechGraphTest` builds
the iOS graph on the simulator without speaking, since CI cannot rely on the simulator's
synthesizer.
