package com.xwab.app

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * What an Activity or view controller gives the app root, as one owner a test can close.
 *
 * Create and close it on the main thread (`runOnIdle`): its lifecycle state changes enforce that.
 *
 * The shell's navigation tests declare the same owner: tests cannot share sources across modules,
 * and the integration scenario needs one here, where the real graph is built.
 */
internal class TestRootOwner : ViewModelStoreOwner, SavedStateRegistryOwner {
    override val viewModelStore = ViewModelStore()
    override val lifecycle = LifecycleRegistry.createUnsafe(this)
    private val controller = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = controller.savedStateRegistry

    init {
        controller.performAttach()
        controller.performRestore(null)
        // As ComponentActivity does. Entries must not lean on the root's handles to keep their own.
        enableSavedStateHandles()
        lifecycle.currentState = Lifecycle.State.RESUMED
    }

    fun close() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
