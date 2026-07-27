package com.xwab.app.core.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the real MediaController -> PlaybackService custom-command boundary.
 *
 * Media3 requires every [MediaController] call to happen on its application thread,
 * and [SleepTimerClient] is likewise always driven from the main thread in
 * production. Instrumentation tests run on their own thread, so every controller
 * and client interaction here is hopped onto the main thread via [onMainThread];
 * only the blocking waits stay on the test thread.
 */
class PlaybackServiceDeviceTest {
    private lateinit var context: Context
    private lateinit var controller: MediaController
    private lateinit var client: SleepTimerClient

    @BeforeTest
    fun connect() {
        context = ApplicationProvider.getApplicationContext()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controller = MediaController.Builder(context, token)
            // Pin the application thread instead of relying on "current or main".
            .setApplicationLooper(Looper.getMainLooper())
            .buildAsync()
            .get(10, TimeUnit.SECONDS)
        client = SleepTimerClient(ContextCompat.getMainExecutor(context))
    }

    @AfterTest
    fun disconnect() {
        // Teardown must not mask the actual test result, so failures are swallowed.
        runCatching { sendCustomCommand(SleepTimerProtocol.ACTION_CANCEL, Bundle.EMPTY) }
        runCatching { onMainThread { controller.release() } }
    }

    @Test
    fun sleepTimerClientRoundTripsStartRestoreAndCancelThroughService() {
        val deadline = SystemClock.elapsedRealtime() + 60_000L

        assertEquals(deadline, awaitClientResult { success, failure ->
            client.start(controller, deadline, success, failure)
        })
        assertEquals(deadline, awaitClientResult { success, failure ->
            client.restore(controller, success, failure)
        })
        assertNull(awaitClientResult { success, failure ->
            client.cancel(controller, success, failure)
        })
    }

    @Test
    fun serviceRejectsAnOverflowingRawDeadline() {
        val result = sendCustomCommand(
            action = SleepTimerProtocol.ACTION_START,
            arguments = SleepTimerProtocol.startArguments(Long.MIN_VALUE),
        )

        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, result.resultCode)
    }

    @Test
    fun clientReportsCancelFailureInsteadOfSilentlyClearingState() {
        onMainThread { controller.release() }
        val completed = CountDownLatch(1)
        var failed = false

        onMainThread {
            client.cancel(
                controller = controller,
                onSuccess = { completed.countDown() },
                onError = {
                    failed = true
                    completed.countDown()
                },
            )
        }

        assertTrue(completed.await(10, TimeUnit.SECONDS), "Timed out waiting for IPC failure.")
        assertTrue(failed, "A released controller must report cancellation failure.")
    }

    /** Issues a custom command from the application thread, then waits off it. */
    private fun sendCustomCommand(action: String, arguments: Bundle): SessionResult =
        onMainThread {
            controller.sendCustomCommand(SleepTimerProtocol.command(action), arguments)
        }.get(5, TimeUnit.SECONDS)

    private fun awaitClientResult(
        action: ((Long?) -> Unit, () -> Unit) -> Unit,
    ): Long? {
        val completed = CountDownLatch(1)
        var result: Long? = null
        var failed = false
        onMainThread {
            action(
                { deadline ->
                    result = deadline
                    completed.countDown()
                },
                {
                    failed = true
                    completed.countDown()
                },
            )
        }
        assertTrue(completed.await(10, TimeUnit.SECONDS), "Timed out waiting for timer IPC.")
        assertTrue(!failed, "Timer IPC returned an error.")
        return result
    }

    /**
     * Runs [block] on the main thread and returns its result. The latch publishes the
     * outcome back to the calling test thread.
     */
    private fun <T> onMainThread(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()

        val done = CountDownLatch(1)
        var outcome: Result<T>? = null
        ContextCompat.getMainExecutor(context).execute {
            outcome = runCatching(block)
            done.countDown()
        }
        assertTrue(done.await(10, TimeUnit.SECONDS), "Timed out waiting for the main thread.")
        return outcome!!.getOrThrow()
    }
}
