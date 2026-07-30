package com.xwab.app.core.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Keeps bundled catalog entries and their physical MP3 files in the same module and verifies the
 * relationship during host tests.
 */
class BundledAudioAssetsTest {
    @Test
    fun everyBundledEntryResolvesToANonEmptyAudioFile() {
        val resourceRoot = assertNotNull(
            RESOURCE_ROOT,
            "Could not locate $RESOURCE_PATH from the repository root.",
        )

        catalogEntries.forEach { entry ->
            val bundled = entry.asset as? AudioAssetSpec.Bundled ?: return@forEach
            val audio = File(resourceRoot, bundled.resourcePath)
            assertTrue(audio.isFile, "${entry.music.id} points at a missing file: ${bundled.resourcePath}")
            assertTrue(audio.length() > 0L, "${entry.music.id} points at an empty file: ${bundled.resourcePath}")
        }
    }

    private companion object {
        private const val RESOURCE_PATH = "core/data/src/commonMain/composeResources"

        private val RESOURCE_ROOT: File? = sequenceOf(File(".").absoluteFile, compiledClassLocation())
            .filterNotNull()
            .flatMap { start -> generateSequence(start) { it.parentFile } }
            .filter { File(it, "settings.gradle.kts").isFile }
            .map { File(it, RESOURCE_PATH) }
            .firstOrNull { it.isDirectory }

        private fun compiledClassLocation(): File? = runCatching {
            File(BundledAudioAssetsTest::class.java.protectionDomain.codeSource.location.toURI()).absoluteFile
        }.getOrNull()
    }
}
