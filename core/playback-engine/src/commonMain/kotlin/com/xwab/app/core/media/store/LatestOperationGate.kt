package com.xwab.app.core.media.store

/**
 * Invalidates completions from superseded native operations.
 *
 * Platform drivers begin a new generation whenever a load, pause, stop, seek,
 * or queue rebuild makes older asynchronous callbacks unsafe.
 */
internal class LatestOperationGate {
    private var currentVersion = 0L

    fun begin(): Long {
        currentVersion += 1L
        return currentVersion
    }

    fun isCurrent(version: Long): Boolean = version == currentVersion
}
