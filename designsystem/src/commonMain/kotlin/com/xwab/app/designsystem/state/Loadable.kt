package com.xwab.app.designsystem.state

/** Shared UI state distinguishing the first emission from loaded, possibly empty content. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data class Ready<T>(val value: T) : Loadable<T>
}
