package com.xwab.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import co.touchlab.kermit.Logger
import okio.Path

internal const val DATA_STORE_FILE_NAME = "xwab.preferences_pb"

internal fun createDataStore(producePath: () -> Path): DataStore<Preferences> = try {
    PreferenceDataStoreFactory.createWithPath(produceFile = producePath)
} catch (error: Throwable) {
    Logger.e(TAG, error) { "Failed to create the preferences DataStore." }
    throw error
}

private const val TAG = "PreferencesDataStore"
