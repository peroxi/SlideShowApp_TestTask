package com.my.slideshowapp.model.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SlideshowRepositoryImplTest {

    @Test
    fun `fetchCreative returns stream with correct bytes from api response body`() = runBlocking {
        val expectedBytes = byteArrayOf(1, 2, 3, 4, 5)
        val fakeApi = FakeApiService(creativeBytes = expectedBytes)
        val repository = SlideshowRepositoryImpl(fakeApi)

        val result = repository.fetchCreative("some-key").readBytes()

        assertEquals(expectedBytes.toList(), result.toList())
    }

    @Test
    fun `fetchCreative passes correct creativeKey to api`() = runBlocking {
        val fakeApi = FakeApiService(creativeBytes = ByteArray(0))
        val repository = SlideshowRepositoryImpl(fakeApi)

        repository.fetchCreative("creative-123")

        assertEquals("creative-123", fakeApi.lastRequestedCreativeKey)
    }

    @Test
    fun `fetchCreative returns empty stream when api returns empty body`() = runBlocking {
        val fakeApi = FakeApiService(creativeBytes = ByteArray(0))
        val repository = SlideshowRepositoryImpl(fakeApi)

        val result = repository.fetchCreative("key").readBytes()

        assertEquals(0, result.size)
    }

    @Test
    fun `fetchCreative propagates exception from api`() = runBlocking {
        val fakeApi = FakeApiService(throwOnCreative = RuntimeException("Download failed"))
        val repository = SlideshowRepositoryImpl(fakeApi)

        val exception = runCatching { repository.fetchCreative("key") }.exceptionOrNull()

        assertEquals("Download failed", exception?.message)
    }
}
