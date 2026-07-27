package com.xwab.app.core.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalMusicRepositoryTest {
    private val repository = LocalMusicRepository()

    @Test
    fun everyCategoryHasMatchingMusic() = runBlocking {
        repository.getCategories().first().forEach { category ->
            val music = repository.getMusicForCategory(category.id).first()
            assertTrue(music.isNotEmpty(), "${category.id} must contain audio")
            assertEquals(music.size, category.musicCount)
            assertTrue(music.all { it.categoryId == category.id })
        }
    }

    @Test
    fun everyTrackHasAnMp3() = runBlocking {
        repository.getAllMusic().first().forEach { music ->
            assertTrue(music.audioResource.endsWith(".mp3"))
            assertNotNull(repository.getMusic(music.id).first())
        }
    }
}
