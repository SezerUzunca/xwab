package com.xwab.app.core.playback.platform

import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * The production graph on the simulator. Metro checks the bindings when it compiles; this checks
 * what only running it can: the engine and the native parts Metro builds for it come up, once.
 */
class IosPlaybackGraphTest {
    @Test
    fun theModuleGraphBuildsOneEngine() {
        val graph = createGraph<IosPlaybackGraph>()

        val engine = graph.engine
        try {
            assertIs<IosPlaybackFacade>(engine)
            assertSame(engine, graph.engine)
        } finally {
            engine.release()
        }
    }
}
