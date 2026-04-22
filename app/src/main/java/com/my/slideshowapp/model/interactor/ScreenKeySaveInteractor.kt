package com.my.slideshowapp.model.interactor

interface ScreenKeySaveInteractor {
    suspend operator fun invoke(key: String)
}