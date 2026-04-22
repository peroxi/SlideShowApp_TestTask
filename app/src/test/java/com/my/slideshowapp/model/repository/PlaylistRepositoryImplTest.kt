package com.my.slideshowapp.model.repository

import com.my.slideshowapp.model.entity.PlaylistItemsResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistRepositoryImplTest {

    @Test
    fun `fetchPlaylistItems passes screenKey to api and returns response`() = runBlocking {
        val expected = PlaylistItemsResponse(screenKey = "test-key")
        val fakeApi = FakeApiService(playlistResponse = expected)
        val screenKeyRepository = FakeScreenKeyRepository("test-key")
        val repository = PlaylistRepositoryImpl(fakeApi, screenKeyRepository)

        val result = repository.fetchPlaylistItems()

        assertEquals("test-key", fakeApi.lastRequestedScreenKey)
        assertEquals(expected, result)
    }

    @Test
    fun `fetchPlaylistItems returns response with null playlists`() = runBlocking {
        val fakeApi = FakeApiService(
            playlistResponse = PlaylistItemsResponse(screenKey = "key", playlists = null)
        )
        val screenKeyRepository = FakeScreenKeyRepository("key")
        val repository = PlaylistRepositoryImpl(fakeApi, screenKeyRepository)

        val result = repository.fetchPlaylistItems()

        assertNull(result.playlists)
    }

    @Test
    fun `fetchPlaylistItems uses screenKey from repository`() = runBlocking {
        val fakeApi = FakeApiService(playlistResponse = PlaylistItemsResponse(screenKey = "abc"))
        val screenKeyRepository = FakeScreenKeyRepository("abc")
        val repository = PlaylistRepositoryImpl(fakeApi, screenKeyRepository)

        repository.fetchPlaylistItems()

        assertEquals("abc", fakeApi.lastRequestedScreenKey)
    }

    @Test
    fun `fetchPlaylistItems propagates exception from api`() = runBlocking {
        val fakeApi = FakeApiService(throwOnPlaylist = RuntimeException("Network error"))
        val screenKeyRepository = FakeScreenKeyRepository("key")
        val repository = PlaylistRepositoryImpl(fakeApi, screenKeyRepository)

        val exception = runCatching { repository.fetchPlaylistItems() }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }
}
