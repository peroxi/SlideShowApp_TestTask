package com.my.slideshowapp.model.network

import com.my.slideshowapp.model.entity.PlaylistItemsResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ApiService {

    @Streaming
    @GET("creative/get/{creativeKey}")
    suspend fun getCreative(
        @Path("creativeKey") creativeKey: String
    ): ResponseBody

    @GET("screen/playlistItems/{screenKey}")
    suspend fun getPlaylistItems(
        @Path("screenKey") screenKey: String
    ): PlaylistItemsResponse
}

