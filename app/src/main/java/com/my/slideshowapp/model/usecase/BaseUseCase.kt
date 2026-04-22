package com.my.slideshowapp.model.usecase

import com.my.slideshowapp.model.entity.PlaylistItem

abstract class BaseUseCase {
    abstract suspend fun execute(): List<PlaylistItem>
}
