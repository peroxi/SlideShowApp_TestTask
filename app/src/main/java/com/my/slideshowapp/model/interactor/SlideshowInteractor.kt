package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.entity.MediaItem

interface SlideshowInteractor {
    suspend operator fun invoke(): List<MediaItem>
}
