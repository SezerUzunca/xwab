package com.xwab.app.designsystem.state

/**
 * Distinguishes an asynchronous source's first emission from a real empty result.
 *
 * Screen state rather than a component, so this module is an odd home for it: reaching it is why
 * every feature's ViewModel and state class compiles against a Compose module at all. It is here
 * because the other homes are closed rather than because this one fits — `core` publishes
 * capability ports and this is not one, `shared` is the app shell and a feature cannot depend on
 * it, and no feature may depend on another. That leaves the one module all five features already
 * share, or a new module of its own, which is worth creating the day this type has company.
 */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data class Ready<T>(val value: T) : Loadable<T>
}
