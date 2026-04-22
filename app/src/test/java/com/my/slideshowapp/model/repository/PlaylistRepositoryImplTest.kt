package com.my.slideshowapp.model.repository

import com.my.slideshowapp.model.ScreenKeyProvider
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
        val repository = PlaylistRepositoryImpl(fakeApi)
        ScreenKeyProvider.screenKey = "test-key"

        val result = repository.fetchPlaylistItems()

        assertEquals("test-key", fakeApi.lastRequestedScreenKey)
        assertEquals(expected, result)
    }

    @Test
    fun `fetchPlaylistItems returns response with null playlists`() = runBlocking {
        val fakeApi = FakeApiService(
            playlistResponse = PlaylistItemsResponse(screenKey = "key", playlists = null)
        )
        val repository = PlaylistRepositoryImpl(fakeApi)
        ScreenKeyProvider.screenKey = "key"

        val result = repository.fetchPlaylistItems()

        assertNull(result.playlists)
    }

    @Test
    fun `fetchPlaylistItems uses current ScreenKeyProvider value`() = runBlocking {
        val fakeApi = FakeApiService(playlistResponse = PlaylistItemsResponse(screenKey = "abc"))
        val repository = PlaylistRepositoryImpl(fakeApi)
        ScreenKeyProvider.screenKey = "abc"

        repository.fetchPlaylistItems()

        assertEquals("abc", fakeApi.lastRequestedScreenKey)
    }

    @Test
    fun `fetchPlaylistItems propagates exception from api`() = runBlocking {
        val fakeApi = FakeApiService(throwOnPlaylist = RuntimeException("Network error"))
        val repository = PlaylistRepositoryImpl(fakeApi)
        ScreenKeyProvider.screenKey = "key"

        val exception = runCatching { repository.fetchPlaylistItems() }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }
}

