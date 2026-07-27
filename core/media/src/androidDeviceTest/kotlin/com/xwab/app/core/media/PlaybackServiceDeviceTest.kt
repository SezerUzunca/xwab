package com.xwab.app.core.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
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

/** Exercises the real MediaController -> PlaybackService custom-command boundary. */
class PlaybackServiceDeviceTest {
    private lateinit var context: Context
    private lateinit var controller: MediaController
    private lateinit var client: SleepTimerClient

    @BeforeTest
    fun connect() {
        context = ApplicationProvider.getApplicationContext()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controller = MediaController.Builder(context, token)
            .buildAsync()
            .get(10, TimeUnit.SECONDS)
        client = SleepTimerClient(ContextCompat.getMainExecutor(context))
    }

    @AfterTest
    fun disconnect() {
        runCatching {
            controller.sendCustomCommand(
                SleepTimerProtocol.command(SleepTimerProtocol.ACTION_CANCEL),
                Bundle.EMPTY,
            ).get(5, TimeUnit.SECONDS)
        }
        controller.release()
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
        val result = controller.sendCustomCommand(
            SleepTimerProtocol.command(SleepTimerProtocol.ACTION_START),
            SleepTimerProtocol.startArguments(Long.MIN_VALUE),
        ).get(5, TimeUnit.SECONDS)

        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, result.resultCode)
    }

    @Test
    fun clientReportsCancelFailureInsteadOfSilentlyClearingState() {
        controller.release()
        val completed = CountDownLatch(1)
        var failed = false

        client.cancel(
            controller = controller,
            onSuccess = { completed.countDown() },
            onError = {
                failed = true
                completed.countDown()
            },
        )

        assertTrue(completed.await(10, TimeUnit.SECONDS), "Timed out waiting for IPC failure.")
        assertTrue(failed, "A released controller must report cancellation failure.")
    }

    private fun awaitClientResult(
        action: ((Long?) -> Unit, () -> Unit) -> Unit,
    ): Long? {
        val completed = CountDownLatch(1)
        var result: Long? = null
        var failed = false
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
        assertTrue(completed.await(10, TimeUnit.SECONDS), "Timed out waiting for timer IPC.")
        assertTrue(!failed, "Timer IPC returned an error.")
        return result
    }
}
