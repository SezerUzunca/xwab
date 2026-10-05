package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import okio.Path

internal const val DATA_STORE_FILE_NAME = "xwab.preferences_pb"

/**
 * Where the platform keeps the favorites file. Each platform's graph adapter supplies its own;
 * DataStore asks on first access, off the main thread, so building the adapter touches no file
 * system.
 */
internal fun interface FavoritesFile {
    fun path(): Path
}

/** DataStore resolves [producePath] on first access, off the main thread, not when it is created. */

internal fun createDataStore(producePath: () -> Path): DataStore<Preferences> = try {
    PreferenceDataStoreFactory.createWithPath(produceFile = producePath)
} catch (error: Throwable) {
    Logger.e(error, tag = TAG) { "Failed to create the preferences DataStore." }
    throw error
}

private const val TAG = "PreferencesDataStore"
