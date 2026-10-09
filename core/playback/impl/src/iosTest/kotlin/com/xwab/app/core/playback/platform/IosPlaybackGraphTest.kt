package com.xwab.app.core.playback.platform

import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * The production graph on the simulator. Metro checks the bindings when it compiles; this checks
 * what only running it can: the engine is built once, from parts Metro builds, and only on the
 * main thread.
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

    /** The facade's own guard still runs when Metro builds it, before any native part exists. */
    @Test
    fun theEngineIsNotBuiltOffTheMainThread() {
        val graph = createGraph<IosPlaybackGraph>()

        val failure = runBlocking(Dispatchers.Default) { runCatching { graph.engine }.exceptionOrNull() }

        assertIs<IllegalStateException>(failure)
    }
}
