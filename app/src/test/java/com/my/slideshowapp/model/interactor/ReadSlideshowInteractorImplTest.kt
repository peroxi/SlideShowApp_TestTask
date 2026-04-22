package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.entity.PlaylistItem
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadSlideshowInteractorImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun manifestOf(vararg items: PlaylistItem): String =
        json.encodeToString(items.toList())

    @Test
    fun `invoke returns empty list when no manifest exists`() = runBlocking {
        val storage = FakeFileStorage()
        val impl = ReadSlideshowInteractorImpl(storage)

        val result = impl()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke reads items from manifest and maps to MediaItems`() = runBlocking {
        val storage = FakeFileStorage()
        storage.saveFile(
            "manifest.json",
            manifestOf(
                PlaylistItem(creativeKey = "a.jpg", duration = 5),
                PlaylistItem(creativeKey = "b.mp4", duration = 8)
            ).toByteArray()
        )
        val impl = ReadSlideshowInteractorImpl(storage)

        val result = impl()

        assertEquals(2, result.size)
        assertEquals("a.jpg", result[0].creativeKey)
        assertEquals("/fake/media_a.jpg", result[0].filePath)
        assertEquals(5, result[0].duration)
        assertEquals("b.mp4", result[1].creativeKey)
        assertEquals(8, result[1].duration)
    }

    @Test
    fun `invoke skips items with null creativeKey`() = runBlocking {
        val storage = FakeFileStorage()
        storage.saveFile(
            "manifest.json",
            manifestOf(
                PlaylistItem(creativeKey = null, duration = 5),
                PlaylistItem(creativeKey = "valid.jpg", duration = 3)
            ).toByteArray()
        )
        val impl = ReadSlideshowInteractorImpl(storage)

        val result = impl()

        assertEquals(1, result.size)
        assertEquals("valid.jpg", result[0].creativeKey)
    }

    @Test
    fun `invoke uses default duration of 5 when item duration is null`() = runBlocking {
        val storage = FakeFileStorage()
        storage.saveFile(
            "manifest.json",
            manifestOf(PlaylistItem(creativeKey = "x.jpg", duration = null)).toByteArray()
        )
        val impl = ReadSlideshowInteractorImpl(storage)

        val result = impl()

        assertEquals(5, result[0].duration)
    }

    @Test
    fun `invoke returns empty list when manifest is malformed`() = runBlocking {
        val storage = FakeFileStorage()
        storage.saveFile("manifest.json", "not valid json at all".toByteArray())
        val impl = ReadSlideshowInteractorImpl(storage)

        val result = impl()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke returns empty list when manifest contains empty array`() = runBlocking {
        val storage = FakeFileStorage()
        storage.saveFile("manifest.json", "[]".toByteArray())
        val impl = ReadSlideshowInteractorImpl(storage)

        val result = impl()

        assertTrue(result.isEmpty())
    }
}

