# Third-party audio

The app uses only recordings marked **Public domain** or **CC0** on their Wikimedia Commons source
pages. Attribution is therefore not a condition of use, but the metadata remains here so every
catalog change can be audited.

## Bundled recordings

The bundled MP3 files were derived from Wikimedia's generated transcodes and edited with a
1.5-second equal-power crossfade to create smoother loop boundaries.

| Bundled file | Source recording | Creator | License | SHA-256 |
|---|---|---|---|---|
| `gentle_rain.mp3` | [Rain.ogg](https://commons.wikimedia.org/wiki/File:Rain.ogg) | ジダネ | Public domain | `b852289b81a243b6341e40320038ae39fcc3228c90056790b4bce7ed3dddfedd` |
| `calm_waves.mp3` | [Waves.ogg](https://commons.wikimedia.org/wiki/File:Waves.ogg) | Dsw4 | Public domain | `1bd668a3949e0f81f6ed08e814fd583a9025161cd8a0d81515969001e2e95dc5` |
| `forest_birds.mp3` | [Birds forest.ogg](https://commons.wikimedia.org/wiki/File:Birds_forest.ogg) | Barracuda1983 | Public domain | `fa5d7e3b9a3d687e578ff423a45a945e14c2645aa63d88cb0dac2ef0c3935b9e` |
| `white_noise.mp3` | [White-noise-sound-20sec-mono-44100Hz.ogg](https://commons.wikimedia.org/wiki/File:White-noise-sound-20sec-mono-44100Hz.ogg) | Jorge Stolfi | Public domain | `564cb7c1f08bc1a0a5805352bba16ee0145ebd43a60b04ad9543e8e44b939a2e` |
| `brahms_lullaby.mp3` | [Lullaby wound up clock guten abend gute nacht.ogg](https://commons.wikimedia.org/wiki/File:Lullaby_wound_up_clock_guten_abend_gute_nacht.ogg) | stephan | Public domain | `b4c4c16c7f82ab5368ea64e19c9dfc899836e811af94adf932c18a7d81c4be50` |

## Streamed and locally cached recordings

The catalog stores permanent HTTPS MP3 transcode URLs from Wikimedia Commons. On first playback a
recording streams immediately and is downloaded in the background to app-owned storage. A
completed download is used through a local `file://` URI on later playback. Partial files are never
selected.

| Catalog ID | Source recording | Creator | License |
|---|---|---|---|
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

License status and direct MP3 availability were checked on 2026-07-28. Every remote URL returned
HTTP 200 with `audio/mpeg` content and byte-range support at verification time.
