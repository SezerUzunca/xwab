package com.xwab.app.core.sources

import com.xwab.app.core.sources.port.ContentSource
import com.xwab.app.core.sources.port.SOUND_NAMESPACE
import com.xwab.app.core.sources.port.SourcePort
import com.xwab.app.core.sources.port.STORY_NAMESPACE
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

internal data class ManifestSource(
    val itemId: String,
    val source: ContentSource,
)

/** Resolves the two immutable source manifests without knowing either content module's types. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class ManifestSourceAdapter internal constructor(
    manifests: Map<String, List<ManifestSource>>,
) : SourcePort {
    @Inject
    internal constructor() : this(
        mapOf(
            SOUND_NAMESPACE to soundSourceManifest,
            STORY_NAMESPACE to storySourceManifest,
        ),
    )

    private val sourcesByNamespace = manifests.mapValues { (namespace, entries) ->
        val duplicateIds = entries.groupBy(ManifestSource::itemId).filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) {
            "$namespace source ids must be unique: ${duplicateIds.joinToString()}"
        }
        entries.associate { it.itemId to it.source }
    }

    override fun sourceFor(namespace: String, itemId: String): ContentSource? =
        sourcesByNamespace[namespace]?.get(itemId)

    override fun cacheFileNames(namespace: String): Set<String> =
        sourcesByNamespace[namespace].orEmpty().values.mapNotNullTo(mutableSetOf()) {
            it.cacheFileName
        }
}
