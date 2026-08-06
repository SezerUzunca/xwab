package com.xwab.app.core.playbackengine.store

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LatestOperationGateTest {

    @Test
    fun onlyLatestOperationCompletionIsAccepted() {
        val gate = LatestOperationGate()
        val oldOperation = gate.begin()
        val latestOperation = gate.begin()

        assertFalse(gate.isCurrent(oldOperation))
        assertTrue(gate.isCurrent(latestOperation))
    }
}
