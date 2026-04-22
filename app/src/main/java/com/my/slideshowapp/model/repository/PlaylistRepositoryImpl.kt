package com.my.slideshowapp.model.repository

import com.my.slideshowapp.BuildConfig
import com.my.slideshowapp.model.entity.PlaylistItemsResponse
import com.my.slideshowapp.model.network.ApiService
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val screenKeyRepository: ScreenKeyRepository
) : PlaylistRepository {

    override suspend fun fetchPlaylistItems(): PlaylistItemsResponse {
        return apiService.getPlaylistItems(screenKeyRepository.getScreenKey() ?: BuildConfig.SCREEN_KEY)
    }
}
