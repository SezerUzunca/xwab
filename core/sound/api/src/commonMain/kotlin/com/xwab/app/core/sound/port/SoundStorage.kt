package com.xwab.app.core.sound.port

/**
 * The cache namespace sounds are downloaded under.
 *
 * Like the favourites namespace and the playback kind beside it, this is a stored name rather than
 * a constant: it is a directory on the device holding downloaded audio. Renaming it strands every
 * file already there, which is why `architecture.properties` pins its value.
 */
const val SOUND_CACHE_NAMESPACE: String = "sound"
