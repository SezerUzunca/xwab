package com.xwab.app.core.playback.platform

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import dev.zacsweers.metro.createGraphFactory
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * The production graph on a device. Metro checks the bindings when it compiles; this checks what
 * only running it can: the engine is built once, from parts Metro builds, and only on the main
 * thread.
 */
class AndroidPlaybackGraphTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theModuleGraphBuildsOneEngineOnTheMainThread() {
        val graph = createGraphFactory<AndroidPlaybackGraph.Factory>().create(context)

        onMainThread {
            val engine = graph.engine
            try {
                assertIs<AndroidPlaybackFacade>(engine)
                assertSame(engine, graph.engine)
            } finally {
                engine.release()
            }
        }
    }

    /** The facade's own guard still runs when Metro builds it, after its injected parts. */
    @Test
    fun theEngineIsNotBuiltOffTheMainThread() {
        val graph = createGraphFactory<AndroidPlaybackGraph.Factory>().create(context)

        assertFailsWith<IllegalStateException> { graph.engine }
    }

    /** Runs [block] on the main thread and rethrows what it threw. */
    private fun onMainThread(block: () -> Unit) {
        val task = FutureTask(block)
        ContextCompat.getMainExecutor(context).execute(task)
        try {
            task.get(MAIN_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (error: ExecutionException) {
            throw error.cause ?: error
        }
    }

    private companion object {
        /**
         * Ten seconds timed out on the two-core CI emulator, where the main thread can stay busy
         * that long. It failed both attempts of the device job on main twice. The test asserts
         * what the graph builds, not how fast, so it waits as long as PlaybackServiceDeviceTest's
         * controller connection, which hit the same limit first.
         */
        const val MAIN_THREAD_TIMEOUT_SECONDS = 45L
    }
}
