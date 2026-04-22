package com.my.slideshowapp.model.usecase


interface BaseFetchUseCase<T> {
    suspend fun execute(): T
}
