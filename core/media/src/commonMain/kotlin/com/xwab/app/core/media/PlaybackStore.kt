package com.xwab.app.core.media

/**
 * Common, single-writer mailbox around the pure [reducePlayback] state machine.
 *
 * Both platform facades share this store instead of each re-implementing the
 * queue + reduce + publish loop. The store owns:
 *
 * - the serialized message mailbox and its re-entrancy guard,
 * - the internal [PlaybackState],
 * - the reducer invocation and effect ordering,
 * - the trigger to (re)publish state after every processed message.
 *
 * It does NOT own native APIs or state projection. After each message it calls
 * the two platform callbacks supplied by the facade.
 *
 * Processing is synchronous and single-threaded: a facade must only call
 * [dispatch] from its designated thread (the main thread on both platforms).
 * Moving to an asynchronous channel-based loop is a separate, test-gated change
 * because it would turn today's synchronous dispatch into an asynchronous one.
 */
internal class PlaybackStore(
    private val executeEffects: (List<PlaybackSideEffect>) -> Unit,
    private val onStateChanged: () -> Unit,
) {
    var state: PlaybackState = PlaybackState()
        private set

    private val mailbox = ArrayDeque<PlaybackMessage>()
    private var processing = false

    fun dispatch(message: PlaybackMessage) {
        mailbox.addLast(message)
        if (processing) return
        processing = true
        try {
            while (mailbox.isNotEmpty()) {
                val next = mailbox.removeFirst()
                val result = reducePlayback(state, next)
                state = result.state
                executeEffects(result.sideEffects)
                onStateChanged()
            }
        } finally {
            processing = false
        }
    }
}
