package com.xwab.app.core.speech

import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue
import platform.Foundation.NSFileManager

/**
 * The iOS production graph. It is built here; it does not speak, because whether the simulator's
 * synthesizer writes audio is not something CI can rely on — the same reason no iOS test here plays
 * media.
 */
class IosSpeechGraphTest {
    @Test
    fun theModuleGraphHandsOutOneRenderer() {
        val graph = createGraph<IosSpeechGraph>()

        assertSame(graph.speech, graph.speech)
    }

    @Test
    fun speechLivesInTheCachesDirectory() {
        val root = IosSpeechCacheLocation(NSFileManager.defaultManager).root().toString()

        assertTrue(root.endsWith("Caches/speech"), root)
    }
}
