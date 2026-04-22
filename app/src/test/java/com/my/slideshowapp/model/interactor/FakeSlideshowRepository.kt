package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.repository.SlideshowRepository
import java.io.ByteArrayInputStream
import java.io.InputStream

internal class FakeSlideshowRepository(
    private val bytesPerKey: Map<String, ByteArray> = emptyMap(),
    private val throwOn: Throwable? = null
) : SlideshowRepository {

    val fetchedKeys = mutableListOf<String>()

    override suspend fun fetchCreative(creativeKey: String): InputStream {
        fetchedKeys += creativeKey
        throwOn?.let { throw it }
        return ByteArrayInputStream(bytesPerKey[creativeKey] ?: byteArrayOf())
    }
}
