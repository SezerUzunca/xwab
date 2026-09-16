package com.xwab.app.core.sources

import com.xwab.app.core.sources.port.ContentSource

internal val soundSourceManifest = listOf(
    soundSource("gentle-rain", "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/3d/Rain.ogg/Rain.ogg.mp3"),
    soundSource("calm-waves", "https://upload.wikimedia.org/wikipedia/commons/transcoded/1/1f/Waves.ogg/Waves.ogg.mp3"),
    soundSource("forest-birds", "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/38/Birds_forest.ogg/Birds_forest.ogg.mp3"),
    soundSource("white-noise", "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/98/White-noise-sound-20sec-mono-44100Hz.ogg/White-noise-sound-20sec-mono-44100Hz.ogg.mp3"),
    soundSource("brahms-lullaby", "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/cf/Lullaby_wound_up_clock_guten_abend_gute_nacht.ogg/Lullaby_wound_up_clock_guten_abend_gute_nacht.ogg.mp3"),
    soundSource("heavy-rain", "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/0e/Rain_(1).ogg/Rain_(1).ogg.mp3"),
    soundSource("window-storm", "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/41/Rain_against_the_window.ogg/Rain_against_the_window.ogg.mp3"),
    soundSource("pebble-shore", "https://upload.wikimedia.org/wikipedia/commons/transcoded/7/73/On_a_pebble_beach.ogg/On_a_pebble_beach.ogg.mp3"),
    soundSource("shorebirds", "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/91/Shorebirds.ogg/Shorebirds.ogg.mp3"),
    soundSource("woodland-ambience", "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/0a/20090610_0_ambience.ogg/20090610_0_ambience.ogg.mp3"),
    soundSource("chorus-cicadas", "https://upload.wikimedia.org/wikipedia/commons/transcoded/b/b1/Chorus_Cicada_singing.ogg/Chorus_Cicada_singing.ogg.mp3"),
    soundSource("brown-noise", "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c9/Brownnoise.ogg/Brownnoise.ogg.mp3"),
    soundSource("pink-noise", "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/a2/Pink.Noise.ogg/Pink.Noise.ogg.mp3"),
    soundSource("chopin-berceuse", "https://upload.wikimedia.org/wikipedia/commons/transcoded/d/d5/Chopin-Berceuse.ogg/Chopin-Berceuse.ogg.mp3"),
    soundSource("igbo-lullaby", "https://upload.wikimedia.org/wikipedia/commons/transcoded/d/d9/Egwu_Nwa.ogg/Egwu_Nwa.ogg.mp3"),
    soundSource("thunder-rain", "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/42/Rain_and_thunder.ogg/Rain_and_thunder.ogg.mp3"),
    soundSource("south-carolina-beach", "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/04/Beach_sounds_South_Carolina.ogg/Beach_sounds_South_Carolina.ogg.mp3"),
    soundSource("nightingale-song", "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/91/Common_Nightingale%27s_song_2.ogg/Common_Nightingale%27s_song_2.ogg.mp3"),
    soundSource("gray-noise", "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c0/Gray_noise.ogg/Gray_noise.ogg.mp3"),
    soundSource("vierne-berceuse", "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c0/Vierne_Berceuse_%28Fernwerk_und_Hauptorgel%29.ogg/Vierne_Berceuse_%28Fernwerk_und_Hauptorgel%29.ogg.mp3"),
)

internal fun soundSource(itemId: String, httpsUrl: String, version: Int = 1): ManifestSource {
    require(version > 0) { "Sound source versions must be positive." }
    return ManifestSource(
        itemId = itemId,
        source = ContentSource(
            httpsUrl = httpsUrl,
            cacheFileName = "$itemId-v$version.mp3",
            headers = mapOf("User-Agent" to WIKIMEDIA_USER_AGENT),
        ),
    )
}

/**
 * Identifies this client to Wikimedia, which every shipped sound source streams from today.
 *
 * Wikimedia's user-agent policy refuses a request that does not say who is making it, and asks for
 * a way to reach whoever is making it — a URL or an address, not just a product name. That refusal
 * arrives as a 4xx, which delivery classifies as a source that will never work: no retry, and the
 * file is not cached. Playback would keep streaming over HTTPS through the platform player, so the
 * only visible effect would be a cache that quietly never fills — which is why this header travels
 * with the source itself rather than being guessed by whatever resolves it.
 *
 * Reaches the *download*, and only the download. On a cache miss delivery hands playback the HTTPS
 * URL and the platform player opens it directly, under its own user agent, with nothing here
 * applying — see `AudioSource`, which carries no headers to give it.
 */
private const val WIKIMEDIA_USER_AGENT = "SleepSounds/1.0 (https://github.com/SezerUzunca/xwab)"
