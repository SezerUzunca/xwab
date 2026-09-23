package com.xwab.app.core.sound.port

/**
 * The cache namespace sounds are downloaded under.
 *
 * Public because the composition root has to name it. Everything a content module caches lives in
 * a directory of this name, and the only place that can say which of those directories still
 * belong to an installed capability is the module that assembles the app — see
 * `INSTALLED_CACHE_NAMESPACES`.
 *
 * Like the favourites namespace and the playback kind beside it, this is a stored name rather than
 * a constant: it is a directory on the device holding downloaded audio. Renaming it strands every
 * file already there, which is why `architecture.properties` pins its value.
 */
public const val SOUND_CACHE_NAMESPACE: String = "sound"
