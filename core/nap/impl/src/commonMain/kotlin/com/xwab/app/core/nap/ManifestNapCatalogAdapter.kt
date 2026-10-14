package com.xwab.app.core.nap

import com.xwab.app.core.nap.port.Nap
import com.xwab.app.core.nap.port.NapId
import com.xwab.app.core.nap.port.NapPort
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Serves nap metadata from one immutable manifest. */
@SingleIn(NapScope::class)
@Inject
internal class ManifestNapCatalogAdapter internal constructor(naps: List<Nap>) : NapPort {
    init {
        val duplicates = naps.groupBy(Nap::id).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Nap ids must be unique: ${duplicates.joinToString { it.value }}" }
    }

    private val allNaps = flowOf(naps)

    override fun observeNaps(): Flow<List<Nap>> = allNaps
    override fun observeNap(napId: NapId): Flow<Nap?> = allNaps.map { values -> values.find { it.id == napId } }
}
