package com.my.slideshowapp.model.repository

import com.my.slideshowapp.model.network.ApiService
import java.io.InputStream
import javax.inject.Inject

class SlideshowRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : SlideshowRepository {

    override suspend fun fetchCreative(creativeKey: String): InputStream {
        return apiService.getCreative(creativeKey).byteStream()
    }
}
