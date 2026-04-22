package com.my.slideshowapp.model.repository

import com.my.slideshowapp.model.entity.PlaylistItemsResponse

interface PlaylistRepository {
    suspend fun fetchPlaylistItems(): PlaylistItemsResponse
}

