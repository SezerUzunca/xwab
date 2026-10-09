package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import okio.Path

internal const val DATA_STORE_FILE_NAME = "xwab.preferences_pb"

/**
 * Where the platform keeps the favorites file. Each platform's graph binds its own. DataStore asks
 * on first access, off the main thread, so building the graph touches no file system.
 */
internal fun interface FavoritesFile {
    fun path(): Path
}

internal fun createDataStore(scope: CoroutineScope, producePath: () -> Path): DataStore<Preferences> = try {
    PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = producePath)
} catch (error: Throwable) {
    Logger.e(error, tag = TAG) { "Failed to create the preferences DataStore." }
    throw error
}

private const val TAG = "PreferencesDataStore"
