package com.xwab.app.core.data.repository

import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The common tests can only check the *shape* of `Music.audioResource`; resolving one needs a
 * platform resource loader they do not have. This host test closes that gap by resolving every
 * catalog entry against the audio files on disk, so a renamed or deleted mp3 fails the build
 * instead of the player.
 *
 * It reaches into `core:playback`, which owns the files while this module owns the catalog that
 * names them — the coupling `Music.audioResource` creates. That coupling is accepted knowingly
 * (PLAYBACK_COORDINATOR_PLAN.md §4) because every way of removing it splits catalog and files
 * across two places that nothing keeps in sync. This test is what makes the coupling safe, so
 * it has to reach.
 *
 * What it does NOT cover: that `Res.getUri` resolves the path, that the file is packaged into
 * the APK/IPA, that the codec plays, or anything iOS-side. Those need the app itself.
 */
class BundledAudioAssetsTest {
    @Test
    fun everyCatalogEntryResolvesToABundledAudioFile() = runBlocking {
        val resourceRoot = assertNotNull(RESOURCE_ROOT, "Could not locate $RESOURCE_PATH from the repository root.")

        BundledMusicCatalogRepository().observeAllMusic().first().forEach { music ->
            val audio = File(resourceRoot, music.audioResource)
            assertTrue(audio.isFile, "${music.id} points at a missing file: ${music.audioResource}")
            assertTrue(audio.length() > 0, "${music.id} points at an empty file: ${music.audioResource}")
        }
    }

    private companion object {
        private const val RESOURCE_PATH = "core/playback/src/commonMain/composeResources"

        /**
         * Gradle, the IDE and CI each pick their own working directory, so the repository is
         * located from the compiled test class as well and the first candidate that resolves wins.
         */
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
