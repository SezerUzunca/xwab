package com.xwab.app.feature.rehearsal

import com.xwab.app.core.rehearsal.port.REHEARSAL_PLAYBACK_KIND
import com.xwab.app.core.rehearsal.port.REHEARSAL_SCRIPT_MAX_LENGTH
import com.xwab.app.core.rehearsal.port.RehearsalPort
import com.xwab.app.core.rehearsal.port.RehearsalSaveResult
import com.xwab.app.core.rehearsal.port.RehearsalScript
import com.xwab.app.core.rehearsal.port.RehearsalScriptId
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.testing.FakePlaybackPort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RehearsalViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** What was kept last time fills the field; there is nothing new to save until it is edited. */
    @Test
    fun theKeptScriptFillsTheField() = runTest(dispatcher) {
        val model = RehearsalViewModel(FakeRehearsal(stored = "I walk home."), FakePlaybackPort())
        collectState(model)
        advanceUntilIdle()

        assertEquals("I walk home.", ready(model).draft)
        assertFalse(ready(model).canSave)
        assertTrue(ready(model).canPlay)
    }

    /**
     * The field's value is set in the keystroke's own call. Coming back a frame later through the
     * screen state, it moved the cursor and scrambled typing on a device ("thbeach", "sand.e").
     */
    @Test
    fun aKeystrokeReachesTheFieldAtOnce() = runTest(dispatcher) {
        val model = RehearsalViewModel(FakeRehearsal(stored = "I walk home."), FakePlaybackPort())
        collectState(model)
        advanceUntilIdle()

        model.onDraftChange("I walk home")
        assertEquals("I walk home", model.draft.value, "no dispatch between the keystroke and the field")

        advanceUntilIdle()
        assertEquals("I walk home", model.draft.value, "the kept script does not overwrite typing")
    }

    @Test
    fun anEditIsSavedAndClearedAndTheFieldIsCapped() = runTest(dispatcher) {
        val rehearsal = FakeRehearsal()
        val model = RehearsalViewModel(rehearsal, FakePlaybackPort())
        collectState(model)
        advanceUntilIdle()
        assertFalse(ready(model).canPlay, "nothing to hear yet")

        model.onDraftChange("  The wave sets me down.  ")
        advanceUntilIdle()
        assertTrue(ready(model).canSave)
        model.save()
        advanceUntilIdle()
        assertEquals("The wave sets me down.", rehearsal.script.value?.text)
        assertEquals("The wave sets me down.", ready(model).draft, "the field follows what was kept")
        assertEquals(SaveStatus.Saved, ready(model).saveStatus)

        model.clear()
        advanceUntilIdle()
        assertNull(rehearsal.script.value)
        assertEquals("", ready(model).draft)

        model.onDraftChange("a".repeat(REHEARSAL_SCRIPT_MAX_LENGTH + 10))
        advanceUntilIdle()
        assertEquals(REHEARSAL_SCRIPT_MAX_LENGTH, ready(model).draft.length)
    }

    @Test
    fun aRefusedSaveKeepsWhatWasTypedAndPlaysNothing() = runTest(dispatcher) {
        val playback = FakePlaybackPort()
        val model = RehearsalViewModel(FakeRehearsal(refuse = true), playback)
        collectState(model)
        advanceUntilIdle()

        model.onDraftChange("I walk home.")
        advanceUntilIdle()
        model.togglePlayback()
        advanceUntilIdle()

        assertEquals("I walk home.", ready(model).draft)
        assertEquals(SaveStatus.Failed, ready(model).saveStatus)
        assertNull(playback.playedItemId)
    }

    /** Play keeps the words on the screen first, then plays that wording: never an older one. */
    @Test
    fun playKeepsTheTypedWordsAndPlaysThatWording() = runTest(dispatcher) {
        val rehearsal = FakeRehearsal(stored = "An older wording.")
        val playback = FakePlaybackPort()
        val model = RehearsalViewModel(rehearsal, playback)
        collectState(model)
        advanceUntilIdle()

        model.onDraftChange("I walk home.")
        advanceUntilIdle()
        model.togglePlayback()
        advanceUntilIdle()

        assertEquals("I walk home.", rehearsal.script.value?.text)
        assertEquals(PlaybackItemId(REHEARSAL_PLAYBACK_KIND, idOf("I walk home.").value), playback.playedItemId)
    }

    @Test
    fun theKeptWordingPlaysThenPausesFromItsOwnControl() = runTest(dispatcher) {
        val playback = FakePlaybackPort()
        val model = RehearsalViewModel(FakeRehearsal(stored = "I walk home."), playback)
        collectState(model)
        advanceUntilIdle()

        model.togglePlayback()
        advanceUntilIdle()
        assertEquals(ITEM, playback.playedItemId)

        playback.publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true))
        advanceUntilIdle()
        assertTrue(ready(model).isPlaying)
        model.togglePlayback()
        assertEquals(1, playback.pauses)
    }

    /** An earlier wording the session still holds, and its failure, are not this screen's. */
    @Test
    fun onlyTheKeptWordingsPlaybackShows() = runTest(dispatcher) {
        val older = PlaybackItemId(REHEARSAL_PLAYBACK_KIND, idOf("An older wording.").value)
        val playback = FakePlaybackPort().apply { publish(PlaybackSummary(requestedItemId = older, playIntent = true)) }
        val model = RehearsalViewModel(FakeRehearsal(stored = "I walk home."), playback)
        collectState(model)
        advanceUntilIdle()
        assertFalse(ready(model).isPlaying)

        playback.publish(PlaybackSummary(failure = PlaybackFailure.ItemNotFound(older)))
        advanceUntilIdle()
        assertNull(ready(model).playbackFailure)

        playback.publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true, isPreparing = true))
        advanceUntilIdle()
        assertTrue(ready(model).isPreparing)

        playback.publish(PlaybackSummary(failure = PlaybackFailure.SourceUnavailable(ITEM)))
        advanceUntilIdle()
        assertIs<PlaybackFailure.SourceUnavailable>(ready(model).playbackFailure)
    }

    /** Clearing a playing script stops it: it is the thing being thrown away. */
    @Test
    fun clearingAPlayingScriptPausesIt() = runTest(dispatcher) {
        val playback = FakePlaybackPort().apply { publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true)) }
        val model = RehearsalViewModel(FakeRehearsal(stored = "I walk home."), playback)
        collectState(model)
        advanceUntilIdle()

        model.clear()
        advanceUntilIdle()

        assertEquals(1, playback.pauses)
    }

    private fun TestScope.collectState(model: RehearsalViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
    }

    private fun ready(model: RehearsalViewModel) = assertIs<RehearsalUiState.Ready>(model.state.value).value

    private companion object {
        val ITEM = PlaybackItemId(REHEARSAL_PLAYBACK_KIND, idOf("I walk home.").value)
    }
}

/** The core names a wording by a hash; any id that follows the text stands in for it here. */
private fun idOf(text: String) = RehearsalScriptId("id-${text.hashCode()}")

/** One feature reads this port, so its fake lives with it rather than in `testing`. */
private class FakeRehearsal(stored: String = "", private val refuse: Boolean = false) : RehearsalPort {
    val script = MutableStateFlow(stored.takeIf { it.isNotBlank() }?.let { RehearsalScript(idOf(it), it) })

    override fun observeScript(): Flow<RehearsalScript?> = script

    override suspend fun save(text: String): RehearsalSaveResult {
        if (refuse) return RehearsalSaveResult.Unavailable
        val trimmed = text.trim()
        script.value = trimmed.takeIf { it.isNotEmpty() }?.let { RehearsalScript(idOf(it), it) }
        return RehearsalSaveResult.Saved
    }
}
