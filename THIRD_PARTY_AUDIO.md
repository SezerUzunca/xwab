# Third-party audio

The app uses only recordings marked **Public domain** or **CC0** on their Wikimedia Commons source
pages. Attribution is therefore not a condition of use, but the metadata remains here so every
catalog change can be audited.

No audio ships inside the app. Sound tracks use permanent HTTPS MP3 transcodes from Wikimedia
Commons, stream on first play, and download in the background to app-owned storage. Completed Sound
downloads use local `file://` URIs later; partial files are never selected. Story recordings also
use Commons MP3 transcodes but remain online-only and are never written to the Sound cache.

## Sound catalog recordings

| Catalog ID | Source recording | Creator | License |
|---|---|---|---|
| `gentle-rain` | [Rain.ogg](https://commons.wikimedia.org/wiki/File:Rain.ogg) | ジダネ | Public domain |
| `calm-waves` | [Waves.ogg](https://commons.wikimedia.org/wiki/File:Waves.ogg) | Dsw4 | Public domain |
| `forest-birds` | [Birds forest.ogg](https://commons.wikimedia.org/wiki/File:Birds_forest.ogg) | Barracuda1983 | Public domain |
| `white-noise` | [White-noise-sound-20sec-mono-44100Hz.ogg](https://commons.wikimedia.org/wiki/File:White-noise-sound-20sec-mono-44100Hz.ogg) | Jorge Stolfi | Public domain |
| `brahms-lullaby` | [Lullaby wound up clock guten abend gute nacht.ogg](https://commons.wikimedia.org/wiki/File:Lullaby_wound_up_clock_guten_abend_gute_nacht.ogg) | stephan | Public domain |
| `heavy-rain` | [Rain (1).ogg](https://commons.wikimedia.org/wiki/File:Rain_(1).ogg) | ezwa | Public domain |
| `window-storm` | [Rain against the window.ogg](https://commons.wikimedia.org/wiki/File:Rain_against_the_window.ogg) | cori | Public domain |
| `pebble-shore` | [On a pebble beach.ogg](https://commons.wikimedia.org/wiki/File:On_a_pebble_beach.ogg) | earthcalling | Public domain |
| `shorebirds` | [Shorebirds.ogg](https://commons.wikimedia.org/wiki/File:Shorebirds.ogg) | U.S. Fish and Wildlife Service | Public domain |
| `woodland-ambience` | [20090610 0 ambience.ogg](https://commons.wikimedia.org/wiki/File:20090610_0_ambience.ogg) | nille | Public domain |
| `chorus-cicadas` | [Chorus Cicada singing.ogg](https://commons.wikimedia.org/wiki/File:Chorus_Cicada_singing.ogg) | Siobhan Leachman | CC0 1.0 |
| `brown-noise` | [Brownnoise.ogg](https://commons.wikimedia.org/wiki/File:Brownnoise.ogg) | — | Public domain |
| `pink-noise` | [Pink.Noise.ogg](https://commons.wikimedia.org/wiki/File:Pink.Noise.ogg) | Bautsch | Public domain |
| `chopin-berceuse` | [Chopin-Berceuse.ogg](https://commons.wikimedia.org/wiki/File:Chopin-Berceuse.ogg) | Veronica van der Knaap | Public domain |
| `igbo-lullaby` | [Egwu Nwa.ogg](https://commons.wikimedia.org/wiki/File:Egwu_Nwa.ogg) | Akum20 | CC0 1.0 |
| `thunder-rain` | [Rain and thunder.ogg](https://commons.wikimedia.org/wiki/File:Rain_and_thunder.ogg) | Caesar | Public domain |
| `south-carolina-beach` | [Beach sounds South Carolina.ogg](https://commons.wikimedia.org/wiki/File:Beach_sounds_South_Carolina.ogg) | Anthropic42 | Public domain |
| `nightingale-song` | [Common Nightingale's song 2.ogg](https://commons.wikimedia.org/wiki/File:Common_Nightingale%27s_song_2.ogg) | Digweed1 | CC0 1.0 |
| `gray-noise` | [Gray noise.ogg](https://commons.wikimedia.org/wiki/File:Gray_noise.ogg) | Omegatron | Public domain |
| `vierne-berceuse` | [Vierne Berceuse (Fernwerk und Hauptorgel).ogg](https://commons.wikimedia.org/wiki/File:Vierne_Berceuse_(Fernwerk_und_Hauptorgel).ogg) | Vox Mirabilis | CC0 1.0 |

## Story catalog recordings

All five recordings were uploaded by Cloudmountain (Alan Davis Drake). Each linked description
page states that the copyright holder releases the recording into the public domain worldwide and,
where that is not legally possible, grants anyone unconditional use for any purpose. The underlying
stories are original English works by authors who died in 1904 or 1900, so no later translation is
being used.

| Catalog ID | Story and source recording | Text author | Narrator | Recording grant |
|---|---|---|---|---|
| `night-came-slowly` | [The Night Came Slowly](https://commons.wikimedia.org/wiki/File:The_Night_Came_Slowly_Chopin.ogg) | Kate Chopin | Alan Davis Drake | Public domain worldwide; unconditional fallback grant |
| `an-idle-fellow` | [An Idle Fellow](https://commons.wikimedia.org/wiki/File:KateChopin_AnIdleFellow.ogg) | Kate Chopin | Alan Davis Drake | Public domain worldwide; unconditional fallback grant |
| `story-of-an-hour` | [The Story of an Hour](https://commons.wikimedia.org/wiki/File:The_Story_of_an_Hour_Chopin.ogg) | Kate Chopin | Alan Davis Drake | Public domain worldwide; unconditional fallback grant |
| `doctor-chevaliers-lie` | [Doctor Chevalier's Lie](https://commons.wikimedia.org/wiki/File:KateChopin_DrChevaliersLie.ogg) | Kate Chopin | Alan Davis Drake | Public domain worldwide; unconditional fallback grant |
| `a-tent-in-agony` | [A Tent in Agony](https://commons.wikimedia.org/wiki/File:ATentInAgony_Crane_add_Stephen_Crane.ogg) | Stephen Crane | Alan Davis Drake | Public domain worldwide; unconditional fallback grant |

Story source-page licenses, durations, and official Commons MP3 derivative metadata were checked
on 2026-08-07. The API reported a completed `audio/mpeg` derivative for every recording.

License status and direct MP3 availability for the original catalog were checked on 2026-07-28 for
the ten streamed recordings, and on 2026-07-31 for the five that had been bundled. Every original
URL returned HTTP 200 with `audio/mpeg` content at verification time. The five additions were
checked on 2026-08-06; every source page still marked its recording Public domain or CC0 and its MP3
transcode as completed.

## Removed: the bundled copies

The first five recordings above used to ship inside the app as MP3 files derived from the same
Wikimedia transcodes: trimmed, and edited with a 1.5-second equal-power crossfade for smoother loop
boundaries. They were deleted on 2026-07-31 so that the app carries no audio at all.

Two consequences are worth knowing before that decision is revisited. The crossfade is gone, so
those five now have an audible seam at every loop — which matters most for the shortest ones,
because `gentle-rain` repeats roughly every 12 seconds all night. And the sources are the full
recordings rather than the trimmed copies, so the catalog durations changed: `gentle-rain`
9 s → 12 s, `forest-birds` 13 s → 17 s, `white-noise` 19 s → 20 s, `brahms-lullaby` 45 s → 81 s,
and `calm-waves` 286 s → about 1109 s, whose download is roughly 18 MB. The deleted files remain in
git history if the trade needs to be reversed.
