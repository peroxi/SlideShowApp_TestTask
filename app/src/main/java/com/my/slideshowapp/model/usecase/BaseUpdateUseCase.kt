package com.my.slideshowapp.model.usecase

interface BaseUpdateUseCase<T> {
    suspend fun execute(param: T)
}