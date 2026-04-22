package com.my.slideshowapp.model.repository

import com.my.slideshowapp.model.entity.PlaylistItemsResponse
import com.my.slideshowapp.model.network.ApiService
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody

internal class FakeApiService(
    private val playlistResponse: PlaylistItemsResponse = PlaylistItemsResponse(),
    private val creativeBytes: ByteArray = ByteArray(0),
    private val throwOnPlaylist: Throwable? = null,
    private val throwOnCreative: Throwable? = null
) : ApiService {

    var lastRequestedScreenKey: String? = null
    var lastRequestedCreativeKey: String? = null

    override suspend fun getPlaylistItems(screenKey: String): PlaylistItemsResponse {
        lastRequestedScreenKey = screenKey
        throwOnPlaylist?.let { throw it }
        return playlistResponse
    }

    override suspend fun getCreative(creativeKey: String): ResponseBody {
        lastRequestedCreativeKey = creativeKey
        throwOnCreative?.let { throw it }
        return creativeBytes.toResponseBody(null)
    }
}

