package com.my.slideshowapp.model.repository

interface SlideshowRepository {
    suspend fun fetchCreative(creativeKey: String): ByteArray
}
