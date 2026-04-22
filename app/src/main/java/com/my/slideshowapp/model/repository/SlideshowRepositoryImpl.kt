package com.my.slideshowapp.model.repository

import com.my.slideshowapp.model.network.ApiService
import javax.inject.Inject

class SlideshowRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : SlideshowRepository {

    override suspend fun fetchCreative(creativeKey: String): ByteArray {
        return apiService.getCreative(creativeKey).bytes()
    }
}
