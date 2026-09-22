package com.xwab.app.core.sound

import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.sound.port.TrackId
import dev.zacsweers.metro.Inject

/** One sound's validated download request; neither the address nor the cache key leaves this module's API. */
internal data class SoundSource(
    val trackId: TrackId,
    val request: DeliveryRequest,
)

/** This catalog owns sound identity and the complete inventory needed for sound cache cleanup. */
internal class SoundSources internal constructor(sources: List<SoundSource>) {
    @Inject
    internal constructor() : this(soundSourceManifest)

    private val requestsByTrackId: Map<TrackId, DeliveryRequest>

    init {
        val duplicateIds = sources.groupBy(SoundSource::trackId).filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) {
            "Sound source ids must be unique: ${duplicateIds.joinToString()}"
        }
        val duplicateCacheNames = sources.groupBy { it.request.key.fileName }.filterValues { it.size > 1 }.keys
        require(duplicateCacheNames.isEmpty()) {
            "Sound cache filenames must be unique: ${duplicateCacheNames.joinToString()}"
        }
        val retainedFileNames = sources.mapTo(mutableSetOf()) { it.request.key.fileName }
        requestsByTrackId = sources.associate { source ->
            source.trackId to source.request.copy(retainedFileNames = retainedFileNames)
        }
    }

    fun requestFor(trackId: TrackId): DeliveryRequest? = requestsByTrackId[trackId]
}
