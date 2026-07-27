package com.xwab.app.core.preferences.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.xwab.app.core.preferences.DATA_STORE_FILE_NAME
import com.xwab.app.core.preferences.createDataStore
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val dataStoreModule: Module = module {
    single<DataStore<Preferences>> {
        val context = androidContext()
        createDataStore(
            producePath = {
                context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath()
            },
        )
    }
}
