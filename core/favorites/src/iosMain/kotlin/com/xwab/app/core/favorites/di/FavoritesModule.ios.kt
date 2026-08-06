@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.favorites.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.xwab.app.core.favorites.DATA_STORE_FILE_NAME
import com.xwab.app.core.favorites.createDataStore
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual val favoritesPlatformModule: Module = module {
    single<DataStore<Preferences>> {
        createDataStore(
            producePath = {
                val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
                (requireNotNull(documentDirectory).path + "/$DATA_STORE_FILE_NAME").toPath()
            },
        )
    }
}
