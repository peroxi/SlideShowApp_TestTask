package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.entity.PlaylistItem
import com.my.slideshowapp.model.repository.SlideshowRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream

class SlideshowInteractorImplTest {

    private fun interactor(
        useCaseItems: List<PlaylistItem> = emptyList(),
        useCaseThrow: Throwable? = null,
        bytesPerKey: Map<String, ByteArray> = emptyMap(),
        repoThrow: Throwable? = null,
        preloadedFiles: Map<String, ByteArray> = emptyMap()
    ): Triple<SlideshowInteractorImpl, FakeSlideshowRepository, FakeFileStorage> {
        val useCase = FakeFetchPlaylistFetchUseCase(useCaseItems, useCaseThrow)
        val repo = FakeSlideshowRepository(bytesPerKey, repoThrow)
        val storage = FakeFileStorage().also { fs ->
            preloadedFiles.forEach { (name, data) -> fs.saveFile(name, data) }
        }
        return Triple(SlideshowInteractorImpl(useCase, repo, storage), repo, storage)
    }

    @Test
    fun `invoke returns empty list when use case returns no items`() = runBlocking {
        val (impl) = interactor()

        val result = impl()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke downloads file when it does not exist in storage`() = runBlocking {
        val items = listOf(PlaylistItem(creativeKey = "img.jpg", duration = 5))
        val bytes = byteArrayOf(1, 2, 3)
        val (impl, repo, storage) = interactor(
            useCaseItems = items,
            bytesPerKey = mapOf("img.jpg" to bytes)
        )

        impl()

        assertTrue(storage.fileExists("media_img.jpg"))
        assertEquals(bytes.toList(), storage.savedFiles["media_img.jpg"]?.toList())
        assertTrue(repo.fetchedKeys.contains("img.jpg"))
    }

    @Test
    fun `invoke skips download when file already exists in storage`() = runBlocking {
        val items = listOf(PlaylistItem(creativeKey = "img.jpg", duration = 5))
        val existingBytes = byteArrayOf(9, 8, 7)
        val (impl, repo) = interactor(
            useCaseItems = items,
            preloadedFiles = mapOf("media_img.jpg" to existingBytes)
        )

        impl()

        assertFalse(repo.fetchedKeys.contains("img.jpg"))
    }

    @Test
    fun `invoke returns MediaItems with correct filePaths and durations`() = runBlocking {
        val items = listOf(
            PlaylistItem(creativeKey = "a.jpg", duration = 5),
            PlaylistItem(creativeKey = "b.mp4", duration = 10)
        )
        val (impl) = interactor(useCaseItems = items)

        val result = impl()

        assertEquals(2, result.size)
        assertEquals("/fake/media_a.jpg", result[0].filePath)
        assertEquals(5, result[0].duration)
        assertEquals("a.jpg", result[0].creativeKey)
        assertEquals("/fake/media_b.mp4", result[1].filePath)
        assertEquals(10, result[1].duration)
    }

    @Test
    fun `invoke skips items with null creativeKey`() = runBlocking {
        val items = listOf(
            PlaylistItem(creativeKey = null, duration = 5),
            PlaylistItem(creativeKey = "valid.jpg", duration = 3)
        )
        val (impl) = interactor(useCaseItems = items)

        val result = impl()

        assertEquals(1, result.size)
        assertEquals("valid.jpg", result[0].creativeKey)
    }

    @Test
    fun `invoke saves manifest after downloading files`() = runBlocking {
        val items = listOf(PlaylistItem(creativeKey = "x.jpg", duration = 5))
        val (impl, _, storage) = interactor(useCaseItems = items)

        impl()

        assertTrue(storage.fileExists("manifest.json"))
    }

    @Test
    fun `invoke does not overwrite manifest when keys are unchanged`() = runBlocking {
        val items = listOf(PlaylistItem(creativeKey = "x.jpg", duration = 5))
        val (impl, _, storage) = interactor(useCaseItems = items)
        impl() // first call — saves manifest
        val manifestAfterFirst = storage.readFile("manifest.json")

        impl() // second call — keys unchanged, manifest should not change
        val manifestAfterSecond = storage.readFile("manifest.json")

        assertEquals(manifestAfterFirst, manifestAfterSecond)
    }

    @Test
    fun `invoke aborts and returns empty list when file fails after retry`() = runBlocking {
        val items = listOf(
            PlaylistItem(creativeKey = "bad.jpg", duration = 5),
            PlaylistItem(creativeKey = "good.jpg", duration = 3)
        )
        // "bad.jpg" always throws, "good.jpg" always succeeds
        val throwingRepo = object : SlideshowRepository {
            override suspend fun fetchCreative(creativeKey: String): InputStream {
                if (creativeKey == "bad.jpg") throw RuntimeException("fail")
                return InputStream.nullInputStream()
            }
        }
        val storage = FakeFileStorage()
        val useCase = FakeFetchPlaylistFetchUseCase(items)
        val impl = SlideshowInteractorImpl(useCase, throwingRepo, storage)

        val result = impl()

        // Entire update aborted — empty result, manifest not saved
        assertTrue(result.isEmpty())
        assertFalse(storage.fileExists("manifest.json"))
    }

    @Test
    fun `invoke succeeds when retry fixes failed file`() = runBlocking {
        val items = listOf(
            PlaylistItem(creativeKey = "flaky.jpg", duration = 5),
            PlaylistItem(creativeKey = "good.jpg", duration = 3)
        )
        var flakyCallCount = 0
        // "flaky.jpg" fails on first attempt, succeeds on retry
        val flakyRepo = object : SlideshowRepository {
            override suspend fun fetchCreative(creativeKey: String): InputStream{
                if (creativeKey == "flaky.jpg") {
                    flakyCallCount++
                    if (flakyCallCount == 1) throw RuntimeException("temporary fail")
                }
                return InputStream.nullInputStream()
            }
        }
        val storage = FakeFileStorage()
        val useCase = FakeFetchPlaylistFetchUseCase(items)
        val impl = SlideshowInteractorImpl(useCase, flakyRepo, storage)

        val result = impl()

        // Retry succeeded — both items returned, manifest saved
        assertEquals(2, result.size)
        assertTrue(storage.fileExists("manifest.json"))
    }
}

