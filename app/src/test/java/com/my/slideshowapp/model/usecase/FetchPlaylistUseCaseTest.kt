package com.my.slideshowapp.model.usecase

import com.my.slideshowapp.model.entity.Playlist
import com.my.slideshowapp.model.entity.PlaylistItem
import com.my.slideshowapp.model.entity.PlaylistItemsResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FetchPlaylistUseCaseTest {

    @Test
    fun `execute returns all items from all playlists`() = runBlocking {
        val items1 = listOf(
            PlaylistItem(creativeKey = "a", duration = 5),
            PlaylistItem(creativeKey = "b", duration = 10)
        )
        val items2 = listOf(
            PlaylistItem(creativeKey = "c", duration = 3)
        )
        val response = PlaylistItemsResponse(
            playlists = listOf(
                Playlist(playlistKey = "p1", playlistItems = items1),
                Playlist(playlistKey = "p2", playlistItems = items2)
            )
        )
        val useCase = FetchPlaylistUseCase(FakePlaylistRepository(response))

        val result = useCase.execute()

        assertEquals(3, result.size)
        assertEquals(listOf("a", "b", "c"), result.map { it.creativeKey })
    }

    @Test
    fun `execute returns empty list when response has no playlists`() = runBlocking {
        val useCase = FetchPlaylistUseCase(
            FakePlaylistRepository(PlaylistItemsResponse(playlists = null))
        )

        val result = useCase.execute()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute returns empty list when playlists list is empty`() = runBlocking {
        val useCase = FetchPlaylistUseCase(
            FakePlaylistRepository(PlaylistItemsResponse(playlists = emptyList()))
        )

        val result = useCase.execute()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute skips playlist with null playlistItems`() = runBlocking {
        val response = PlaylistItemsResponse(
            playlists = listOf(
                Playlist(playlistKey = "p1", playlistItems = null),
                Playlist(playlistKey = "p2", playlistItems = listOf(PlaylistItem(creativeKey = "x", duration = 5)))
            )
        )
        val useCase = FetchPlaylistUseCase(FakePlaylistRepository(response))

        val result = useCase.execute()

        assertEquals(1, result.size)
        assertEquals("x", result.first().creativeKey)
    }

    @Test
    fun `execute returns empty list when all playlists have null items`() = runBlocking {
        val response = PlaylistItemsResponse(
            playlists = listOf(
                Playlist(playlistKey = "p1", playlistItems = null),
                Playlist(playlistKey = "p2", playlistItems = null)
            )
        )
        val useCase = FetchPlaylistUseCase(FakePlaylistRepository(response))

        val result = useCase.execute()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute calls repository exactly once`() = runBlocking {
        val fakeRepo = FakePlaylistRepository(PlaylistItemsResponse())
        val useCase = FetchPlaylistUseCase(fakeRepo)

        useCase.execute()

        assertEquals(1, fakeRepo.callCount)
    }

    @Test
    fun `execute propagates exception from repository`() = runBlocking {
        val fakeRepo = FakePlaylistRepository(throwOn = RuntimeException("Fetch failed"))
        val useCase = FetchPlaylistUseCase(fakeRepo)

        val exception = runCatching { useCase.execute() }.exceptionOrNull()

        assertEquals("Fetch failed", exception?.message)
    }
}

