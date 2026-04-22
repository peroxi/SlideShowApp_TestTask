package com.my.slideshowapp.viewmodel

import com.my.slideshowapp.model.entity.MediaItem
import com.my.slideshowapp.model.interactor.ReadSlideshowInteractor

internal class FakeReadSlideshowInteractor(
    private val items: List<MediaItem> = emptyList(),
    private val throwOn: Throwable? = null
) : ReadSlideshowInteractor {

    var callCount = 0

    override suspend fun invoke(): List<MediaItem> {
        callCount++
        throwOn?.let { throw it }
        return items
    }
}

