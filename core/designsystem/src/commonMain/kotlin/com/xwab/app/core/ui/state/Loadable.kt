package com.xwab.app.core.ui.state

/** Distinguishes an asynchronous source's first emission from a real empty result. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data class Ready<T>(val value: T) : Loadable<T>
}
