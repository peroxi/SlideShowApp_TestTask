package com.my.slideshowapp.model.interactor

interface ScreenKeyInteractor {
    suspend operator fun invoke(): String
}