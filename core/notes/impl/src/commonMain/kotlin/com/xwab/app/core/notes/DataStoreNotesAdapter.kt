package com.xwab.app.core.notes

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.xwab.app.core.notes.port.NoteSaveResult
import com.xwab.app.core.notes.port.NotesPort
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException

/**
 * Notes in a preferences file of their own. A note that cannot be read shows as empty and is
 * logged; one that cannot be written is reported, and the screen keeps the text so nothing typed is
 * lost.
 */
@SingleIn(NotesScope::class)
@Inject
internal class DataStoreNotesAdapter(
    private val dataStore: DataStore<Preferences>,
    logger: Logger,
) : NotesPort {
    private val logger = logger.withTag("DataStoreNotesAdapter")

    override fun observe(key: String): Flow<String> {
        val preferencesKey = keyOf(key)
        return dataStore.data
            .map { preferences -> preferences[preferencesKey].orEmpty() }
            .catch { error ->
                if (error !is IOException) throw error
                logger.w(error) { "Could not read the $key note." }
                emit("")
            }
            .distinctUntilChanged()
    }

    override suspend fun save(key: String, text: String): NoteSaveResult {
        val preferencesKey = keyOf(key)
        return try {
            dataStore.edit { preferences ->
                if (text.isBlank()) preferences.remove(preferencesKey) else preferences[preferencesKey] = text
            }
            NoteSaveResult.Saved
        } catch (error: IOException) {
            logger.e(error) { "Could not save the $key note." }
            NoteSaveResult.Unavailable
        }
    }

    private fun keyOf(key: String): Preferences.Key<String> {
        require(KEY.matches(key)) { "A note key is lower-case letters, digits and dashes: $key" }
        return stringPreferencesKey("note_$key")
    }

    private companion object {
        val KEY = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    }
}
