package com.my.slideshowapp.model.repository

import java.io.InputStream

interface SlideshowRepository {
    suspend fun fetchCreative(creativeKey: String): InputStream
}
