package com.xwab.app.feature.aftershift

import com.xwab.app.core.guide.port.GUIDE_PLAYBACK_KIND
import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.guide.port.GuideUse
import com.xwab.app.core.guide.port.GuidePort
import com.xwab.app.core.notes.port.NoteSaveResult
import com.xwab.app.core.notes.port.NotesPort
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.feature.aftershift.domain.ObserveAfterShiftContentUseCase
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
import kotlinx.coroutines.flow.flowOf
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
class AfterShiftViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aGuidePlaysThenPausesFromItsOwnControl() = runTest(dispatcher) {
        val port = FakePlaybackPort()
        val model = model(port)
        collectState(model)
        advanceUntilIdle()

        model.togglePlayback(RELAX)
        advanceUntilIdle()
        assertEquals(PlaybackItemId(GUIDE_PLAYBACK_KIND, "relax"), port.playedItemId)

        port.publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true))
        advanceUntilIdle()
        assertTrue(ready(model).isGuidePlaying(RELAX))
        assertFalse(ready(model).isGuidePlaying(BEACH))
        model.togglePlayback(RELAX)
        assertEquals(1, port.pauses)
    }

    /** The first play speaks the whole guide; the guide says so, and only its own failure shows. */
    @Test
    fun aPreparingGuideSaysSoAndShowsOnlyItsOwnFailure() = runTest(dispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true, isPreparing = true))
        }
        val model = model(port)
        collectState(model)
        advanceUntilIdle()
        assertTrue(ready(model).isGuidePreparing(RELAX))

        port.publish(PlaybackSummary(failure = PlaybackFailure.SourceUnavailable(ITEM)))
        advanceUntilIdle()
        assertIs<PlaybackFailure.SourceUnavailable>(ready(model).guideFailure(RELAX))
        assertNull(ready(model).guideFailure(BEACH))

        port.publish(PlaybackSummary(failure = PlaybackFailure.EngineFailed(PlaybackItemId("sound", "relax"))))
        advanceUntilIdle()
        assertNull(ready(model).guideFailure(RELAX), "a sound's failure is not this guide's")
    }

    /** A timer set beside the guides is for winding down: it starts the first guide when none plays. */
    @Test
    fun startingTheTimerPlaysTheFirstGuideWhenNoneIsPlaying() = runTest(dispatcher) {
        val port = FakePlaybackPort()
        val model = model(port)
        collectState(model)
        advanceUntilIdle()

        model.startSleepTimer(0L)
        assertNull(port.startedTimerMs, "an invalid duration starts nothing")
        model.startSleepTimer(1_800_000L)
        advanceUntilIdle()

        assertEquals(1_800_000L, port.startedTimerMs)
        assertEquals(ITEM, port.playedItemId)
        model.cancelSleepTimer()
        assertEquals(1, port.cancelledTimers)
    }

    @Test
    fun restartingTheTimerWhileAGuidePlaysLeavesPlaybackAlone() = runTest(dispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = playbackItemOf(BEACH), playIntent = true))
        }
        val model = model(port)
        collectState(model)
        advanceUntilIdle()

        model.startSleepTimer(600_000L)
        advanceUntilIdle()

        assertNull(port.playedItemId)
    }

    /** What was parked last time fills the field; saving and clearing go to the device under one key. */
    @Test
    fun theParkedNoteIsReadSavedAndCleared() = runTest(dispatcher) {
        val notes = FakeNotes(stored = "Call the bank")
        val model = model(FakePlaybackPort(), notes)
        advanceUntilIdle()
        assertEquals(ParkedNote(draft = "Call the bank", saved = "Call the bank"), model.note.value)
        assertFalse(model.note.value.canSave, "nothing new to save")

        model.onNoteChange("Call the bank\nBuy milk  ")
        assertTrue(model.note.value.canSave)
        model.saveNote()
        advanceUntilIdle()
        assertEquals("Call the bank\nBuy milk", notes.text.value, "saved without trailing space")
        assertEquals(NoteStatus.Saved, model.note.value.status)

        model.clearNote()
        advanceUntilIdle()
        assertEquals("", notes.text.value)
        assertEquals(ParkedNote(draft = "", saved = "", status = NoteStatus.Saved), model.note.value)
    }

    /** Typing is never overwritten by the stored note arriving late, and a refused save keeps the text. */
    @Test
    fun aRefusedSaveKeepsWhatWasTyped() = runTest(dispatcher) {
        val notes = FakeNotes(stored = "", refuse = true)
        val model = model(FakePlaybackPort(), notes)
        model.onNoteChange("Pick up groceries")
        advanceUntilIdle()

        model.saveNote()
        advanceUntilIdle()

        assertEquals("Pick up groceries", model.note.value.draft)
        assertEquals(NoteStatus.Failed, model.note.value.status)
        assertTrue(model.note.value.canSave, "still something to save")
    }

    /** A guide for out of bed belongs to the screen for lying awake; the timer never starts it here. */
    @Test
    fun onlyWindDownGuidesAreListed() = runTest(dispatcher) {
        val port = FakePlaybackPort()
        val guides = FakeGuides(listOf(guide("awake", GuideUse.OutOfBed), guide("relax")))
        val model = AfterShiftViewModel(ObserveAfterShiftContentUseCase(guides, port), port, FakeNotes())
        collectState(model)
        advanceUntilIdle()

        assertEquals(listOf(RELAX), ready(model).guides.map { it.id })
        model.startSleepTimer(600_000L)
        advanceUntilIdle()
        assertEquals(ITEM, port.playedItemId)
    }

    /** The key is a name on disk: changing it strands every parked note. */
    @Test
    fun theNoteKeepsItsKey() {
        assertEquals("after-shift", AFTER_SHIFT_NOTE)
    }

    private fun model(port: FakePlaybackPort, notes: FakeNotes = FakeNotes()) = AfterShiftViewModel(
        ObserveAfterShiftContentUseCase(FakeGuides(listOf(guide("relax"), guide("beach"))), port),
        port,
        notes,
    )

    private fun TestScope.collectState(model: AfterShiftViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
    }

    private fun ready(model: AfterShiftViewModel) = assertIs<AfterShiftUiState.Ready>(model.state.value).value

    private companion object {
        val RELAX = GuideId("relax")
        val BEACH = GuideId("beach")
        val ITEM = PlaybackItemId(GUIDE_PLAYBACK_KIND, "relax")
    }
}

private fun guide(id: String, use: GuideUse = GuideUse.WindDown) =
    Guide(GuideId(id), id, "A test guide.", minutes = 5, languageTag = "en-US", use = use)

/** One feature reads these ports, so their fakes live with it rather than in `testing`. */
private class FakeGuides(private val guides: List<Guide>) : GuidePort {
    override fun observeGuides(): Flow<List<Guide>> = flowOf(guides)
    override fun observeGuide(guideId: GuideId): Flow<Guide?> = flowOf(guides.find { it.id == guideId })
}

private class FakeNotes(stored: String = "", private val refuse: Boolean = false) : NotesPort {
    val text = MutableStateFlow(stored)

    override fun observe(key: String): Flow<String> {
        assertEquals(AFTER_SHIFT_NOTE, key)
        return text
    }

    override suspend fun save(key: String, text: String): NoteSaveResult {
        assertEquals(AFTER_SHIFT_NOTE, key)
        if (refuse) return NoteSaveResult.Unavailable
        this.text.value = text
        return NoteSaveResult.Saved
    }
}
