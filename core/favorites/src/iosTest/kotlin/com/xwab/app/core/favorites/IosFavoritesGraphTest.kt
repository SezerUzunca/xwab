package com.xwab.app.core.favorites

import dev.zacsweers.metro.createDynamicGraph
import kotlin.test.Test
import kotlin.test.assertTrue
import platform.Foundation.NSFileManager

/** The iOS production graph, with its favorites file built by Metro from the system's file manager. */
class IosFavoritesGraphTest {
    @Test
    fun theModuleGraphSharesOneAdapterAndLeavesTheFileForItsFirstAccess() =
        checkTheModuleGraphSharesOneAdapterAndLeavesTheFileForItsFirstAccess(
            createDynamicGraph<IosFavoritesGraph>(UnresolvableFile),
        )

    @Test
    fun theFileLivesInTheDocumentsDirectory() {
        val path = IosFavoritesFile(NSFileManager.defaultManager).path().toString()

        assertTrue(path.endsWith("Documents/$DATA_STORE_FILE_NAME"), path)
    }
}
