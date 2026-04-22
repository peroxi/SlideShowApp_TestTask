package com.my.slideshowapp.viewmodel

import com.my.slideshowapp.model.entity.MediaItem
import com.my.slideshowapp.model.interactor.SlideshowInteractor

internal class FakeSlideshowInteractor(
    private val items: List<MediaItem> = emptyList(),
    private val throwOn: Throwable? = null
) : SlideshowInteractor {

    var callCount = 0

    override suspend fun invoke(): List<MediaItem> {
        callCount++
        throwOn?.let { throw it }
        return items
    }
}

